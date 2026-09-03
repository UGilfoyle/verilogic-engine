package com.verilogic.application.pipeline;

import com.verilogic.application.port.in.SimulationReport;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.application.port.out.ConstraintSolverPort;
import com.verilogic.application.port.out.LLMInferencePort;
import com.verilogic.application.port.out.ValkeyCachePort;
import com.verilogic.domain.model.ConstraintViolation;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.rule.AntiMoneyLaunderingRiskRule;
import com.verilogic.domain.rule.BaselLiquidityCoverageRule;
import com.verilogic.domain.rule.QualifiedMortgageSolvencyRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * High-Throughput Stress Simulation Test Suite.
 * Benchmarks 500, 1,000, 5,000, and 10,000 concurrent evaluations using Java 21 Virtual Threads.
 */
class BatchSimulationStressTest {

    private BatchSimulationService simulationService;

    @BeforeEach
    void setUp() {
        // High-speed deterministic test ports
        LLMInferencePort llmPort = new LLMInferencePort() {
            @Override
            public DecisionCase extractSemanticCase(String rawText) {
                int score = extractInt(rawText, "(?i)Credit\\s*Score:\\s*(\\d+)", 720);
                double income = extractDouble(rawText, "(?i)Monthly\\s*Income:\\s*\\$?([\\d,.]+)", 10000.0);
                double debt = extractDouble(rawText, "(?i)Monthly\\s*Debt:\\s*\\$?([\\d,.]+)", 2500.0);
                double reserves = extractDouble(rawText, "(?i)Liquid\\s*Reserves:\\s*\\$?([\\d,.]+)", 50000.0);
                double loan = extractDouble(rawText, "(?i)Loan\\s*Amount:\\s*\\$?([\\d,.]+)", 250000.0);
                boolean guarantor = rawText.contains("Guarantor: Verified");
                double risk = extractDouble(rawText, "(?i)Risk\\s*Score:\\s*([\\d,.]+)", 0.15);

                return DecisionCase.of("APP-" + Math.abs(rawText.hashCode() % 1000000), score, income, debt, reserves, loan, guarantor, risk);
            }

            @Override
            public DecisionCase reconcileWithBoundaryConstraints(DecisionCase currentCase, List<ConstraintViolation> violations, String rawContext) {
                return new DecisionCase(
                        currentCase.caseId(),
                        currentCase.applicantId(),
                        Math.max(680, currentCase.creditScore()),
                        currentCase.monthlyIncome(),
                        currentCase.monthlyIncome() * 0.35,
                        currentCase.liquidReserves() + 20000.0,
                        currentCase.loanAmountRequested(),
                        true,
                        Math.min(0.20, currentCase.riskScore()),
                        currentCase.submittedAt()
                );
            }
        };

        ConstraintSolverPort solverPort = new ConstraintSolverPort() {
            private final QualifiedMortgageSolvencyRule qm = new QualifiedMortgageSolvencyRule();
            private final BaselLiquidityCoverageRule basel = new BaselLiquidityCoverageRule();
            private final AntiMoneyLaunderingRiskRule aml = new AntiMoneyLaunderingRiskRule();

            @Override
            public ProofTrace solveAndVerify(DecisionCase decisionCase) {
                long start = System.nanoTime();
                List<ProofExplanation> list = List.of(
                        qm.evaluate(decisionCase),
                        basel.evaluate(decisionCase),
                        aml.evaluate(decisionCase)
                );
                boolean pass = list.stream().allMatch(ProofExplanation::satisfied);
                return new ProofTrace(
                        decisionCase.caseId(),
                        pass ? ProofTrace.ProofStatus.CERTIFIED : ProofTrace.ProofStatus.VIOLATED,
                        list,
                        Instant.now(),
                        "BENCHMARK-RULES-v1",
                        System.nanoTime() - start
                );
            }

            @Override
            public List<com.verilogic.domain.rule.McdcTruthTable> getActiveMcdcMatrices() {
                return List.of(qm.getMcdcTruthTable(), basel.getMcdcTruthTable(), aml.getMcdcTruthTable());
            }

            @Override
            public String getRuleSetVersionHash() {
                return "RULESET-BENCHMARK-HASH";
            }
        };

        ValkeyCachePort valkeyPort = new ValkeyCachePort() {
            @Override public boolean acquireLock(String key, String leaseToken, long ttlMillis) { return true; }
            @Override public boolean releaseLock(String key, String leaseToken) { return true; }
            @Override public void cacheSessionState(String caseId, String payloadJson, long ttlMillis) {}
            @Override public Optional<String> getSessionState(String caseId) { return Optional.empty(); }
            @Override public void publishDomainEvent(String channel, String eventJson) {}
        };

        AuditStoragePort auditPort = new AuditStoragePort() {
            @Override public void recordCertificate(com.verilogic.domain.model.VerificationCertificate certificate, ProofTrace trace) {}
            @Override public Optional<com.verilogic.domain.model.VerificationCertificate> findCertificateById(String certificateId) { return Optional.empty(); }
            @Override public Optional<com.verilogic.domain.model.VerificationCertificate> findCertificateByCaseId(String caseId) { return Optional.empty(); }
            @Override public List<com.verilogic.domain.model.VerificationCertificate> findRecentCertificates(int limit) { return List.of(); }
            @Override public String getLatestCertificateHash() { return com.verilogic.domain.model.VerificationCertificate.GENESIS_PREVIOUS_HASH; }
            @Override public boolean verifyFullLedgerChain() { return true; }
        };

        NeurosymbolicPipeline pipeline = new NeurosymbolicPipeline(llmPort, solverPort, valkeyPort, auditPort);
        simulationService = new BatchSimulationService(pipeline);
    }

    @ParameterizedTest(name = "Stress Simulation Batch N={0}")
    @ValueSource(ints = {500, 1000, 5000, 10000})
    @DisplayName("Execute Stress Benchmarks (500, 1k, 5k, 10k) with Java 21 Virtual Threads")
    void testScaleSimulations(int batchSize) {
        SimulationReport report = simulationService.runSimulation(batchSize, true);

        System.out.printf(
                """
                ------------------------------------------------------------------------
                [BENCHMARK SCORECARD] Batch Size: N=%d
                ------------------------------------------------------------------------
                Total Duration   : %d ms
                Throughput       : %,.0f operations/sec
                Latency p50      : %.2f ms
                Latency p90      : %.2f ms
                Latency p99      : %.2f ms
                Latency Max      : %.2f ms
                Outcomes         : Certified=%d | Reconciled=%d | Rejected=%d
                Merkle Integrity : %b (100%% cryptographic non-repudiation)
                ------------------------------------------------------------------------
                %n""",
                report.totalCases(),
                report.totalDurationMs(),
                report.throughputPerSecond(),
                report.p50LatencyMs(),
                report.p90LatencyMs(),
                report.p99LatencyMs(),
                report.maxLatencyMs(),
                report.certifiedCount(),
                report.reconciledCount(),
                report.rejectedCount(),
                report.allMerkleRootsValid()
        );

        assertThat(report.totalCases()).isEqualTo(batchSize);
        assertThat(report.allMerkleRootsValid()).isTrue();
        assertThat(report.certifiedCount() + report.reconciledCount() + report.rejectedCount()).isEqualTo(batchSize);
        assertThat(report.throughputPerSecond()).isGreaterThan(500.0);
    }

    private int extractInt(String text, String regex, int defVal) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? Integer.parseInt(m.group(1).replace(",", "")) : defVal;
    }

    private double extractDouble(String text, String regex, double defVal) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? Double.parseDouble(m.group(1).replace(",", "")) : defVal;
    }
}
