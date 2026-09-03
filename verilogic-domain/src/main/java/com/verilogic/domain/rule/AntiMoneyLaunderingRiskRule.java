package com.verilogic.domain.rule;

import com.verilogic.domain.model.ConstraintViolation;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;

import java.util.List;
import java.util.Map;

/**
 * 31 U.S.C. § 5313 / FinCEN AML Risk &amp; Solvency Rule.
 * Formal Boolean Formula: C1 AND C2
 * C1: Composite Risk Score &lt;= 0.40
 * C2: Monthly Debt Obligations &lt; Monthly Income (Strict solvency)
 *
 * MC/DC N=2 -&gt; 3 vectors.
 */
public class AntiMoneyLaunderingRiskRule implements RuleSpecification<DecisionCase> {

    public static final String RULE_ID = "RULE-AML-5313";
    public static final String STATUTE = "31 U.S.C. § 5313 / FinCEN";
    public static final String FORMULA = "RiskScore <= 0.40 && DebtObligations < Income";

    public static final double MAX_AML_RISK = 0.40;

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
        boolean c1 = entity.riskScore() <= MAX_AML_RISK;
        boolean c2 = entity.monthlyDebtObligations() < entity.monthlyIncome();

        boolean satisfied = c1 && c2;

        String details = String.format(
                "AML Policy: [C1: Risk=%.3f <= 0.40 -> %b], [C2: Debt=%.2f < Income=%.2f -> %b]. Satisfied=%b",
                entity.riskScore(), c1,
                entity.monthlyDebtObligations(), entity.monthlyIncome(), c2,
                satisfied
        );

        if (satisfied) {
            return ProofExplanation.satisfied(RULE_ID, STATUTE, FORMULA, details);
        }

        ConstraintViolation violation = new ConstraintViolation(
                RULE_ID,
                STATUTE,
                ConstraintViolation.Severity.HARD_STOP,
                String.format("AML/Solvency Violation: RiskScore=%.3f (threshold <= 0.40), SolvencyCheck=%b", entity.riskScore(), c2),
                entity.riskScore(),
                MAX_AML_RISK,
                FORMULA
        );

        return ProofExplanation.violated(RULE_ID, STATUTE, FORMULA, details, violation);
    }

    @Override
    public McdcTruthTable getMcdcTruthTable() {
        return new McdcTruthTable(
                RULE_ID,
                FORMULA,
                List.of("C1: RiskScore <= 0.40", "C2: DebtObligations < Income"),
                List.of(
                        new McdcTruthTable.McdcVector("TC-AML-01", List.of(true, true), true, "Baseline"),
                        new McdcTruthTable.McdcVector("TC-AML-02", List.of(false, true), false, "C1 toggled (Risk Score fails)"),
                        new McdcTruthTable.McdcVector("TC-AML-03", List.of(true, false), false, "C2 toggled (Insolvent debt obligations)")
                ),
                Map.of(
                        "C1", new McdcTruthTable.IndependencePair("C1", "TC-AML-01", "TC-AML-02", "C1 independent effect verified"),
                        "C2", new McdcTruthTable.IndependencePair("C2", "TC-AML-01", "TC-AML-03", "C2 independent effect verified")
                )
        );
    }
}
