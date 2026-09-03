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
 * Level-A DO-178C / ISO 26262 ASIL-D standard MC/DC Test Suite for QualifiedMortgageSolvencyRule.
 *
 * Formula: (C1 && C2) || (C3 && C4)
 * N=4 conditions -> requires testing independent effect for each condition:
 * - C1: DTI <= 0.43
 * - C2: Credit Score >= 680
 * - C3: Reserve Ratio >= 1.50
 * - C4: Has Guarantor
 */
class QualifiedMortgageSolvencyRuleMcdcTest {

    private QualifiedMortgageSolvencyRule rule;

    @BeforeEach
    void setUp() {
        rule = new QualifiedMortgageSolvencyRule();
    }

    @Test
    @DisplayName("Verify MC/DC Truth Table Structure and Completeness")
    void testTruthTableMetadata() {
        McdcTruthTable table = rule.getMcdcTruthTable();
        assertThat(table.ruleId()).isEqualTo(QualifiedMortgageSolvencyRule.RULE_ID);
        assertThat(table.vectors()).hasSize(6);
        assertThat(table.independenceProofs()).containsKeys("C1", "C2", "C3", "C4");
    }

    /**
     * Executes the formal MC/DC test vectors.
     * TC-QM-01: T T F F -> TRUE  (Baseline for C1, C2)
     * TC-QM-02: F T F F -> FALSE (C1 toggled -> outcome flips to FALSE -> proves C1 independence)
     * TC-QM-03: T F F F -> FALSE (C2 toggled -> outcome flips to FALSE -> proves C2 independence)
     * TC-QM-04: F F T T -> TRUE  (Baseline for C3, C4)
     * TC-QM-05: F F T F -> FALSE (C4 toggled -> outcome flips to FALSE -> proves C4 independence)
     * TC-QM-06: F F F T -> FALSE (C3 toggled -> outcome flips to FALSE -> proves C3 independence)
     */
    @ParameterizedTest(name = "{0}: C1(DTI<={1})={2}, C2(Score>={3})={4}, C3(Res>={5})={6}, C4(Guar)={7} => Expected {8}")
    @CsvSource({
            // TestId, monthlyIncome, monthlyDebt, score, reserves, loanReq, hasGuarantor, expectedOutcome
            // TC-01: DTI=0.40(T), Score=720(T), Reserves=0(F), Guarantor=false(F) -> TRUE
            "TC-QM-01, 10000.0, 4000.0, 720, 0.0, 300000.0, false, true",
            // TC-02: DTI=0.50(F), Score=720(T), Reserves=0(F), Guarantor=false(F) -> FALSE (Proves C1)
            "TC-QM-02, 10000.0, 5000.0, 720, 0.0, 300000.0, false, false",
            // TC-03: DTI=0.40(T), Score=650(F), Reserves=0(F), Guarantor=false(F) -> FALSE (Proves C2)
            "TC-QM-03, 10000.0, 4000.0, 650, 0.0, 300000.0, false, false",
            // TC-04: DTI=0.50(F), Score=650(F), Reserves=40000(T: 40k/(6*4k)=1.67>=1.5), Guarantor=true(T) -> TRUE
            "TC-QM-04, 8000.0, 4000.0, 650, 40000.0, 300000.0, true, true",
            // TC-05: DTI=0.50(F), Score=650(F), Reserves=40000(T), Guarantor=false(F) -> FALSE (Proves C4)
            "TC-QM-05, 8000.0, 4000.0, 650, 40000.0, 300000.0, false, false",
            // TC-06: DTI=0.50(F), Score=650(F), Reserves=5000(F: 5k/(6*4k)=0.21<1.5), Guarantor=true(T) -> FALSE (Proves C3)
            "TC-QM-06, 8000.0, 4000.0, 650, 5000.0, 300000.0, true, false"
    })
    void testMcdcVectors(
            String testId,
            double income,
            double debt,
            int score,
            double reserves,
            double loanAmount,
            boolean hasGuarantor,
            boolean expectedOutcome
    ) {
        DecisionCase decisionCase = DecisionCase.of(
                "APPLICANT-" + testId,
                score,
                income,
                debt,
                reserves,
                loanAmount,
                hasGuarantor,
                0.15
        );

        ProofExplanation explanation = rule.evaluate(decisionCase);

        assertThat(explanation.satisfied())
                .as("MC/DC Vector %s failed expectation", testId)
                .isEqualTo(expectedOutcome);

        if (!expectedOutcome) {
            assertThat(explanation.violation()).isPresent();
            assertThat(explanation.violation().get().ruleId()).isEqualTo(QualifiedMortgageSolvencyRule.RULE_ID);
        } else {
            assertThat(explanation.violation()).isEmpty();
        }
    }

    @Test
    @DisplayName("Formal Independence Pair Check: Every Condition Has An Isolated Outcome Flip")
    void verifyIndependencePairs() {
        Map<String, McdcTruthTable.IndependencePair> proofs = rule.getMcdcTruthTable().independenceProofs();

        // C1 Independence proved by TC-QM-01 and TC-QM-02
        assertThat(proofs.get("C1").trueVectorId()).isEqualTo("TC-QM-01");
        assertThat(proofs.get("C1").falseVectorId()).isEqualTo("TC-QM-02");

        // C2 Independence proved by TC-QM-01 and TC-QM-03
        assertThat(proofs.get("C2").trueVectorId()).isEqualTo("TC-QM-01");
        assertThat(proofs.get("C2").falseVectorId()).isEqualTo("TC-QM-03");

        // C3 Independence proved by TC-QM-04 and TC-QM-06
        assertThat(proofs.get("C3").trueVectorId()).isEqualTo("TC-QM-04");
        assertThat(proofs.get("C3").falseVectorId()).isEqualTo("TC-QM-06");

        // C4 Independence proved by TC-QM-04 and TC-QM-05
        assertThat(proofs.get("C4").trueVectorId()).isEqualTo("TC-QM-04");
        assertThat(proofs.get("C4").falseVectorId()).isEqualTo("TC-QM-05");
    }
}
