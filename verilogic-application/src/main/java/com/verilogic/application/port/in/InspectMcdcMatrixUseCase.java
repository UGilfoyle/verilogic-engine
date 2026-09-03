package com.verilogic.application.port.in;

import com.verilogic.domain.rule.McdcTruthTable;
import java.util.List;

/**
 * Inbound port for compliance officers to inspect active MC/DC truth tables and proofs.
 */
public interface InspectMcdcMatrixUseCase {
    List<McdcTruthTable> getActiveMcdcMatrices();
}
