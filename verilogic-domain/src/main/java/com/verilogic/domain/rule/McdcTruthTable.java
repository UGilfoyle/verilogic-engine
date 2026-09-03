package com.verilogic.domain.rule;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates the formal MC/DC (Modified Condition/Decision Coverage)
 * truth table and independent effect verification proofs.
 */
public record McdcTruthTable(
        String ruleId,
        String booleanFormula,
        List<String> conditionDescriptions,
        List<McdcVector> vectors,
        Map<String, IndependencePair> independenceProofs
) {
    public McdcTruthTable {
        Objects.requireNonNull(ruleId, "ruleId cannot be null");
        Objects.requireNonNull(booleanFormula, "booleanFormula cannot be null");
        Objects.requireNonNull(conditionDescriptions, "conditionDescriptions cannot be null");
        Objects.requireNonNull(vectors, "vectors cannot be null");
        Objects.requireNonNull(independenceProofs, "independenceProofs cannot be null");
    }

    /**
     * An individual test vector in the MC/DC matrix.
     */
    public record McdcVector(
            String testCaseId,
            List<Boolean> conditionValues,
            boolean expectedOutcome,
            String targetConditionTested
    ) {}

    /**
     * Represents two test vectors (one True outcome, one False outcome)
     * demonstrating that toggling condition X alone changes the decision outcome.
     */
    public record IndependencePair(
            String conditionName,
            String trueVectorId,
            String falseVectorId,
            String explanation
    ) {}
}
