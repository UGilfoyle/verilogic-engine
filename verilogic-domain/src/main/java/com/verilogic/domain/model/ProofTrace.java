package com.verilogic.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Immutable audit trace summarizing the evaluation of all rules.
 * Generates a deterministic SHA-256 hash for Merkle tree anchoring.
 */
public record ProofTrace(
        String caseId,
        ProofStatus status,
        List<ProofExplanation> explanations,
        Instant evaluatedAt,
        String ruleSetVersion,
        long evaluationDurationNanos
) {
    public enum ProofStatus {
        CERTIFIED,
        VIOLATED,
        RECONCILED,
        REJECTED
    }

    public ProofTrace {
        Objects.requireNonNull(caseId, "caseId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(explanations, "explanations cannot be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt cannot be null");
        Objects.requireNonNull(ruleSetVersion, "ruleSetVersion cannot be null");
        explanations = List.copyOf(explanations);
    }

    /**
     * Computes the deterministic SHA-256 canonical hash of this proof trace.
     */
    public String computeProofHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(caseId).append("|")
              .append(status.name()).append("|")
              .append(ruleSetVersion).append("|")
              .append(evaluatedAt.toEpochMilli()).append("|");

            for (ProofExplanation exp : explanations) {
                sb.append(exp.ruleId()).append(":")
                  .append(exp.satisfied()).append(";")
                  .append(exp.formulaDescription()).append(";");
            }

            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing from JVM", e);
        }
    }
}
