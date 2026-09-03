package com.verilogic.domain.rule;

import com.verilogic.domain.model.ConstraintViolation;
import com.verilogic.domain.model.ProofExplanation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Specification Pattern Interface for deterministic business rules.
 * Open/Closed: New rules can be plugged in without modifying the engine.
 */
public interface RuleSpecification<T> {

    String ruleId();

    String statuteCitation();

    String formulaDescription();

    ProofExplanation evaluate(T entity);

    default boolean isSatisfiedBy(T entity) {
        return evaluate(entity).satisfied();
    }

    McdcTruthTable getMcdcTruthTable();

    /**
     * Composite AND Specification.
     */
    default RuleSpecification<T> and(RuleSpecification<T> other) {
        Objects.requireNonNull(other, "other rule cannot be null");
        RuleSpecification<T> self = this;

        return new RuleSpecification<>() {
            @Override
            public String ruleId() {
                return "(" + self.ruleId() + " AND " + other.ruleId() + ")";
            }

            @Override
            public String statuteCitation() {
                return self.statuteCitation() + " & " + other.statuteCitation();
            }

            @Override
            public String formulaDescription() {
                return "(" + self.formulaDescription() + " && " + other.formulaDescription() + ")";
            }

            @Override
            public ProofExplanation evaluate(T entity) {
                ProofExplanation left = self.evaluate(entity);
                if (!left.satisfied()) {
                    return left;
                }
                return other.evaluate(entity);
            }

            @Override
            public McdcTruthTable getMcdcTruthTable() {
                return new McdcTruthTable(
                        ruleId(),
                        formulaDescription(),
                        List.of("Left: " + self.ruleId(), "Right: " + other.ruleId()),
                        List.of(
                                new McdcTruthTable.McdcVector("TC-AND-1", List.of(true, true), true, "Baseline"),
                                new McdcTruthTable.McdcVector("TC-AND-2", List.of(false, true), false, "Left condition toggled"),
                                new McdcTruthTable.McdcVector("TC-AND-3", List.of(true, false), false, "Right condition toggled")
                        ),
                        Map.of(
                                "Left", new McdcTruthTable.IndependencePair("Left", "TC-AND-1", "TC-AND-2", "Left independent effect"),
                                "Right", new McdcTruthTable.IndependencePair("Right", "TC-AND-1", "TC-AND-3", "Right independent effect")
                        )
                );
            }
        };
    }

    /**
     * Composite OR Specification.
     */
    default RuleSpecification<T> or(RuleSpecification<T> other) {
        Objects.requireNonNull(other, "other rule cannot be null");
        RuleSpecification<T> self = this;

        return new RuleSpecification<>() {
            @Override
            public String ruleId() {
                return "(" + self.ruleId() + " OR " + other.ruleId() + ")";
            }

            @Override
            public String statuteCitation() {
                return self.statuteCitation() + " | " + other.statuteCitation();
            }

            @Override
            public String formulaDescription() {
                return "(" + self.formulaDescription() + " || " + other.formulaDescription() + ")";
            }

            @Override
            public ProofExplanation evaluate(T entity) {
                ProofExplanation left = self.evaluate(entity);
                if (left.satisfied()) {
                    return left;
                }
                ProofExplanation right = other.evaluate(entity);
                if (right.satisfied()) {
                    return right;
                }
                return ProofExplanation.violated(
                        ruleId(),
                        statuteCitation(),
                        formulaDescription(),
                        "Neither condition satisfied: [" + left.details() + "] OR [" + right.details() + "]",
                        new ConstraintViolation(
                                ruleId(),
                                statuteCitation(),
                                ConstraintViolation.Severity.HARD_STOP,
                                "Both OR branches failed evaluation",
                                0.0,
                                1.0,
                                formulaDescription()
                        )
                );
            }

            @Override
            public McdcTruthTable getMcdcTruthTable() {
                return new McdcTruthTable(
                        ruleId(),
                        formulaDescription(),
                        List.of("Left: " + self.ruleId(), "Right: " + other.ruleId()),
                        List.of(
                                new McdcTruthTable.McdcVector("TC-OR-1", List.of(false, false), false, "Baseline False"),
                                new McdcTruthTable.McdcVector("TC-OR-2", List.of(true, false), true, "Left condition toggled"),
                                new McdcTruthTable.McdcVector("TC-OR-3", List.of(false, true), true, "Right condition toggled")
                        ),
                        Map.of(
                                "Left", new McdcTruthTable.IndependencePair("Left", "TC-OR-2", "TC-OR-1", "Left independent effect"),
                                "Right", new McdcTruthTable.IndependencePair("Right", "TC-OR-3", "TC-OR-1", "Right independent effect")
                        )
                );
            }
        };
    }
}
