package com.verilogic.application.pipeline;

import com.verilogic.application.port.in.ClearLedgerStatementPayload;
import com.verilogic.application.port.in.ClearLedgerUnderwritingUseCase;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.application.port.out.ConstraintSolverPort;
import com.verilogic.application.port.out.ValkeyCachePort;
import com.verilogic.domain.model.DecisionCase;
import com.verilogic.domain.model.ProofExplanation;
import com.verilogic.domain.model.ProofTrace;
import com.verilogic.domain.model.VerificationCertificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Enterprise service bridging ClearLedger's forensic statement intelligence
 * with VeriLogic's deterministic statutory solver and Merkle chain.
 */
public class ClearLedgerUnderwritingService implements ClearLedgerUnderwritingUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClearLedgerUnderwritingService.class);

    private final ConstraintSolverPort solverPort;
    private final ValkeyCachePort valkeyPort;
    private final AuditStoragePort auditPort;

    public ClearLedgerUnderwritingService(
            ConstraintSolverPort solverPort,
            ValkeyCachePort valkeyPort,
            AuditStoragePort auditPort
    ) {
        this.solverPort = Objects.requireNonNull(solverPort, "solverPort cannot be null");
        this.valkeyPort = Objects.requireNonNull(valkeyPort, "valkeyPort cannot be null");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort cannot be null");
    }

    @Override
    public VerificationCertificate underwriteStatement(ClearLedgerStatementPayload payload, boolean dryRun) {
        Objects.requireNonNull(payload, "payload cannot be null");

        String caseId = "CL-" + UUID.randomUUID().toString().substring(0, 8);
        String applicantId = payload.accountHolder() != null && !payload.accountHolder().isBlank()
                ? payload.accountHolder()
                : "ACCT-" + payload.accountNumber();

        // 1. HARD FORENSIC SAFETY STOP: Check for PDF Tampering or Balance Ledger Invariant Discontinuity
        if (payload.fraud().isTamperedPDF() || !payload.isBalanced()) {
            List<String> flags = new ArrayList<>();
            if (payload.fraud().isTamperedPDF()) {
                flags.add("PDF Tampering Detected by ClearLedger (Producer: " + payload.fraud().producerDetected() + ")");
                flags.addAll(payload.fraud().tamperingFlags());
            }
            if (!payload.isBalanced()) {
                flags.add(String.format("Ledger Invariant Discontinuity: Discrepancy of %.2f detected across transactions", payload.discrepancyAmount()));
            }

            log.warn("[FORENSIC SAFETY STOP] Case [{}] REJECTED for Applicant [{}]. Tampering/Discontinuity detected: {}",
                    caseId, applicantId, String.join("; ", flags));

            com.verilogic.domain.model.ConstraintViolation violation = new com.verilogic.domain.model.ConstraintViolation(
                    "RULE-CLEARLEDGER-FORENSIC-TAMPER-001",
                    "Statutory Banking Invariant: Strict Authenticity & Ledger Continuity",
                    com.verilogic.domain.model.ConstraintViolation.Severity.HARD_STOP,
                    String.join(" | ", flags),
                    payload.fraud().documentAuthenticity(),
                    99.0,
                    "IsTamperedPDF == false && IsBalanced == true"
            );

            ProofExplanation forensicViolation = ProofExplanation.violated(
                    "RULE-CLEARLEDGER-FORENSIC-TAMPER-001",
                    "Statutory Banking Invariant: Strict Authenticity & Ledger Continuity",
                    "IsTamperedPDF == false && IsBalanced == true",
                    "VIOLATION: " + String.join(" | ", flags),
                    violation
            );

            ProofTrace rejectionTrace = new ProofTrace(
                    caseId,
                    ProofTrace.ProofStatus.REJECTED,
                    List.of(forensicViolation),
                    Instant.now(),
                    solverPort.getRuleSetVersionHash(),
                    100_000L
            );

            String inputHash = computeSha256(payload.toString());
            String prevHash = auditPort.getLatestCertificateHash();

            VerificationCertificate cert = VerificationCertificate.seal(
                    caseId,
                    ProofTrace.ProofStatus.REJECTED,
                    inputHash,
                    solverPort.getRuleSetVersionHash(),
                    rejectionTrace.computeProofHash(),
                    prevHash
            );

            if (!dryRun) {
                auditPort.recordCertificate(cert, rejectionTrace);
            }
            return cert;
        }

        // 2. Derive Verified Financial Metrics from ClearLedger
        int months = Math.max(1, payload.monthsOfHistory());
        double monthlyIncome = payload.fraud().salaryDetected() && payload.fraud().salaryAmount() > 0.0
                ? payload.fraud().salaryAmount()
                : payload.totalCredits() / months;

        double monthlyDebt = payload.totalDebits() / months;
        double liquidReserves = Math.max(0.0, payload.fraud().averageBankBalance());
        double loanRequested = payload.loanAmountRequested() > 0.0 ? payload.loanAmountRequested() : 250_000.0;
        boolean guarantor = payload.hasGuarantor();
        double riskScore = Math.min(1.0, Math.max(0.0, payload.fraud().overallRiskScore() / 100.0));

        // Estimate FICO: base 740, minus penalty for bounces & risk
        int estimatedFico = Math.max(350, Math.min(850, 750 - (int) (riskScore * 150) - (payload.fraud().inwardBouncesCount() * 30)));

        DecisionCase verifiedCase = DecisionCase.of(
                applicantId,
                estimatedFico,
                monthlyIncome,
                monthlyDebt,
                liquidReserves,
                loanRequested,
                guarantor,
                riskScore
        );

        // 3. Solve Deterministic Statutory Rules (QM 1026.43e, Basel LCR, AML 5313)
        ProofTrace trace = solverPort.solveAndVerify(verifiedCase);
        log.info("[POLICY SOLVER] Case [{}] passed forensic check. Evaluated statutory rules. Outcome: [{}] (FICO: {}, Income: ${}/mo, Debt: ${}/mo)",
                caseId, trace.status(), estimatedFico, String.format("%.2f", monthlyIncome), String.format("%.2f", monthlyDebt));

        // 4. Seal into Cryptographic Merkle Ledger Chain
        String inputHash = computeSha256(payload.toString());
        String prevHash = auditPort.getLatestCertificateHash();

        VerificationCertificate cert = VerificationCertificate.seal(
                caseId,
                trace.status(),
                inputHash,
                solverPort.getRuleSetVersionHash(),
                trace.computeProofHash(),
                prevHash
        );

        log.info("[AUDIT LEDGER] Case [{}] sealed into cryptographic ledger. Proof: {} | Tip: {}",
                caseId, cert.merkleRootHash(), prevHash.substring(0, Math.min(16, prevHash.length())));

        if (!dryRun) {
            auditPort.recordCertificate(cert, trace);
            String eventPayload = String.format(
                    "{\"event\":\"ClearLedgerCaseUnderwritten\",\"certificateId\":\"%s\",\"caseId\":\"%s\",\"status\":\"%s\"}",
                    cert.certificateId(), cert.caseId(), cert.status()
            );
            valkeyPort.publishDomainEvent(NeurosymbolicPipeline.VALKEY_EVENT_CHANNEL, eventPayload);
        }

        return cert;
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 missing", e);
        }
    }
}
