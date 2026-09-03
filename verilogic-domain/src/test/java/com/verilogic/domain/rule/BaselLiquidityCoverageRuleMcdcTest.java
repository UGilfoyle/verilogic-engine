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
 * MC/DC Test Suite for BaselLiquidityCoverageRule (Basel III LCR BCBS d238).
 * Formula: C1 && (C2 || C3)
 * C1: Income >= 5000
 * C2: Reserves >= 0.20 * Loan
 * C3: RiskScore <= 0.25
 */
class BaselLiquidityCoverageRuleMcdcTest {

    private BaselLiquidityCoverageRule rule;

    @BeforeEach
    void setUp() {
        rule = new BaselLiquidityCoverageRule();
    }

    @Test
    @DisplayName("Verify MC/DC Truth Table Structure")
    void testMetadata() {
        McdcTruthTable table = rule.getMcdcTruthTable();
        assertThat(table.ruleId()).isEqualTo(BaselLiquidityCoverageRule.RULE_ID);
        assertThat(table.vectors()).hasSize(4);
        assertThat(table.independenceProofs()).containsKeys("C1", "C2", "C3");
    }

    @ParameterizedTest(name = "{0}: Income={1}, Reserves={2}, Loan={3}, Risk={4} => Expected={5}")
    @CsvSource({
            // TC-01: Income=6000(T), Reserves=65000(T >= 60k), Risk=0.35(F) -> TRUE (Baseline)
            "TC-LCR-01, 6000.0, 65000.0, 300000.0, 0.35, true",
            // TC-02: Income=4500(F), Reserves=65000(T), Risk=0.35(F) -> FALSE (Proves C1)
            "TC-LCR-02, 4500.0, 65000.0, 300000.0, 0.35, false",
            // TC-03: Income=6000(T), Reserves=10000(F < 60k), Risk=0.35(F) -> FALSE (Proves C2)
            "TC-LCR-03, 6000.0, 10000.0, 300000.0, 0.35, false",
            // TC-04: Income=6000(T), Reserves=10000(F), Risk=0.20(T <= 0.25) -> TRUE (Proves C3)
            "TC-LCR-04, 6000.0, 10000.0, 300000.0, 0.20, true"
    })
    void testMcdcVectors(
            String testId,
            double income,
            double reserves,
            double loanAmount,
            double riskScore,
            boolean expectedOutcome
    ) {
        DecisionCase decisionCase = DecisionCase.of(
                "APPLICANT-" + testId,
                700,
                income,
                1500.0,
                reserves,
                loanAmount,
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

        // C1 proved by TC-LCR-01 vs TC-LCR-02
        assertThat(proofs.get("C1").trueVectorId()).isEqualTo("TC-LCR-01");
        assertThat(proofs.get("C1").falseVectorId()).isEqualTo("TC-LCR-02");

        // C2 proved by TC-LCR-01 vs TC-LCR-03
        assertThat(proofs.get("C2").trueVectorId()).isEqualTo("TC-LCR-01");
        assertThat(proofs.get("C2").falseVectorId()).isEqualTo("TC-LCR-03");

        // C3 proved by TC-LCR-04 vs TC-LCR-03
        assertThat(proofs.get("C3").trueVectorId()).isEqualTo("TC-LCR-04");
        assertThat(proofs.get("C3").falseVectorId()).isEqualTo("TC-LCR-03");
    }
}
