package com.verilogic.application.port.out;

import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.rule.McdcTruthTable;
import java.util.List;

/**
 * Outbound port for the deterministic mathematical rule and constraint engine.
 */
public interface ConstraintSolverPort {

    /**
     * Executes all active deterministic rules against the case.
     */
    ProofTrace solveAndVerify(DecisionCase decisionCase);

    /**
     * Retrieves the formal MC/DC truth tables for all registered statutory rules.
     */
    List<McdcTruthTable> getActiveMcdcMatrices();

    /**
     * Returns the canonical SHA-256 hash representing the current active rule set version.
     */
    String getRuleSetVersionHash();
}
