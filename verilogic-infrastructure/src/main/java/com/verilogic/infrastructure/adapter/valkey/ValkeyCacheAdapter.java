package com.verilogic.infrastructure.adapter.valkey;

import com.verilogic.application.port.out.ValkeyCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valkey 8.0 Distributed Cache &amp; Mutex Adapter (BSD-3 Clause).
 * Enforces atomic distributed locking via SETNX and safe Lua release.
 * Features automatic resilient fallback to in-memory lock if Valkey is not reachable.
 */
@Component
public class ValkeyCacheAdapter implements ValkeyCachePort {

    private static final Logger log = LoggerFactory.getLogger(ValkeyCacheAdapter.class);

    private static final String LUA_RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final JedisPool jedisPool;
    private final boolean valkeyAvailable;

    // Resilient in-memory fallback structures
    private final Map<String, String> localLockMap = new ConcurrentHashMap<>();
    private final Map<String, String> localSessionMap = new ConcurrentHashMap<>();

    public ValkeyCacheAdapter(
            @Value("${valkey.host:localhost}") String host,
            @Value("${valkey.port:6379}") int port
    ) {
        JedisPool pool = null;
        boolean connected = false;
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(16);
            config.setMaxIdle(8);
            config.setMinIdle(2);
            pool = new JedisPool(config, host, port, 2000);

            try (Jedis jedis = pool.getResource()) {
                String ping = jedis.ping();
                connected = "PONG".equalsIgnoreCase(ping);
                log.info("Successfully established connection to Valkey 8.0 cluster at {}:{}", host, port);
            }
        } catch (Exception e) {
            log.warn("Valkey instance not reachable at {}:{} ({}). Falling back to in-memory resilient state.", host, port, e.getMessage());
            connected = false;
        }

        this.jedisPool = pool;
        this.valkeyAvailable = connected;
    }

    @Override
    public boolean acquireLock(String key, String leaseToken, long ttlMillis) {
        if (valkeyAvailable && jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                SetParams params = SetParams.setParams().nx().px(ttlMillis);
                String result = jedis.set(key, leaseToken, params);
                return "OK".equalsIgnoreCase(result);
            } catch (Exception e) {
                log.warn("Valkey lock acquisition failed, falling back to local lock: {}", e.getMessage());
            }
        }
        // Local in-memory lock fallback
        return localLockMap.putIfAbsent(key, leaseToken) == null;
    }

    @Override
    public boolean releaseLock(String key, String leaseToken) {
        if (valkeyAvailable && jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                Object result = jedis.eval(LUA_RELEASE_LOCK_SCRIPT, Collections.singletonList(key), Collections.singletonList(leaseToken));
                return Long.valueOf(1L).equals(result);
            } catch (Exception e) {
                log.warn("Valkey lock release failed: {}", e.getMessage());
            }
        }
        return localLockMap.remove(key, leaseToken);
    }

    @Override
    public void cacheSessionState(String caseId, String payloadJson, long ttlMillis) {
        if (valkeyAvailable && jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex("session:reconciliation:" + caseId, (int) (ttlMillis / 1000L), payloadJson);
                return;
            } catch (Exception e) {
                log.warn("Valkey cache write failed: {}", e.getMessage());
            }
        }
        localSessionMap.put(caseId, payloadJson);
    }

    @Override
    public Optional<String> getSessionState(String caseId) {
        if (valkeyAvailable && jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                String val = jedis.get("session:reconciliation:" + caseId);
                return Optional.ofNullable(val);
            } catch (Exception e) {
                log.warn("Valkey cache read failed: {}", e.getMessage());
            }
        }
        return Optional.ofNullable(localSessionMap.get(caseId));
    }

    @Override
    public void publishDomainEvent(String channel, String eventJson) {
        if (valkeyAvailable && jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channel, eventJson);
                return;
            } catch (Exception e) {
                log.warn("Valkey event publish failed: {}", e.getMessage());
            }
        }
        log.info("[DomainEvent Published] Channel: {} | Payload: {}", channel, eventJson);
    }

    public boolean isValkeyAvailable() {
        return valkeyAvailable;
    }
}
