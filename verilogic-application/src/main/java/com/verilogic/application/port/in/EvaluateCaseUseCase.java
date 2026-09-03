package com.verilogic.application.port.in;

import com.verilogic.domain.model.VerificationCertificate;

/**
 * Primary inbound port for executing the neurosymbolic verification pipeline.
 */
public interface EvaluateCaseUseCase {
    VerificationCertificate evaluateCase(SubmitCaseCommand command);
}
