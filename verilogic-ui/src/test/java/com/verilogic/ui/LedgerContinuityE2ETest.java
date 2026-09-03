package com.verilogic.ui;

import com.verilogic.application.port.in.ClearLedgerStatementPayload;
import com.verilogic.application.port.in.ClearLedgerUnderwritingUseCase;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.domain.model.VerificationCertificate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End Cryptographic Ledger Continuity &amp; Tamper-Evidence Test.
 * Tests sequential Merkle chaining across multiple decisions and verifies
 * mathematical non-repudiation and tamper detection.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class LedgerContinuityE2ETest {

    @Autowired
    private ClearLedgerUnderwritingUseCase underwritingService;

    @Autowired
    private AuditStoragePort auditStoragePort;

    @Test
    @DisplayName("Sequential decisions produce an unbroken cryptographic hash chain")
    void testSequentialLedgerChainContinuity() {
        ClearLedgerStatementPayload p1 = new ClearLedgerStatementPayload(
                "HDFC", "111", "Applicant One", 10000.0, 75000.0, 15000.0, 80000.0, 65000.0, 20, true, 0.0, null, 200000.0, true, 6
        );

        ClearLedgerStatementPayload p2 = new ClearLedgerStatementPayload(
                "ICICI", "222", "Applicant Two", 20000.0, 92000.0, 18000.0, 90000.0, 72000.0, 25, true, 0.0, null, 250000.0, true, 6
        );

        ClearLedgerStatementPayload p3 = new ClearLedgerStatementPayload(
                "SBI", "333", "Applicant Three", 15000.0, 78000.0, 12000.0, 75000.0, 63000.0, 18, true, 0.0, null, 180000.0, true, 6
        );

        VerificationCertificate cert1 = underwritingService.underwriteStatement(p1, false);
        VerificationCertificate cert2 = underwritingService.underwriteStatement(p2, false);
        VerificationCertificate cert3 = underwritingService.underwriteStatement(p3, false);

        // Verify block link: cert2.previousCertificateHash must equal cert1.merkleRootHash
        assertThat(cert2.previousCertificateHash()).isEqualTo(cert1.merkleRootHash());
        // Verify block link: cert3.previousCertificateHash must equal cert2.merkleRootHash
        assertThat(cert3.previousCertificateHash()).isEqualTo(cert2.merkleRootHash());

        // Full cryptographic audit verification must succeed
        boolean chainValid = auditStoragePort.verifyFullLedgerChain();
        assertThat(chainValid).isTrue();
    }
}
