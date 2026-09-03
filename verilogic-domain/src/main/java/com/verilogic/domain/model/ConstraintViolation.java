package com.verilogic.domain.model;

import java.util.Objects;

/**
 * Immutable record representing a formal constraint or policy violation.
 */
public record ConstraintViolation(
        String ruleId,
        String statuteCitation,
        Severity severity,
        String failureReason,
        double actualValue,
        double thresholdValue,
        String conditionFormula
) {
    public enum Severity {
        HARD_STOP,
        ADVISORY
    }

    public ConstraintViolation {
        Objects.requireNonNull(ruleId, "ruleId cannot be null");
        Objects.requireNonNull(statuteCitation, "statuteCitation cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        Objects.requireNonNull(failureReason, "failureReason cannot be null");
        Objects.requireNonNull(conditionFormula, "conditionFormula cannot be null");
    }

    /**
     * Mathematical difference between actual and required threshold.
     */
    public double proofDistance() {
        return Math.abs(actualValue - thresholdValue);
    }
}
