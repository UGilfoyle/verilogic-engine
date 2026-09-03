package com.verilogic.application.pipeline;

import com.verilogic.application.port.out.LLMInferencePort;
import com.verilogic.application.port.out.ConstraintSolverPort;
import com.verilogic.domain.model.ConstraintViolation;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;
import com.verilogic.domain.model.ProofTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * State Machine managing the Neurosymbolic Self-Correction Reconciliation Loop.
 * Enforces strict convergence boundaries (K <= 3 iterations) to prevent infinite loops.
 */
public class ReconciliationCoordinator {

    public static final int MAX_RECONCILIATION_ATTEMPTS = 3;

    private final LLMInferencePort llmPort;
    private final ConstraintSolverPort solverPort;

    public record ReconciliationResult(
            DecisionCase finalCase,
            ProofTrace finalTrace,
            int totalIterations,
            boolean converged
    ) {}

    public record IterationSnapshot(
            int iterationIndex,
            DecisionCase decisionCase,
            List<ConstraintViolation> violations
    ) {}

    public ReconciliationCoordinator(LLMInferencePort llmPort, ConstraintSolverPort solverPort) {
        this.llmPort = Objects.requireNonNull(llmPort, "llmPort cannot be null");
        this.solverPort = Objects.requireNonNull(solverPort, "solverPort cannot be null");
    }

    /**
     * Executes the bounded reconciliation convergence loop.
     */
    public ReconciliationResult coordinate(
            DecisionCase initialCase,
            ProofTrace initialTrace,
            String rawContext
    ) {
        DecisionCase currentCase = initialCase;
        ProofTrace currentTrace = initialTrace;
        List<IterationSnapshot> history = new ArrayList<>();

        int iteration = 0;

        while (iteration < MAX_RECONCILIATION_ATTEMPTS) {
            List<ConstraintViolation> activeViolations = currentTrace.explanations().stream()
                    .map(ProofExplanation::violation)
                    .flatMap(Optional::stream)
                    .toList();

            if (activeViolations.isEmpty()) {
                // Converged successfully!
                return new ReconciliationResult(currentCase, currentTrace, iteration, true);
            }

            history.add(new IterationSnapshot(iteration, currentCase, activeViolations));
            iteration++;

            // Re-evaluate with mathematical constraint boundary feedback
            currentCase = llmPort.reconcileWithBoundaryConstraints(currentCase, activeViolations, rawContext);
            currentTrace = solverPort.solveAndVerify(currentCase);
        }

        // Final check after loop limit
        boolean converged = currentTrace.explanations().stream().allMatch(ProofExplanation::satisfied);

        return new ReconciliationResult(currentCase, currentTrace, iteration, converged);
    }
}
