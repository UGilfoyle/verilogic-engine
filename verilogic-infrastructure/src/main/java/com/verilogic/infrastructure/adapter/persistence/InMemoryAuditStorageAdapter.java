package com.verilogic.infrastructure.adapter.persistence;

import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-assurance in-memory audit ledger maintaining cryptographic proof chains.
 * Verifies sequential hash pointer integrity from genesis to tip.
 */
@Component
public class InMemoryAuditStorageAdapter implements AuditStoragePort {

    private final Map<String, VerificationCertificate> certificateById = new ConcurrentHashMap<>();
    private final Map<String, VerificationCertificate> certificateByCaseId = new ConcurrentHashMap<>();
    private final Map<String, ProofTrace> traceByCaseId = new ConcurrentHashMap<>();
    private final List<VerificationCertificate> ledgerChain = new CopyOnWriteArrayList<>();

    @Override
    public synchronized void recordCertificate(VerificationCertificate certificate, ProofTrace trace) {
        certificateById.put(certificate.certificateId(), certificate);
        certificateByCaseId.put(certificate.caseId(), certificate);
        traceByCaseId.put(certificate.caseId(), trace);
        ledgerChain.add(certificate);
    }

    @Override
    public Optional<VerificationCertificate> findCertificateById(String certificateId) {
        return Optional.ofNullable(certificateById.get(certificateId));
    }

    @Override
    public Optional<VerificationCertificate> findCertificateByCaseId(String caseId) {
        return Optional.ofNullable(certificateByCaseId.get(caseId));
    }

    @Override
    public List<VerificationCertificate> findRecentCertificates(int limit) {
        return certificateById.values().stream()
                .sorted(Comparator.comparing(VerificationCertificate::issuedAt).reversed())
                .limit(limit)
                .toList();
    }

    public Optional<ProofTrace> findTraceByCaseId(String caseId) {
        return Optional.ofNullable(traceByCaseId.get(caseId));
    }

    @Override
    public synchronized String getLatestCertificateHash() {
        if (ledgerChain.isEmpty()) {
            return VerificationCertificate.GENESIS_PREVIOUS_HASH;
        }
        return ledgerChain.getLast().merkleRootHash();
    }

    /**
     * Re-computes and cryptographically verifies the integrity of an individual stored certificate.
     */
    public boolean verifyCertificateIntegrity(String certificateId) {
        VerificationCertificate cert = certificateById.get(certificateId);
        if (cert == null) {
            return false;
        }

        String recomputedMerkle = VerificationCertificate.computeMerkleRoot(
                cert.canonicalInputHash(),
                cert.ruleSetHash(),
                cert.proofTraceHash(),
                cert.previousCertificateHash()
        );

        return recomputedMerkle.equalsIgnoreCase(cert.merkleRootHash());
    }

    /**
     * Cryptographically verifies the unbroken integrity of the entire audit chain from genesis to tip.
     * Validates sequential hash pointers and Merkle root integrity for every block.
     */
    @Override
    public synchronized boolean verifyFullLedgerChain() {
        if (ledgerChain.isEmpty()) {
            return true;
        }

        String expectedPrevHash = VerificationCertificate.GENESIS_PREVIOUS_HASH;

        for (VerificationCertificate cert : ledgerChain) {
            // 1. Verify sequential hash link
            if (!cert.previousCertificateHash().equalsIgnoreCase(expectedPrevHash)) {
                return false;
            }

            // 2. Re-compute Merkle root for the current certificate
            String recomputedRoot = VerificationCertificate.computeMerkleRoot(
                    cert.canonicalInputHash(),
                    cert.ruleSetHash(),
                    cert.proofTraceHash(),
                    cert.previousCertificateHash()
            );

            if (!recomputedRoot.equalsIgnoreCase(cert.merkleRootHash())) {
                return false;
            }

            // Advance pointer to current certificate's root hash
            expectedPrevHash = cert.merkleRootHash();
        }

        return true;
    }
}
