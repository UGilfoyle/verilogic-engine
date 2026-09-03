package com.verilogic.application.port.in;

import com.verilogic.domain.model.ProofTrace;
import java.util.Optional;

/**
 * Inbound port for retrieving proof traces by case ID.
 */
public interface InspectProofTraceUseCase {
    Optional<ProofTrace> inspectProof(String caseId);
}
