package com.verilogic.infrastructure.adapter.solver;

import com.verilogic.application.port.out.ConstraintSolverPort;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.rule.AntiMoneyLaunderingRiskRule;
import com.verilogic.domain.rule.BaselLiquidityCoverageRule;
import com.verilogic.domain.rule.McdcTruthTable;
import com.verilogic.domain.rule.QualifiedMortgageSolvencyRule;
import com.verilogic.domain.rule.RuleSpecification;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Adapter implementing ConstraintSolverPort.
 * Evaluates registered statutory rules deterministically.
 */
@Component
public class DeterministicConstraintEngineAdapter implements ConstraintSolverPort {

    public static final String RULE_SET_VERSION = "VERILOGIC-RULES-2026.1";

    private final List<RuleSpecification<DecisionCase>> registeredRules;
    private final String ruleSetVersionHash;

    public DeterministicConstraintEngineAdapter() {
        this.registeredRules = List.of(
                new QualifiedMortgageSolvencyRule(),
                new BaselLiquidityCoverageRule(),
                new AntiMoneyLaunderingRiskRule()
        );
        this.ruleSetVersionHash = calculateRuleSetHash();
    }

    public DeterministicConstraintEngineAdapter(List<RuleSpecification<DecisionCase>> customRules) {
        this.registeredRules = List.copyOf(customRules);
        this.ruleSetVersionHash = calculateRuleSetHash();
    }

    @Override
    public ProofTrace solveAndVerify(DecisionCase decisionCase) {
        long startTime = System.nanoTime();
        List<ProofExplanation> explanations = new ArrayList<>();
        boolean allSatisfied = true;

        for (RuleSpecification<DecisionCase> rule : registeredRules) {
            ProofExplanation explanation = rule.evaluate(decisionCase);
            explanations.add(explanation);
            if (!explanation.satisfied()) {
                allSatisfied = false;
            }
        }

        long durationNanos = System.nanoTime() - startTime;
        ProofTrace.ProofStatus status = allSatisfied
                ? ProofTrace.ProofStatus.CERTIFIED
                : ProofTrace.ProofStatus.VIOLATED;

        return new ProofTrace(
                decisionCase.caseId(),
                status,
                explanations,
                Instant.now(),
                RULE_SET_VERSION,
                durationNanos
        );
    }

    @Override
    public List<McdcTruthTable> getActiveMcdcMatrices() {
        return registeredRules.stream()
                .map(RuleSpecification::getMcdcTruthTable)
                .toList();
    }

    @Override
    public String getRuleSetVersionHash() {
        return ruleSetVersionHash;
    }

    private String calculateRuleSetHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder(RULE_SET_VERSION).append(":");
            for (RuleSpecification<DecisionCase> rule : registeredRules) {
                sb.append(rule.ruleId()).append("#").append(rule.formulaDescription()).append(";");
            }
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
