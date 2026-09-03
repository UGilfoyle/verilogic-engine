package com.verilogic.application.port.in;

import com.verilogic.domain.model.VerificationCertificate;

/**
 * Inbound port for underwriting forensic bank statements ingested from ClearLedger.
 */
public interface ClearLedgerUnderwritingUseCase {
    VerificationCertificate underwriteStatement(ClearLedgerStatementPayload payload, boolean dryRun);
}
