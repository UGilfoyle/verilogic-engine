package com.verilogic.application.port.out;

import com.verilogic.domain.model.ConstraintViolation;
import com.verilogic.domain.model.DecisionCase;
import java.util.List;

/**
 * Outbound port for semantic model inference (LangChain4j / Ollama).
 * Dependency Inversion: Application defines the contract, infrastructure implements it.
 */
public interface LLMInferencePort {

    /**
     * Extracts an immutable DecisionCase from unstructured input text.
     */
    DecisionCase extractSemanticCase(String rawText);

    /**
     * Re-evaluates applicant parameters against formal mathematical constraint violations.
     * Instructs the LLM to re-extract or re-calibrate parameters strictly within the bounds.
     */
    DecisionCase reconcileWithBoundaryConstraints(
            DecisionCase currentCase,
            List<ConstraintViolation> violations,
            String rawContext
    );
}
