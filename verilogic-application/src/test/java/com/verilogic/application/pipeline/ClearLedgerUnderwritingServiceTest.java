package com.verilogic.application.pipeline;

import com.verilogic.application.port.in.ClearLedgerStatementPayload;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.application.port.out.ConstraintSolverPort;
import com.verilogic.application.port.out.ValkeyCachePort;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;
import com.verilogic.domain.rule.AntiMoneyLaunderingRiskRule;
import com.verilogic.domain.rule.BaselLiquidityCoverageRule;
import com.verilogic.domain.rule.QualifiedMortgageSolvencyRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * High-Assurance Integration Test Suite for ClearLedger Forensics &amp; Statutory Underwriting.
 */
class ClearLedgerUnderwritingServiceTest {

    private ClearLedgerUnderwritingService underwritingService;

    @BeforeEach
    void setUp() {
        ConstraintSolverPort solverPort = new ConstraintSolverPort() {
            private final QualifiedMortgageSolvencyRule qm = new QualifiedMortgageSolvencyRule();
            private final BaselLiquidityCoverageRule basel = new BaselLiquidityCoverageRule();
            private final AntiMoneyLaunderingRiskRule aml = new AntiMoneyLaunderingRiskRule();

            @Override
            public ProofTrace solveAndVerify(DecisionCase decisionCase) {
                List<ProofExplanation> list = List.of(qm.evaluate(decisionCase), basel.evaluate(decisionCase), aml.evaluate(decisionCase));
                boolean pass = list.stream().allMatch(ProofExplanation::satisfied);
                return new ProofTrace(decisionCase.caseId(), pass ? ProofTrace.ProofStatus.CERTIFIED : ProofTrace.ProofStatus.VIOLATED, list, Instant.now(), "rules-v1", 100L);
            }

            @Override public List<com.verilogic.domain.rule.McdcTruthTable> getActiveMcdcMatrices() { return List.of(); }
            @Override public String getRuleSetVersionHash() { return "RULES-HASH-2026"; }
        };

        ValkeyCachePort valkeyPort = new ValkeyCachePort() {
            @Override public boolean acquireLock(String key, String leaseToken, long ttlMillis) { return true; }
            @Override public boolean releaseLock(String key, String leaseToken) { return true; }
            @Override public void cacheSessionState(String caseId, String payloadJson, long ttlMillis) {}
            @Override public Optional<String> getSessionState(String caseId) { return Optional.empty(); }
            @Override public void publishDomainEvent(String channel, String eventJson) {}
        };

        AuditStoragePort auditPort = new AuditStoragePort() {
            @Override public void recordCertificate(VerificationCertificate certificate, ProofTrace trace) {}
            @Override public Optional<VerificationCertificate> findCertificateById(String certificateId) { return Optional.empty(); }
            @Override public Optional<VerificationCertificate> findCertificateByCaseId(String caseId) { return Optional.empty(); }
            @Override public List<VerificationCertificate> findRecentCertificates(int limit) { return List.of(); }
            @Override public String getLatestCertificateHash() { return VerificationCertificate.GENESIS_PREVIOUS_HASH; }
            @Override public boolean verifyFullLedgerChain() { return true; }
        };

        underwritingService = new ClearLedgerUnderwritingService(solverPort, valkeyPort, auditPort);
    }

    @Test
    @DisplayName("Successfully Certify pristine statement verified by ClearLedger")
    void shouldCertifyCleanStatement() {
        var fraud = new ClearLedgerStatementPayload.FraudReportPayload(
                5, "LOW", "SANCTION_APPROVED", 99.8, "Finacle Core Banking",
                false, List.of(), 54200.0, 80400.0, 16800.0, 63600.0,
                true, 13400.0, "HIGH", 0, 0.0, 0, 0.0, List.of(), "FORENSIC_PDF"
        );

        ClearLedgerStatementPayload payload = new ClearLedgerStatementPayload(
                "HDFC Bank", "9018420911", "Rajesh Sharma",
                10000.0, 73600.0, 16800.0, 80400.0, 63600.0,
                142, true, 0.0, fraud, 300000.0, true, 6
        );

        VerificationCertificate cert = underwritingService.underwriteStatement(payload, true);

        assertThat(cert.status()).isEqualTo(ProofTrace.ProofStatus.CERTIFIED);
        assertThat(cert.merkleRootHash()).isNotBlank();
    }

    @Test
    @DisplayName("Immediately Reject statement if ClearLedger flags Canva/PDF tampering")
    void shouldRejectTamperedPdfStatement() {
        var fraud = new ClearLedgerStatementPayload.FraudReportPayload(
                85, "CRITICAL", "REJECT_SUSPECTED_FRAUD", 42.0, "Canva PDF Generator",
                true, List.of("Trailer /Prev pointer missing", "Font Helvetica replaced with Arial"),
                20000.0, 50000.0, 10000.0, 40000.0,
                true, 10000.0, "VOLATILE", 3, 0.15, 2, 5000.0, List.of(), "FORENSIC_PDF"
        );

        ClearLedgerStatementPayload payload = new ClearLedgerStatementPayload(
                "Fake Bank", "12345678", "Tampered User",
                5000.0, 45000.0, 10000.0, 50000.0, 40000.0,
                50, true, 0.0, fraud, 250000.0, false, 6
        );

        VerificationCertificate cert = underwritingService.underwriteStatement(payload, true);

        assertThat(cert.status()).isEqualTo(ProofTrace.ProofStatus.REJECTED);
    }

    @Test
    @DisplayName("Immediately Reject statement if ClearLedger flags transaction balance discrepancy")
    void shouldRejectUnbalancedLedgerStatement() {
        var fraud = new ClearLedgerStatementPayload.FraudReportPayload(
                60, "HIGH", "MANUAL_REVIEW_REQUIRED", 90.0, "Core Banking",
                false, List.of(), 30000.0, 40000.0, 20000.0, 20000.0,
                true, 8000.0, "MODERATE", 0, 0.0, 0, 0.0, List.of(), "FORENSIC_PDF"
        );

        // Discrepancy amount = 5000.0, isBalanced = false
        ClearLedgerStatementPayload payload = new ClearLedgerStatementPayload(
                "Axis Bank", "99887766", "Discrepancy User",
                10000.0, 25000.0, 20000.0, 40000.0, 15000.0,
                80, false, 5000.0, fraud, 200000.0, false, 6
        );

        VerificationCertificate cert = underwritingService.underwriteStatement(payload, true);

        assertThat(cert.status()).isEqualTo(ProofTrace.ProofStatus.REJECTED);
    }
}
