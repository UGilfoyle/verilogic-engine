package com.verilogic.application.port.in;

/**
 * Inbound port for triggering high-throughput stress simulations (500, 1k, 5k, 10k cases).
 */
public interface RunSimulationUseCase {
    SimulationReport runSimulation(int batchSize, boolean useVirtualThreads);
}
