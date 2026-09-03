package com.verilogic.application.port.out;

import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for persisting cryptographic certificates and proof traces.
 * Enforces immutable Merkle hash audit chains.
 */
public interface AuditStoragePort {

    void recordCertificate(VerificationCertificate certificate, ProofTrace trace);

    Optional<VerificationCertificate> findCertificateById(String certificateId);

    Optional<VerificationCertificate> findCertificateByCaseId(String caseId);

    List<VerificationCertificate> findRecentCertificates(int limit);

    /**
     * Retrieves the latest certificate hash in the ledger chain (or GENESIS_HASH if empty).
     */
    String getLatestCertificateHash();

    /**
     * Cryptographically verifies the unbroken integrity of the entire audit chain from genesis to tip.
     * Re-hashes every block and checks sequential hash pointer links.
     */
    boolean verifyFullLedgerChain();
}
