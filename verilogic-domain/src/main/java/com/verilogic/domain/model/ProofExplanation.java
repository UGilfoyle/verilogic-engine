package com.verilogic.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of evaluating a specific rule against an applicant case.
 */
public record ProofExplanation(
        String ruleId,
        String statuteCitation,
        boolean satisfied,
        String formulaDescription,
        String details,
        Optional<ConstraintViolation> violation
) {
    public ProofExplanation {
        Objects.requireNonNull(ruleId, "ruleId cannot be null");
        Objects.requireNonNull(statuteCitation, "statuteCitation cannot be null");
        Objects.requireNonNull(formulaDescription, "formulaDescription cannot be null");
        Objects.requireNonNull(details, "details cannot be null");
        Objects.requireNonNull(violation, "violation cannot be null");
    }

    public static ProofExplanation satisfied(String ruleId, String statuteCitation, String formulaDescription, String details) {
        return new ProofExplanation(ruleId, statuteCitation, true, formulaDescription, details, Optional.empty());
    }

    public static ProofExplanation violated(String ruleId, String statuteCitation, String formulaDescription, String details, ConstraintViolation violation) {
        return new ProofExplanation(ruleId, statuteCitation, false, formulaDescription, details, Optional.of(violation));
    }
}
