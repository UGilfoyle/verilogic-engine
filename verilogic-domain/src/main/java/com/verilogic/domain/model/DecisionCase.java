package com.verilogic.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain record representing an applicant case for evaluation.
 * Follows DDD and Clean Code principles: zero framework annotations, 100% thread-safe.
 * Hardened with strict numerical guards and financial half-up currency rounding.
 */
public record DecisionCase(
        String caseId,
        String applicantId,
        int creditScore,
        double monthlyIncome,
        double monthlyDebtObligations,
        double liquidReserves,
        double loanAmountRequested,
        boolean hasGuarantor,
        double riskScore,
        Instant submittedAt
) {
    public DecisionCase {
        Objects.requireNonNull(caseId, "caseId cannot be null");
        Objects.requireNonNull(applicantId, "applicantId cannot be null");
        Objects.requireNonNull(submittedAt, "submittedAt cannot be null");

        validateFinite("monthlyIncome", monthlyIncome);
        validateFinite("monthlyDebtObligations", monthlyDebtObligations);
        validateFinite("liquidReserves", liquidReserves);
        validateFinite("loanAmountRequested", loanAmountRequested);
        validateFinite("riskScore", riskScore);

        if (monthlyIncome < 0.0) {
            throw new IllegalArgumentException("monthlyIncome cannot be negative: " + monthlyIncome);
        }
        if (monthlyDebtObligations < 0.0) {
            throw new IllegalArgumentException("monthlyDebtObligations cannot be negative: " + monthlyDebtObligations);
        }
        if (liquidReserves < 0.0) {
            throw new IllegalArgumentException("liquidReserves cannot be negative: " + liquidReserves);
        }
        if (loanAmountRequested < 0.0) {
            throw new IllegalArgumentException("loanAmountRequested cannot be negative: " + loanAmountRequested);
        }
        if (riskScore < 0.0 || riskScore > 1.0) {
            throw new IllegalArgumentException("riskScore must be in range [0.0, 1.0]: " + riskScore);
        }
        if (creditScore < 300 || creditScore > 850) {
            throw new IllegalArgumentException("creditScore must be in valid FICO range [300, 850]: " + creditScore);
        }

        // Standardize financial amounts to exact cents
        monthlyIncome = roundCents(monthlyIncome);
        monthlyDebtObligations = roundCents(monthlyDebtObligations);
        liquidReserves = roundCents(liquidReserves);
        loanAmountRequested = roundCents(loanAmountRequested);
        riskScore = roundScale(riskScore, 4);
    }

    /**
     * Factory method for creating a fresh case with generated ID and timestamp.
     */
    public static DecisionCase of(
            String applicantId,
            int creditScore,
            double monthlyIncome,
            double monthlyDebtObligations,
            double liquidReserves,
            double loanAmountRequested,
            boolean hasGuarantor,
            double riskScore
    ) {
        return new DecisionCase(
                UUID.randomUUID().toString(),
                applicantId,
                creditScore,
                monthlyIncome,
                monthlyDebtObligations,
                liquidReserves,
                loanAmountRequested,
                hasGuarantor,
                riskScore,
                Instant.now()
        );
    }

    /**
     * Computes Debt-To-Income (DTI) ratio.
     * Guarded against division by zero.
     */
    public double debtToIncomeRatio() {
        if (monthlyIncome == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return roundScale(monthlyDebtObligations / monthlyIncome, 4);
    }

    /**
     * Computes Liquid Reserve Ratio relative to 6 months of debt obligations.
     */
    public double reserveRatio() {
        double sixMonthObligations = monthlyDebtObligations * 6.0;
        if (sixMonthObligations == 0.0) {
            return liquidReserves > 0 ? 10.0 : 0.0;
        }
        return roundScale(liquidReserves / sixMonthObligations, 4);
    }

    private static void validateFinite(String fieldName, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be a finite numerical value, got: " + value);
        }
    }

    public static double roundCents(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double roundScale(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
