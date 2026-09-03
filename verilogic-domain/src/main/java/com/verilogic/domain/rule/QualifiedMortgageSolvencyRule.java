package com.verilogic.domain.rule;

import com.verilogic.domain.model.ConstraintViolation;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;

import java.util.List;
import java.util.Map;

/**
 * 12 CFR § 1026.43(e) - Ability to Repay &amp; Qualified Mortgage Solvency Rule.
 * Formal Decision Boolean Formula: (C1 AND C2) OR (C3 AND C4)
 * C1: Debt-to-Income (DTI) &lt;= 0.43
 * C2: Credit Score &gt;= 680
 * C3: Reserve Ratio &gt;= 1.50 (6-month liquid cushion)
 * C4: Verified Corporate or Personal Guarantor Present
 *
 * Subject to Level-A MC/DC verification with N=4 conditions -&gt; 5 minimal test vectors.
 */
public class QualifiedMortgageSolvencyRule implements RuleSpecification<DecisionCase> {

    public static final String RULE_ID = "RULE-QM-1026.43E";
    public static final String STATUTE = "12 CFR § 1026.43(e)";
    public static final String FORMULA = "(DTI <= 0.43 && Score >= 680) || (Reserves >= 1.50 && HasGuarantor)";

    public static final double MAX_DTI = 0.43;
    public static final int MIN_CREDIT_SCORE = 680;
    public static final double MIN_RESERVE_RATIO = 1.50;

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
        boolean c1 = entity.debtToIncomeRatio() <= MAX_DTI;
        boolean c2 = entity.creditScore() >= MIN_CREDIT_SCORE;
        boolean c3 = entity.reserveRatio() >= MIN_RESERVE_RATIO;
        boolean c4 = entity.hasGuarantor();

        boolean primaryPath = c1 && c2;
        boolean secondaryPath = c3 && c4;
        boolean overallSatisfied = primaryPath || secondaryPath;

        String details = String.format(
                "Evaluated [C1: DTI=%.2f <= 0.43 -> %b], [C2: Score=%d >= 680 -> %b], [C3: Reserves=%.2f >= 1.50 -> %b], [C4: Guarantor=%b -> %b]. Primary=%b, Secondary=%b",
                entity.debtToIncomeRatio(), c1,
                entity.creditScore(), c2,
                entity.reserveRatio(), c3,
                c4, c4,
                primaryPath, secondaryPath
        );

        if (overallSatisfied) {
            return ProofExplanation.satisfied(RULE_ID, STATUTE, FORMULA, details);
        }

        // Build violation with exact mathematical proof distance
        double dtiExcess = Math.max(0.0, entity.debtToIncomeRatio() - MAX_DTI);
        int scoreDeficit = Math.max(0, MIN_CREDIT_SCORE - entity.creditScore());

        ConstraintViolation violation = new ConstraintViolation(
                RULE_ID,
                STATUTE,
                ConstraintViolation.Severity.HARD_STOP,
                String.format("Qualified Mortgage Solvency Invariant Violated. DTI excess: %.4f, Score deficit: %d", dtiExcess, scoreDeficit),
                entity.debtToIncomeRatio(),
                MAX_DTI,
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
                        "C1: DTI <= 0.43",
                        "C2: Credit Score >= 680",
                        "C3: Reserve Ratio >= 1.50",
                        "C4: Verified Guarantor"
                ),
                List.of(
                        new McdcTruthTable.McdcVector("TC-QM-01", List.of(true, true, false, false), true, "Baseline for C1, C2"),
                        new McdcTruthTable.McdcVector("TC-QM-02", List.of(false, true, false, false), false, "C1 toggled (C1 independence)"),
                        new McdcTruthTable.McdcVector("TC-QM-03", List.of(true, false, false, false), false, "C2 toggled (C2 independence)"),
                        new McdcTruthTable.McdcVector("TC-QM-04", List.of(false, false, true, true), true, "Baseline for C3, C4"),
                        new McdcTruthTable.McdcVector("TC-QM-05", List.of(false, false, true, false), false, "C4 toggled (C4 independence)"),
                        new McdcTruthTable.McdcVector("TC-QM-06", List.of(false, false, false, true), false, "C3 toggled (C3 independence)")
                ),
                Map.of(
                        "C1", new McdcTruthTable.IndependencePair("C1", "TC-QM-01", "TC-QM-02", "C1 independent effect verified against TC-QM-01/02"),
                        "C2", new McdcTruthTable.IndependencePair("C2", "TC-QM-01", "TC-QM-03", "C2 independent effect verified against TC-QM-01/03"),
                        "C3", new McdcTruthTable.IndependencePair("C3", "TC-QM-04", "TC-QM-06", "C3 independent effect verified against TC-QM-04/06"),
                        "C4", new McdcTruthTable.IndependencePair("C4", "TC-QM-04", "TC-QM-05", "C4 independent effect verified against TC-QM-04/05")
                )
        );
    }
}
