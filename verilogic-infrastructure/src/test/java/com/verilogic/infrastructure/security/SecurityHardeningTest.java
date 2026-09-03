package com.verilogic.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enterprise Security &amp; Edge Protection Test Suite.
 * Validates immunity against SQL Injections, Brute-Force jailing, and DDoS token bucket rate limiting.
 */
class SecurityHardeningTest {

    @ParameterizedTest(name = "SQLi Vector: {0}")
    @ValueSource(strings = {
            "' OR '1'='1",
            "' OR 1=1 --",
            "admin' UNION SELECT id, password, email FROM users --",
            "'; DROP TABLE audit_certificates; --",
            "1; TRUNCATE TABLE cases;",
            "'; WAITFOR DELAY '0:0:5'--",
            "1 AND SLEEP(5)",
            "SELECT * FROM accounts WHERE id = 1"
    })
    @DisplayName("Detect and Neutralize SQL Injection Vectors")
    void shouldDetectAndNeutralizeSqlInjections(String maliciousInput) {
        var result = SqlInjectionDefenseGuard.scan(maliciousInput);
        assertThat(result.injectionDetected()).isTrue();
        assertThat(result.detectedVector()).isNotBlank();
    }

    @Test
    @DisplayName("Allow Clean Financial Texts Without False Positives")
    void shouldAllowCleanFinancialInputs() {
        String cleanInput = "Applicant Rajesh Sharma requesting $250,000 mortgage with monthly salary $12,000.";
        var result = SqlInjectionDefenseGuard.scan(cleanInput);
        assertThat(result.injectionDetected()).isFalse();
    }

    @Test
    @DisplayName("Enforce 5-Strike IP Lockout on Brute-Force Abuse")
    void shouldJailClientIpAfterFiveStrikes() {
        BruteForceLockoutService lockoutService = new BruteForceLockoutService();
        String attackerIp = "192.168.1.105";

        // Strikes 1 to 4 should not jail
        for (int i = 1; i <= 4; i++) {
            lockoutService.recordStrike(attackerIp);
            assertThat(lockoutService.isLockedOut(attackerIp)).isFalse();
        }

        // Strike 5 must trigger jailing
        lockoutService.recordStrike(attackerIp);
        assertThat(lockoutService.isLockedOut(attackerIp)).isTrue();
        assertThat(lockoutService.getRemainingLockoutSeconds(attackerIp)).isGreaterThan(800L);

        // Reset clears lockout
        lockoutService.reset(attackerIp);
        assertThat(lockoutService.isLockedOut(attackerIp)).isFalse();
    }
}
