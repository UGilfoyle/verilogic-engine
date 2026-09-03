package com.verilogic.application.port.in;

import java.time.Instant;

/**
 * Immutable report summarizing high-throughput batch simulation and latency percentiles.
 */
public record SimulationReport(
        int totalCases,
        long totalDurationMs,
        double throughputPerSecond,
        double p50LatencyMs,
        double p90LatencyMs,
        double p99LatencyMs,
        double maxLatencyMs,
        int certifiedCount,
        int reconciledCount,
        int rejectedCount,
        boolean allMerkleRootsValid,
        Instant completedAt
) {}
