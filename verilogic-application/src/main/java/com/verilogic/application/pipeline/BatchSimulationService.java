package com.verilogic.application.pipeline;

import com.verilogic.application.port.in.EvaluateCaseUseCase;
import com.verilogic.application.port.in.RunSimulationUseCase;
import com.verilogic.application.port.in.SimulationReport;
import com.verilogic.application.port.in.SubmitCaseCommand;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance batch simulation engine.
 * Benchmarks throughput and latency percentiles using Java 21 Virtual Threads (Project Loom).
 */
public class BatchSimulationService implements RunSimulationUseCase {

    private final EvaluateCaseUseCase evaluateCaseUseCase;

    public BatchSimulationService(EvaluateCaseUseCase evaluateCaseUseCase) {
        this.evaluateCaseUseCase = Objects.requireNonNull(evaluateCaseUseCase, "evaluateCaseUseCase cannot be null");
    }

    @Override
    public SimulationReport runSimulation(int batchSize, boolean useVirtualThreads) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }

        ExecutorService executor = useVirtualThreads
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        AtomicInteger certifiedCount = new AtomicInteger(0);
        AtomicInteger reconciledCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        AtomicBoolean allMerkleValid = new AtomicBoolean(true);

        double[] latencies = new double[batchSize];

        List<Callable<Void>> tasks = new ArrayList<>(batchSize);

        for (int i = 0; i < batchSize; i++) {
            final int index = i;
            String scenarioText = generateScenarioText(index);

            tasks.add(() -> {
                long startNanos = System.nanoTime();

                SubmitCaseCommand command = new SubmitCaseCommand(scenarioText, "simulation-worker-" + index, true);
                VerificationCertificate cert = evaluateCaseUseCase.evaluateCase(command);

                long durationNanos = System.nanoTime() - startNanos;
                latencies[index] = durationNanos / 1_000_000.0; // convert to ms

                if (cert.status() == ProofTrace.ProofStatus.CERTIFIED) {
                    certifiedCount.incrementAndGet();
                } else if (cert.status() == ProofTrace.ProofStatus.RECONCILED) {
                    reconciledCount.incrementAndGet();
                } else {
                    rejectedCount.incrementAndGet();
                }

                // Verify cryptographic Merkle root integrity
                String recomputed = VerificationCertificate.computeMerkleRoot(
                        cert.canonicalInputHash(),
                        cert.ruleSetHash(),
                        cert.proofTraceHash(),
                        cert.previousCertificateHash()
                );
                if (!recomputed.equalsIgnoreCase(cert.merkleRootHash())) {
                    allMerkleValid.set(false);
                }

                return null;
            });
        }

        long wallClockStart = System.currentTimeMillis();

        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Simulation interrupted or failed", e);
        } finally {
            executor.shutdown();
        }

        long totalDurationMs = Math.max(1L, System.currentTimeMillis() - wallClockStart);
        double throughput = (batchSize / (double) totalDurationMs) * 1000.0;

        Arrays.sort(latencies);
        double p50 = latencies[(int) (batchSize * 0.50)];
        double p90 = latencies[(int) (batchSize * 0.90)];
        double p99 = latencies[(int) Math.min(batchSize - 1, batchSize * 0.99)];
        double max = latencies[batchSize - 1];

        return new SimulationReport(
                batchSize,
                totalDurationMs,
                Math.round(throughput * 100.0) / 100.0,
                Math.round(p50 * 100.0) / 100.0,
                Math.round(p90 * 100.0) / 100.0,
                Math.round(p99 * 100.0) / 100.0,
                Math.round(max * 100.0) / 100.0,
                certifiedCount.get(),
                reconciledCount.get(),
                rejectedCount.get(),
                allMerkleValid.get(),
                Instant.now()
        );
    }

    private String generateScenarioText(int seed) {
        int scenarioType = seed % 3;
        return switch (scenarioType) {
            case 0 -> String.format(
                    "SYNTHETIC DOSSIER #%d - PRIME\n" +
                    "Applicant: Case-%d\n" +
                    "Credit Score: %d FICO\n" +
                    "Monthly Income: $%.2f\n" +
                    "Monthly Debt: $%.2f\n" +
                    "Liquid Reserves: $%.2f\n" +
                    "Loan Amount: $%.2f\n" +
                    "Corporate Guarantor: Verified\n" +
                    "Risk Score: %.2f",
                    seed, seed, 740 + (seed % 60), 12000.0 + (seed % 4000), 2200.0 + (seed % 600),
                    85000.0 + (seed % 20000), 300000.0, 0.12
            );
            case 1 -> String.format(
                    "SYNTHETIC DOSSIER #%d - BORDERLINE RECONCILE\n" +
                    "Applicant: Case-%d\n" +
                    "Credit Score: %d FICO\n" +
                    "Monthly Income: $%.2f\n" +
                    "Monthly Debt: $%.2f\n" +
                    "Liquid Reserves: $%.2f\n" +
                    "Loan Amount: $%.2f\n" +
                    "Corporate Guarantor: Pending approval upon co-signer\n" +
                    "Risk Score: %.2f",
                    seed, seed, 640 + (seed % 30), 6500.0 + (seed % 1000), 3400.0 + (seed % 300),
                    30000.0 + (seed % 5000), 200000.0, 0.28
            );
            default -> String.format(
                    "SYNTHETIC DOSSIER #%d - HARD VIOLATION\n" +
                    "Applicant: Case-%d\n" +
                    "Credit Score: %d FICO\n" +
                    "Monthly Income: $%.2f\n" +
                    "Monthly Debt: $%.2f\n" +
                    "Liquid Reserves: $%.2f\n" +
                    "Loan Amount: $%.2f\n" +
                    "Corporate Guarantor: None\n" +
                    "Risk Score: %.2f",
                    seed, seed, 580 + (seed % 20), 3800.0 + (seed % 300), 3100.0 + (seed % 200),
                    2500.0, 320000.0, 0.52
            );
        };
    }
}
