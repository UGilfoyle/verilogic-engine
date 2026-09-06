package com.verilogic.application.port.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClearLedgerStatementPayloadTest {

    @Test
    @DisplayName("Omitted ledger continuity fields default to a balanced statement")
    void omittedLedgerFieldsDefaultToBalanced() {
        ClearLedgerStatementPayload payload = new ClearLedgerStatementPayload(
                "HDFC Bank",
                "9018420911",
                "Rajesh Sharma",
                0.0,
                0.0,
                16800.0,
                80400.0,
                0.0,
                0,
                false,
                0.0,
                null,
                300000.0,
                true,
                6
        );

        assertThat(payload.isBalanced()).isTrue();
        assertThat(payload.monthsOfHistory()).isEqualTo(6);
        assertThat(payload.fraud()).isNotNull();
        assertThat(payload.fraud().isTamperedPDF()).isFalse();
    }

    @Test
    @DisplayName("Explicit imbalance with a discrepancy is preserved")
    void explicitImbalanceIsPreserved() {
        var fraud = new ClearLedgerStatementPayload.FraudReportPayload(
                60, "HIGH", "MANUAL_REVIEW_REQUIRED", 90.0, "Core Banking",
                false, List.of(), 30000.0, 40000.0, 20000.0, 20000.0,
                true, 8000.0, "MODERATE", 0, 0.0, 0, 0.0, List.of(), "FORENSIC_PDF"
        );

        ClearLedgerStatementPayload payload = new ClearLedgerStatementPayload(
                "Axis Bank", "99887766", "Discrepancy User",
                10000.0, 25000.0, 20000.0, 40000.0, 15000.0,
                80, false, 5000.0, fraud, 200000.0, false, 6
        );

        assertThat(payload.isBalanced()).isFalse();
        assertThat(payload.discrepancyAmount()).isEqualTo(5000.0);
    }
}
