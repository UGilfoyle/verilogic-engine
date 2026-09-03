package com.verilogic.application.port.out;

import java.util.Optional;

/**
 * Outbound port for distributed cache, locking, and pub/sub.
 * Implemented via Valkey 8.0 (Linux Foundation open-source fork).
 */
public interface ValkeyCachePort {

    /**
     * Acquires a distributed mutex lock using atomic lease token.
     * Prevents duplicate evaluation across distributed nodes.
     */
    boolean acquireLock(String key, String leaseToken, long ttlMillis);

    /**
     * Releases the distributed lock only if the token matches.
     */
    boolean releaseLock(String key, String leaseToken);

    /**
     * Caches reconciliation session state (L2 cache).
     */
    void cacheSessionState(String caseId, String payloadJson, long ttlMillis);

    /**
     * Retrieves cached session state.
     */
    Optional<String> getSessionState(String caseId);

    /**
     * Publishes domain event to Valkey Pub/Sub channel for real-time UI/event streaming.
     */
    void publishDomainEvent(String channel, String eventJson);
}
