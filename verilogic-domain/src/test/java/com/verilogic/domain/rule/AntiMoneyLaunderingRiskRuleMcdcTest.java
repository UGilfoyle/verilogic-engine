package com.verilogic.domain.rule;

import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MC/DC Test Suite for AntiMoneyLaunderingRiskRule (31 U.S.C. § 5313).
 * Formula: C1 && C2
 * C1: RiskScore <= 0.40
 * C2: DebtObligations < Income
 */
class AntiMoneyLaunderingRiskRuleMcdcTest {

    private AntiMoneyLaunderingRiskRule rule;

    @BeforeEach
    void setUp() {
        rule = new AntiMoneyLaunderingRiskRule();
    }

    @Test
    @DisplayName("Verify MC/DC Truth Table Structure")
    void testMetadata() {
        McdcTruthTable table = rule.getMcdcTruthTable();
        assertThat(table.ruleId()).isEqualTo(AntiMoneyLaunderingRiskRule.RULE_ID);
        assertThat(table.vectors()).hasSize(3);
        assertThat(table.independenceProofs()).containsKeys("C1", "C2");
    }

    @ParameterizedTest(name = "{0}: Risk={1}, Debt={2}, Income={3} => Expected={4}")
    @CsvSource({
            // TC-01: Risk=0.20(T), Debt=2000 < Income=8000(T) -> TRUE
            "TC-AML-01, 0.20, 2000.0, 8000.0, true",
            // TC-02: Risk=0.55(F), Debt=2000 < Income=8000(T) -> FALSE (Proves C1)
            "TC-AML-02, 0.55, 2000.0, 8000.0, false",
            // TC-03: Risk=0.20(T), Debt=9000 < Income=8000(F) -> FALSE (Proves C2)
            "TC-AML-03, 0.20, 9000.0, 8000.0, false"
    })
    void testMcdcVectors(
            String testId,
            double riskScore,
            double debt,
            double income,
            boolean expectedOutcome
    ) {
        DecisionCase decisionCase = DecisionCase.of(
                "APPLICANT-" + testId,
                750,
                income,
                debt,
                50000.0,
                200000.0,
                false,
                riskScore
        );

        ProofExplanation explanation = rule.evaluate(decisionCase);

        assertThat(explanation.satisfied())
                .as("MC/DC Vector %s failed expectation", testId)
                .isEqualTo(expectedOutcome);
    }

    @Test
    @DisplayName("Verify Independent Condition Effect Pairs")
    void testIndependencePairs() {
        Map<String, McdcTruthTable.IndependencePair> proofs = rule.getMcdcTruthTable().independenceProofs();

        // C1 proved by TC-AML-01 vs TC-AML-02
        assertThat(proofs.get("C1").trueVectorId()).isEqualTo("TC-AML-01");
        assertThat(proofs.get("C1").falseVectorId()).isEqualTo("TC-AML-02");

        // C2 proved by TC-AML-01 vs TC-AML-03
        assertThat(proofs.get("C2").trueVectorId()).isEqualTo("TC-AML-01");
        assertThat(proofs.get("C2").falseVectorId()).isEqualTo("TC-AML-03");
    }
}
