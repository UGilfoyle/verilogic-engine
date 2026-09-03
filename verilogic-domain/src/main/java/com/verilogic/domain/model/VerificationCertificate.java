package com.verilogic.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable cryptographic decision certificate.
 * Anchors canonical inputs, statutory rules, proof trace, and previous certificate hash in a Merkle tree root.
 * Forms an unbroken, tamper-evident cryptographic audit chain.
 */
public record VerificationCertificate(
        String certificateId,
        String caseId,
        ProofTrace.ProofStatus status,
        String canonicalInputHash,
        String ruleSetHash,
        String proofTraceHash,
        String previousCertificateHash,
        String merkleRootHash,
        Instant issuedAt,
        String issuer
) {
    public static final String GENESIS_PREVIOUS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    public VerificationCertificate {
        Objects.requireNonNull(certificateId, "certificateId cannot be null");
        Objects.requireNonNull(caseId, "caseId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(canonicalInputHash, "canonicalInputHash cannot be null");
        Objects.requireNonNull(ruleSetHash, "ruleSetHash cannot be null");
        Objects.requireNonNull(proofTraceHash, "proofTraceHash cannot be null");
        Objects.requireNonNull(previousCertificateHash, "previousCertificateHash cannot be null");
        Objects.requireNonNull(merkleRootHash, "merkleRootHash cannot be null");
        Objects.requireNonNull(issuedAt, "issuedAt cannot be null");
        Objects.requireNonNull(issuer, "issuer cannot be null");
    }

    /**
     * Factory to assemble and seal a certificate chained to the previous certificate hash.
     */
    public static VerificationCertificate seal(
            String caseId,
            ProofTrace.ProofStatus status,
            String canonicalInputHash,
            String ruleSetHash,
            String proofTraceHash,
            String previousCertificateHash
    ) {
        String prevHash = (previousCertificateHash == null || previousCertificateHash.isBlank())
                ? GENESIS_PREVIOUS_HASH
                : previousCertificateHash;

        String merkleRoot = computeMerkleRoot(canonicalInputHash, ruleSetHash, proofTraceHash, prevHash);
        return new VerificationCertificate(
                UUID.randomUUID().toString(),
                caseId,
                status,
                canonicalInputHash,
                ruleSetHash,
                proofTraceHash,
                prevHash,
                merkleRoot,
                Instant.now(),
                "VeriLogic-ZeroHallucination-Engine/v1.0"
        );
    }

    /**
     * Overloaded factory defaulting to genesis previous hash.
     */
    public static VerificationCertificate seal(
            String caseId,
            ProofTrace.ProofStatus status,
            String canonicalInputHash,
            String ruleSetHash,
            String proofTraceHash
    ) {
        return seal(caseId, status, canonicalInputHash, ruleSetHash, proofTraceHash, GENESIS_PREVIOUS_HASH);
    }

    /**
     * Computes the Merkle Root Hash: H( H(Input || Rules) || H(Proof || PreviousHash) )
     */
    public static String computeMerkleRoot(String inputHash, String ruleHash, String proofHash, String previousHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Left Branch: H(Input || Rules)
            String leftBranch = inputHash + ":" + ruleHash;
            byte[] leftHash = digest.digest(leftBranch.getBytes(StandardCharsets.UTF_8));

            // Right Branch: H(Proof || PreviousHash)
            digest.reset();
            String rightBranch = proofHash + ":" + (previousHash != null ? previousHash : GENESIS_PREVIOUS_HASH);
            byte[] rightHash = digest.digest(rightBranch.getBytes(StandardCharsets.UTF_8));

            // Root: H(leftHash || rightHash)
            digest.reset();
            digest.update(leftHash);
            digest.update(rightHash);
            byte[] root = digest.digest();

            return HexFormat.of().formatHex(root);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available in JVM", e);
        }
    }

    /**
     * Backwards-compatible root computation with genesis previous hash.
     */
    public static String computeMerkleRoot(String inputHash, String ruleHash, String proofHash) {
        return computeMerkleRoot(inputHash, ruleHash, proofHash, GENESIS_PREVIOUS_HASH);
    }
}
