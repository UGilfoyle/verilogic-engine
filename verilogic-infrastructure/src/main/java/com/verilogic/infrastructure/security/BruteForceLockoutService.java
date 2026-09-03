package com.verilogic.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Brute-Force and Credential/Token Abuse Lockout Service.
 * Applies a 5-strike jail threshold with exponential backoff per offending IP address.
 */
@Service
public class BruteForceLockoutService {

    private static final Logger log = LoggerFactory.getLogger(BruteForceLockoutService.class);

    public static final int MAX_STRIKES_BEFORE_LOCKOUT = 5;
    public static final long BASE_LOCKOUT_DURATION_SECONDS = 900L; // 15 minutes

    private static class ClientAbuseRecord {
        int strikes;
        long firstStrikeEpochSeconds;
        long lockedUntilEpochSeconds;
        int lockoutCount;
    }

    private final Map<String, ClientAbuseRecord> abuseRegistry = new ConcurrentHashMap<>();

    public synchronized void recordStrike(String clientIp) {
        long now = Instant.now().getEpochSecond();
        ClientAbuseRecord record = abuseRegistry.computeIfAbsent(clientIp, k -> {
            ClientAbuseRecord r = new ClientAbuseRecord();
            r.firstStrikeEpochSeconds = now;
            return r;
        });

        // Reset strike window if older than 60 seconds
        if (now - record.firstStrikeEpochSeconds > 60) {
            record.strikes = 1;
            record.firstStrikeEpochSeconds = now;
            return;
        }

        record.strikes++;
        if (record.strikes >= MAX_STRIKES_BEFORE_LOCKOUT) {
            record.lockoutCount++;
            long backoffMultiplier = 1L << Math.min(4, record.lockoutCount - 1); // 1x, 2x, 4x, 8x, 16x
            long lockoutDuration = BASE_LOCKOUT_DURATION_SECONDS * backoffMultiplier;
            record.lockedUntilEpochSeconds = now + lockoutDuration;
            log.warn("[SECURITY ALERT] Client IP [{}] JAILED for {} seconds due to repeated brute-force / malicious abuse.",
                    clientIp, lockoutDuration);
        }
    }

    public synchronized boolean isLockedOut(String clientIp) {
        ClientAbuseRecord record = abuseRegistry.get(clientIp);
        if (record == null) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        if (record.lockedUntilEpochSeconds > now) {
            return true;
        }
        // Lockout expired
        if (record.lockedUntilEpochSeconds > 0) {
            record.strikes = 0;
            record.lockedUntilEpochSeconds = 0;
        }
        return false;
    }

    public synchronized long getRemainingLockoutSeconds(String clientIp) {
        ClientAbuseRecord record = abuseRegistry.get(clientIp);
        if (record == null) {
            return 0L;
        }
        long remaining = record.lockedUntilEpochSeconds - Instant.now().getEpochSecond();
        return Math.max(0L, remaining);
    }

    public synchronized void reset(String clientIp) {
        abuseRegistry.remove(clientIp);
    }
}
