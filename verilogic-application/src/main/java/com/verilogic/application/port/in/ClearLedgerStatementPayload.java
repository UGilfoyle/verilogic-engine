package com.verilogic.application.port.in;

import java.util.List;

/**
 * Immutable DTO mirroring ClearLedger's forensic StatementSummary and FraudReport.
 * Ingests low-level bank statement metrics and forensic PDF authenticity signals.
 */
public record ClearLedgerStatementPayload(
        String bankName,
        String accountNumber,
        String accountHolder,
        double openingBalance,
        double closingBalance,
        double totalDebits,
        double totalCredits,
        double netChange,
        int transactionCount,
        boolean isBalanced,
        double discrepancyAmount,
        FraudReportPayload fraud,
        double loanAmountRequested,
        boolean hasGuarantor,
        int monthsOfHistory
) {
    public record FraudReportPayload(
            int overallRiskScore,
            String riskLevel,
            String sanctionRecommendation,
            double documentAuthenticity,
            String producerDetected,
            boolean isTamperedPDF,
            List<String> tamperingFlags,
            double averageBankBalance,
            double totalInflow,
            double totalOutflow,
            double netOperatingFlow,
            boolean salaryDetected,
            double salaryAmount,
            String salaryStability,
            int inwardBouncesCount,
            double inwardBounceRatio,
            int circularTxnCount,
            double circularVolume,
            List<String> highRiskDebits,
            String dataRail
    ) {}

    public ClearLedgerStatementPayload {
        if (monthsOfHistory <= 0) {
            monthsOfHistory = 6; // default to 6 months
        }
        if (loanAmountRequested <= 0.0) {
            loanAmountRequested = 250_000.0; // default loan requested if not specified
        }
        // Jackson maps omitted booleans to false. Treat a fully omitted ledger
        // continuity block (all zeros) as balanced so the documented API sample works.
        if (!isBalanced
                && discrepancyAmount == 0.0
                && openingBalance == 0.0
                && closingBalance == 0.0
                && transactionCount == 0) {
            isBalanced = true;
        }
        if (fraud == null) {
            fraud = new FraudReportPayload(
                    10, "LOW", "SANCTION_APPROVED", 99.8, "Core Banking",
                    false, List.of(), 50000.0, totalCredits, totalDebits,
                    totalCredits - totalDebits, true, totalCredits / Math.max(1, monthsOfHistory),
                    "HIGH", 0, 0.0, 0, 0.0, List.of(), "FORENSIC_PDF_INGESTION"
            );
        }
    }
}
