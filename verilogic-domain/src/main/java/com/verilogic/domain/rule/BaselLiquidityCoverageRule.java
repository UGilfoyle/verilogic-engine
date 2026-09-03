package com.verilogic.domain.rule;

import com.verilogic.domain.model.ConstraintViolation;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;

import java.util.List;
import java.util.Map;

/**
 * Basel III Liquidity Coverage Framework (BCBS d238).
 * Formal Decision Boolean Formula: C1 AND (C2 OR C3)
 * C1: Monthly Net Income &gt;= 5000.00
 * C2: Liquid Reserves &gt;= 20% of requested loan amount
 * C3: Composite Risk Score &lt;= 0.25 (Prime Tier)
 *
 * Minimal MC/DC vectors: N=3 -&gt; 4 vectors proving independent condition effects.
 */
public class BaselLiquidityCoverageRule implements RuleSpecification<DecisionCase> {

    public static final String RULE_ID = "RULE-BASEL-LCR-238";
    public static final String STATUTE = "Basel III BCBS d238 (LCR)";
    public static final String FORMULA = "Income >= 5000 && (Reserves >= 0.20 * Loan || RiskScore <= 0.25)";

    public static final double MIN_MONTHLY_INCOME = 5000.00;
    public static final double RESERVE_RATIO_FACTOR = 0.20;
    public static final double MAX_PRIME_RISK_SCORE = 0.25;

    @Override
    public String ruleId() {
        return RULE_ID;
    }

    @Override
    public String statuteCitation() {
        return STATUTE;
    }

    @Override
    public String formulaDescription() {
        return FORMULA;
    }

    @Override
    public ProofExplanation evaluate(DecisionCase entity) {
        double requiredReserves = entity.loanAmountRequested() * RESERVE_RATIO_FACTOR;

        boolean c1 = entity.monthlyIncome() >= MIN_MONTHLY_INCOME;
        boolean c2 = entity.liquidReserves() >= requiredReserves;
        boolean c3 = entity.riskScore() <= MAX_PRIME_RISK_SCORE;

        boolean conditionSatisfied = c1 && (c2 || c3);

        String details = String.format(
                "Basel LCR: [C1: Income=%.2f >= 5000 -> %b], [C2: Reserves=%.2f >= %.2f -> %b], [C3: RiskScore=%.3f <= 0.25 -> %b]. Satisfied=%b",
                entity.monthlyIncome(), c1,
                entity.liquidReserves(), requiredReserves, c2,
                entity.riskScore(), c3,
                conditionSatisfied
        );

        if (conditionSatisfied) {
            return ProofExplanation.satisfied(RULE_ID, STATUTE, FORMULA, details);
        }

        ConstraintViolation violation = new ConstraintViolation(
                RULE_ID,
                STATUTE,
                ConstraintViolation.Severity.HARD_STOP,
                String.format("Basel III Liquidity Deficit. Income: %.2f (req >= 5000), Reserves: %.2f (req >= %.2f), Risk: %.2f (req <= 0.25)",
                        entity.monthlyIncome(), entity.liquidReserves(), requiredReserves, entity.riskScore()),
                entity.liquidReserves(),
                requiredReserves,
                FORMULA
        );

        return ProofExplanation.violated(RULE_ID, STATUTE, FORMULA, details, violation);
    }

    @Override
    public McdcTruthTable getMcdcTruthTable() {
        return new McdcTruthTable(
                RULE_ID,
                FORMULA,
                List.of(
                        "C1: Income >= 5000",
                        "C2: Reserves >= 0.20 * Loan",
                        "C3: RiskScore <= 0.25"
                ),
                List.of(
                        new McdcTruthTable.McdcVector("TC-LCR-01", List.of(true, true, false), true, "Baseline C1, C2"),
                        new McdcTruthTable.McdcVector("TC-LCR-02", List.of(false, true, false), false, "C1 toggled (C1 independence)"),
                        new McdcTruthTable.McdcVector("TC-LCR-03", List.of(true, false, false), false, "C2 toggled (C2 independence)"),
                        new McdcTruthTable.McdcVector("TC-LCR-04", List.of(true, false, true), true, "C3 toggled to True (C3 independence)")
                ),
                Map.of(
                        "C1", new McdcTruthTable.IndependencePair("C1", "TC-LCR-01", "TC-LCR-02", "C1 toggles outcome with (C2=T, C3=F)"),
                        "C2", new McdcTruthTable.IndependencePair("C2", "TC-LCR-01", "TC-LCR-03", "C2 toggles outcome with (C1=T, C3=F)"),
                        "C3", new McdcTruthTable.IndependencePair("C3", "TC-LCR-04", "TC-LCR-03", "C3 toggles outcome with (C1=T, C2=F)")
                )
        );
    }
}
