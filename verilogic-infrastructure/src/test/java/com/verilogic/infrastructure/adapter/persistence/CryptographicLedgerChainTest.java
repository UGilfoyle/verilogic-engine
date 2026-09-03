package com.verilogic.infrastructure.adapter.persistence;

import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite verifying blockchain-grade cryptographic Merkle ledger chaining.
 * Validates sequential pointer integrity and instant tamper detection.
 */
class CryptographicLedgerChainTest {

    private InMemoryAuditStorageAdapter auditAdapter;

    @BeforeEach
    void setUp() {
        auditAdapter = new InMemoryAuditStorageAdapter();
    }

    @Test
    @DisplayName("Verify unbroken cryptographic chain across sequential decision certificates")
    void shouldVerifyUnbrokenChain() {
        // Genesis certificate (index 0)
        String prevHash = auditAdapter.getLatestCertificateHash();
        assertThat(prevHash).isEqualTo(VerificationCertificate.GENESIS_PREVIOUS_HASH);

        VerificationCertificate cert1 = VerificationCertificate.seal(
                "case-1", ProofTrace.ProofStatus.CERTIFIED, "input-hash-1", "rule-v1", "proof-hash-1", prevHash
        );
        auditAdapter.recordCertificate(cert1, createDummyTrace("case-1"));

        // Certificate 2 (linked to cert1)
        prevHash = auditAdapter.getLatestCertificateHash();
        assertThat(prevHash).isEqualTo(cert1.merkleRootHash());

        VerificationCertificate cert2 = VerificationCertificate.seal(
                "case-2", ProofTrace.ProofStatus.RECONCILED, "input-hash-2", "rule-v1", "proof-hash-2", prevHash
        );
        auditAdapter.recordCertificate(cert2, createDummyTrace("case-2"));

        // Certificate 3 (linked to cert2)
        prevHash = auditAdapter.getLatestCertificateHash();
        assertThat(prevHash).isEqualTo(cert2.merkleRootHash());

        VerificationCertificate cert3 = VerificationCertificate.seal(
                "case-3", ProofTrace.ProofStatus.REJECTED, "input-hash-3", "rule-v1", "proof-hash-3", prevHash
        );
        auditAdapter.recordCertificate(cert3, createDummyTrace("case-3"));

        // The entire 3-block chain must be verified
        assertThat(auditAdapter.verifyFullLedgerChain()).isTrue();
    }

    @Test
    @DisplayName("Detect tampering if any historical certificate in the chain is modified")
    void shouldDetectTamperingInHistoricalBlock() {
        String prevHash = auditAdapter.getLatestCertificateHash();
        VerificationCertificate cert1 = VerificationCertificate.seal(
                "case-1", ProofTrace.ProofStatus.CERTIFIED, "input-hash-1", "rule-v1", "proof-hash-1", prevHash
        );
        auditAdapter.recordCertificate(cert1, createDummyTrace("case-1"));

        prevHash = auditAdapter.getLatestCertificateHash();
        VerificationCertificate cert2 = VerificationCertificate.seal(
                "case-2", ProofTrace.ProofStatus.CERTIFIED, "input-hash-2", "rule-v1", "proof-hash-2", prevHash
        );
        auditAdapter.recordCertificate(cert2, createDummyTrace("case-2"));

        assertThat(auditAdapter.verifyFullLedgerChain()).isTrue();

        // Simulate an in-memory adversary forging cert1's status or hashes without updating merkleRoot
        VerificationCertificate forgedCert1 = new VerificationCertificate(
                cert1.certificateId(),
                cert1.caseId(),
                ProofTrace.ProofStatus.REJECTED, // Tampered status!
                cert1.canonicalInputHash(),
                cert1.ruleSetHash(),
                "forged-proof-hash", // Tampered proof!
                cert1.previousCertificateHash(),
                cert1.merkleRootHash(), // Stale root hash
                cert1.issuedAt(),
                cert1.issuer()
        );

        auditAdapter.recordCertificate(forgedCert1, createDummyTrace("case-1"));

        // Chain validation MUST now fail immediately
        assertThat(auditAdapter.verifyFullLedgerChain()).isFalse();
    }

    private ProofTrace createDummyTrace(String caseId) {
        return new ProofTrace(caseId, ProofTrace.ProofStatus.CERTIFIED, List.of(), Instant.now(), "v1", 100L);
    }
}
