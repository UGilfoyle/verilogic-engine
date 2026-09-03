package com.verilogic.application.pipeline;

import com.verilogic.application.port.in.SubmitCaseCommand;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.application.port.out.ConstraintSolverPort;
import com.verilogic.application.port.out.LLMInferencePort;
import com.verilogic.application.port.out.ValkeyCachePort;
import com.verilogic.domain.model.ConstraintViolation;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeurosymbolicPipelineTest {

    @Mock
    private LLMInferencePort llmPort;
    @Mock
    private ConstraintSolverPort solverPort;
    @Mock
    private ValkeyCachePort valkeyPort;
    @Mock
    private AuditStoragePort auditPort;

    private NeurosymbolicPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new NeurosymbolicPipeline(llmPort, solverPort, valkeyPort, auditPort);
        when(valkeyPort.acquireLock(anyString(), anyString(), anyLong())).thenReturn(true);
        when(valkeyPort.releaseLock(anyString(), anyString())).thenReturn(true);
        when(solverPort.getRuleSetVersionHash()).thenReturn("ruleset-hash-v1.0");
    }

    @Test
    @DisplayName("Golden Path: Semantic Case passes formal proof directly without reconciliation")
    void testGoldenPathCertification() {
        DecisionCase pristineCase = DecisionCase.of("APP-001", 750, 10000.0, 2000.0, 50000.0, 200000.0, true, 0.10);
        when(llmPort.extractSemanticCase(anyString())).thenReturn(pristineCase);

        ProofTrace passedTrace = new ProofTrace(
                pristineCase.caseId(),
                ProofTrace.ProofStatus.CERTIFIED,
                List.of(ProofExplanation.satisfied("RULE-QM", "12 CFR", "DTI <= 0.43", "Satisfied")),
                Instant.now(),
                "v1.0",
                1_000_000L
        );
        when(solverPort.solveAndVerify(pristineCase)).thenReturn(passedTrace);

        SubmitCaseCommand command = new SubmitCaseCommand("Applicant earns 10k/mo with 2k debt and 750 credit score", "underwriter-1", false);
        VerificationCertificate certificate = pipeline.evaluateCase(command);

        assertThat(certificate).isNotNull();
        assertThat(certificate.status()).isEqualTo(ProofTrace.ProofStatus.CERTIFIED);
        assertThat(certificate.caseId()).isEqualTo(pristineCase.caseId());
        assertThat(certificate.merkleRootHash()).isNotBlank();

        verify(auditPort).recordCertificate(eq(certificate), any(ProofTrace.class));
        verify(valkeyPort).publishDomainEvent(eq(NeurosymbolicPipeline.VALKEY_EVENT_CHANNEL), anyString());
    }

    @Test
    @DisplayName("Self-Correction Path: Invariant violation triggers bounded reconciliation and achieves certification")
    void testReconciliationRecovery() {
        DecisionCase violatedCase = DecisionCase.of("APP-002", 640, 5000.0, 3000.0, 1000.0, 200000.0, false, 0.45);
        DecisionCase reconciledCase = DecisionCase.of("APP-002", 640, 5000.0, 3000.0, 35000.0, 150000.0, true, 0.20);

        when(llmPort.extractSemanticCase(anyString())).thenReturn(violatedCase);

        ProofTrace failedTrace = new ProofTrace(
                violatedCase.caseId(),
                ProofTrace.ProofStatus.VIOLATED,
                List.of(ProofExplanation.violated("RULE-QM", "12 CFR", "DTI <= 0.43", "Violated",
                        new ConstraintViolation("RULE-QM", "12 CFR", ConstraintViolation.Severity.HARD_STOP, "DTI high", 0.60, 0.43, "DTI <= 0.43"))),
                Instant.now(),
                "v1.0",
                1_000_000L
        );

        ProofTrace reconciledTrace = new ProofTrace(
                reconciledCase.caseId(),
                ProofTrace.ProofStatus.CERTIFIED,
                List.of(ProofExplanation.satisfied("RULE-QM", "12 CFR", "DTI <= 0.43", "Satisfied with Guarantor")),
                Instant.now(),
                "v1.0",
                1_200_000L
        );

        when(solverPort.solveAndVerify(violatedCase)).thenReturn(failedTrace);
        when(llmPort.reconcileWithBoundaryConstraints(eq(violatedCase), anyList(), anyString())).thenReturn(reconciledCase);
        when(solverPort.solveAndVerify(reconciledCase)).thenReturn(reconciledTrace);

        SubmitCaseCommand command = new SubmitCaseCommand("Re-evaluate loan parameters with co-signer guarantor", "underwriter-2", false);
        VerificationCertificate certificate = pipeline.evaluateCase(command);

        assertThat(certificate.status()).isEqualTo(ProofTrace.ProofStatus.RECONCILED);
        verify(auditPort).recordCertificate(eq(certificate), any(ProofTrace.class));
    }
}
