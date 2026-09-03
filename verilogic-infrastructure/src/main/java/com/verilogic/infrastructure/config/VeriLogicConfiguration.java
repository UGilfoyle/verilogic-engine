package com.verilogic.infrastructure.config;

import com.verilogic.application.pipeline.NeurosymbolicPipeline;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.application.port.out.ConstraintSolverPort;
import com.verilogic.application.port.out.LLMInferencePort;
import com.verilogic.application.port.out.ValkeyCachePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring Boot configuration wiring up Hexagonal Ports &amp; Adapters.
 * Adheres strictly to the Dependency Inversion Principle.
 */
@Configuration
public class VeriLogicConfiguration {

    @Bean
    @Primary
    public NeurosymbolicPipeline neurosymbolicPipeline(
            LLMInferencePort llmPort,
            ConstraintSolverPort solverPort,
            ValkeyCachePort valkeyPort,
            AuditStoragePort auditPort
    ) {
        return new NeurosymbolicPipeline(llmPort, solverPort, valkeyPort, auditPort);
    }

    @Bean
    public com.verilogic.application.port.in.RunSimulationUseCase runSimulationUseCase(NeurosymbolicPipeline pipeline) {
        return new com.verilogic.application.pipeline.BatchSimulationService(pipeline);
    }

    @Bean
    public com.verilogic.application.port.in.ClearLedgerUnderwritingUseCase clearLedgerUnderwritingUseCase(
            ConstraintSolverPort solverPort,
            ValkeyCachePort valkeyPort,
            AuditStoragePort auditPort
    ) {
        return new com.verilogic.application.pipeline.ClearLedgerUnderwritingService(solverPort, valkeyPort, auditPort);
    }

    @Bean
    public com.verilogic.application.port.in.EvaluateCaseUseCase evaluateCaseUseCase(NeurosymbolicPipeline pipeline) {
        return pipeline;
    }

    @Bean
    public com.verilogic.application.port.in.InspectProofTraceUseCase inspectProofTraceUseCase(NeurosymbolicPipeline pipeline) {
        return pipeline;
    }

    @Bean
    public com.verilogic.application.port.in.InspectMcdcMatrixUseCase inspectMcdcMatrixUseCase(NeurosymbolicPipeline pipeline) {
        return pipeline;
    }
}
