package com.verilogic.application.pipeline;

import com.verilogic.application.port.in.EvaluateCaseUseCase;
import com.verilogic.application.port.in.InspectMcdcMatrixUseCase;
import com.verilogic.application.port.in.InspectProofTraceUseCase;
import com.verilogic.application.port.in.SubmitCaseCommand;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.application.port.out.ConstraintSolverPort;
import com.verilogic.application.port.out.LLMInferencePort;
import com.verilogic.application.port.out.ValkeyCachePort;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;
import com.verilogic.domain.rule.McdcTruthTable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Neurosymbolic Decision Pipeline orchestrating the multi-stage evaluation.
 * Combines Pipeline Pattern, Strategy Pattern, and Distributed Mutex via Valkey.
 */
public class NeurosymbolicPipeline implements EvaluateCaseUseCase, InspectProofTraceUseCase, InspectMcdcMatrixUseCase {

    public static final String VALKEY_EVENT_CHANNEL = "verilogic:events:audit";
    public static final long LOCK_TTL_MILLIS = 30_000L;

    private final LLMInferencePort llmPort;
    private final ConstraintSolverPort solverPort;
    private final ValkeyCachePort valkeyPort;
    private final AuditStoragePort auditPort;
    private final ReconciliationCoordinator reconciliationCoordinator;

    // Fast in-memory cache for proof traces
    private final Map<String, ProofTrace> traceRegistry = new ConcurrentHashMap<>();

    public NeurosymbolicPipeline(
            LLMInferencePort llmPort,
            ConstraintSolverPort solverPort,
            ValkeyCachePort valkeyPort,
            AuditStoragePort auditPort
    ) {
        this.llmPort = Objects.requireNonNull(llmPort, "llmPort cannot be null");
        this.solverPort = Objects.requireNonNull(solverPort, "solverPort cannot be null");
        this.valkeyPort = Objects.requireNonNull(valkeyPort, "valkeyPort cannot be null");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort cannot be null");
        this.reconciliationCoordinator = new ReconciliationCoordinator(llmPort, solverPort);
    }

    @Override
    public VerificationCertificate evaluateCase(SubmitCaseCommand command) {
        // Stage 0: Adversarial Prompt Injection Defense & Token Sanitization
        var scanResult = com.verilogic.application.security.PromptInjectionDetector.scan(command.rawUnstructuredText());
        if (scanResult.threatDetected()) {
            System.err.println("[SECURITY ALERT] Adversarial prompt injection detected: " + scanResult.detectedThreats());
        }
        String cleanContext = scanResult.sanitizedText();

        // Stage 1: Semantic Ingestion via LLM
        DecisionCase initialCase = llmPort.extractSemanticCase(cleanContext);

        String lockKey = "lock:case:" + initialCase.caseId();
        String leaseToken = UUID.randomUUID().toString();

        // Valkey Distributed Lock to prevent duplicate processing
        boolean acquired = valkeyPort.acquireLock(lockKey, leaseToken, LOCK_TTL_MILLIS);
        if (!acquired) {
            throw new IllegalStateException("Case " + initialCase.caseId() + " is currently being evaluated by another node.");
        }

        try {
            // Stage 2: Initial Formal Proof Evaluation
            ProofTrace initialTrace = solverPort.solveAndVerify(initialCase);

            DecisionCase finalCase = initialCase;
            ProofTrace finalTrace = initialTrace;

            boolean hasViolations = initialTrace.explanations().stream()
                    .anyMatch(exp -> !exp.satisfied());

            // Stage 3: Self-Correction Reconciliation Loop (if invariants broken)
            if (hasViolations) {
                ReconciliationCoordinator.ReconciliationResult reconResult = reconciliationCoordinator.coordinate(
                        initialCase,
                        initialTrace,
                        cleanContext
                );

                finalCase = reconResult.finalCase();
                ProofTrace.ProofStatus finalStatus = reconResult.converged()
                        ? ProofTrace.ProofStatus.RECONCILED
                        : ProofTrace.ProofStatus.REJECTED;

                finalTrace = new ProofTrace(
                        finalCase.caseId(),
                        finalStatus,
                        reconResult.finalTrace().explanations(),
                        reconResult.finalTrace().evaluatedAt(),
                        reconResult.finalTrace().ruleSetVersion(),
                        reconResult.finalTrace().evaluationDurationNanos()
                );
            }

            traceRegistry.put(finalCase.caseId(), finalTrace);

            // Stage 4: Cryptographic Merkle Root Attestation & Hash Chaining
            String inputHash = computeSha256(cleanContext);
            String ruleSetHash = solverPort.getRuleSetVersionHash();
            String proofTraceHash = finalTrace.computeProofHash();
            String previousHash = auditPort.getLatestCertificateHash();

            VerificationCertificate certificate = VerificationCertificate.seal(
                    finalCase.caseId(),
                    finalTrace.status(),
                    inputHash,
                    ruleSetHash,
                    proofTraceHash,
                    previousHash
            );

            // Stage 5: Persistence & Event Publishing
            if (!command.dryRun()) {
                auditPort.recordCertificate(certificate, finalTrace);
                String eventPayload = String.format(
                        "{\"event\":\"CaseEvaluated\",\"certificateId\":\"%s\",\"caseId\":\"%s\",\"status\":\"%s\"}",
                        certificate.certificateId(), certificate.caseId(), certificate.status()
                );
                valkeyPort.publishDomainEvent(VALKEY_EVENT_CHANNEL, eventPayload);
            }

            return certificate;
        } finally {
            valkeyPort.releaseLock(lockKey, leaseToken);
        }
    }

    @Override
    public Optional<ProofTrace> inspectProof(String caseId) {
        return Optional.ofNullable(traceRegistry.get(caseId));
    }

    @Override
    public List<McdcTruthTable> getActiveMcdcMatrices() {
        return solverPort.getActiveMcdcMatrices();
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing from JVM", e);
        }
    }
}
