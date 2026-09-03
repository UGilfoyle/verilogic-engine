package com.verilogic.ui;

import com.verilogic.ui.views.AuditLedgerView;
import com.verilogic.ui.views.CaseWorkbenchView;
import com.verilogic.ui.views.McdcMatrixView;
import com.verilogic.ui.views.SimulationBenchmarkView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ViewWiringIntegrationTest {

    @Autowired
    private CaseWorkbenchView caseWorkbenchView;

    @Autowired
    private AuditLedgerView auditLedgerView;

    @Autowired
    private McdcMatrixView mcdcMatrixView;

    @Autowired
    private SimulationBenchmarkView simulationBenchmarkView;

    @Test
    void allVaadinViewsWiredAndInstantiatedSuccessfully() {
        assertThat(caseWorkbenchView).isNotNull();
        assertThat(auditLedgerView).isNotNull();
        assertThat(mcdcMatrixView).isNotNull();
        assertThat(simulationBenchmarkView).isNotNull();
    }
}
