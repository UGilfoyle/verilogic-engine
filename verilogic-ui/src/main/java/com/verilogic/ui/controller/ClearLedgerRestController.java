package com.verilogic.ui.controller;

import com.verilogic.application.port.in.ClearLedgerStatementPayload;
import com.verilogic.application.port.in.ClearLedgerUnderwritingUseCase;
import com.verilogic.application.port.in.EvaluateCaseUseCase;
import com.verilogic.application.port.out.AuditStoragePort;
import com.verilogic.domain.model.VerificationCertificate;
import com.verilogic.infrastructure.security.BruteForceLockoutService;
import com.verilogic.infrastructure.security.SqlInjectionDefenseGuard;
import com.verilogic.ui.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Unified REST API Gateway bridging ClearLedger's forensic statement engine
 * with VeriLogic's deterministic statutory decision brain.
 * Hardened against DDoS, Brute-Force jailing, and SQL Injection attacks.
 * Features production-grade SLF4J observability and audit network headers.
 */
@RestController
@RequestMapping("/api/v1")
public class ClearLedgerRestController {

    private static final Logger log = LoggerFactory.getLogger(ClearLedgerRestController.class);

    private final ClearLedgerUnderwritingUseCase clearLedgerUseCase;
    private final EvaluateCaseUseCase evaluateCaseUseCase;
    private final BruteForceLockoutService lockoutService;
    private final ClientIpResolver clientIpResolver;
    private final AuditStoragePort auditStoragePort;

    public ClearLedgerRestController(
            ClearLedgerUnderwritingUseCase clearLedgerUseCase,
            EvaluateCaseUseCase evaluateCaseUseCase,
            BruteForceLockoutService lockoutService,
            ClientIpResolver clientIpResolver,
            AuditStoragePort auditStoragePort
    ) {
        this.clearLedgerUseCase = Objects.requireNonNull(clearLedgerUseCase, "clearLedgerUseCase cannot be null");
        this.evaluateCaseUseCase = Objects.requireNonNull(evaluateCaseUseCase, "evaluateCaseUseCase cannot be null");
        this.lockoutService = Objects.requireNonNull(lockoutService, "lockoutService cannot be null");
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver, "clientIpResolver cannot be null");
        this.auditStoragePort = Objects.requireNonNull(auditStoragePort, "auditStoragePort cannot be null");
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "VeriLogic-ClearLedger-Bridge",
                "version", "1.0.0",
                "security", "DDoS-TokenBucket + BruteForce-Lockout + SQLi-Guard Active",
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Ingests ClearLedger's StatementSummary and executes formal statutory underwriting.
     */
    @PostMapping("/underwrite/clearledger")
    public ResponseEntity<?> underwriteClearLedgerStatement(
            @RequestBody ClearLedgerStatementPayload payload,
            @RequestParam(defaultValue = "false") boolean dryRun,
            HttpServletRequest request
    ) {
        long startTime = System.nanoTime();
        String clientIp = clientIpResolver.resolve(request);
        String traceId = "trc-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("--> [API REQUEST] Inbound Underwriting | Client IP: {} | Bank: '{}' | Account: '{}' | Holder: '{}' | Amount: ${} | Balanced: {} | Trace: {}",
                clientIp, payload.bankName(), payload.accountNumber(), payload.accountHolder(), payload.loanAmountRequested(), payload.isBalanced(), traceId);

        // 1. Brute-Force Lockout Defense Check
        if (lockoutService.isLockedOut(clientIp)) {
            long remaining = lockoutService.getRemainingLockoutSeconds(clientIp);
            log.warn("[SECURITY ALERT] [403 FORBIDDEN] Jailed Client IP [{}] attempted access on URI [{}]. Lockout expires in {}s",
                    clientIp, request.getRequestURI(), remaining);

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", Instant.now().toString());
            errorBody.put("status", HttpStatus.FORBIDDEN.value());
            errorBody.put("error", "Forbidden - Client IP Jailed");
            errorBody.put("code", "IP_JAILED_BRUTE_FORCE");
            errorBody.put("message", "Too many malicious/failed requests. IP is jailed. Remaining: " + remaining + "s");
            errorBody.put("lockoutRemainingSeconds", remaining);
            errorBody.put("path", request.getRequestURI());
            errorBody.put("traceId", traceId);

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header("X-VeriLogic-Trace-Id", traceId)
                    .body(errorBody);
        }

        // 2. SQL Injection Inspection on payload text fields
        var sqlCheck = SqlInjectionDefenseGuard.scan(payload.toString());
        if (sqlCheck.injectionDetected()) {
            lockoutService.recordStrike(clientIp);
            log.warn("[SECURITY ALERT] [400 BAD_REQUEST] SQL Injection attempt detected from Client IP [{}] on URI [{}] | Vector: {}",
                    clientIp, request.getRequestURI(), sqlCheck.detectedVector());

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", Instant.now().toString());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            errorBody.put("error", "Bad Request - Malicious Payload");
            errorBody.put("code", "SQL_INJECTION_DETECTED");
            errorBody.put("message", "Input payload contains disallowed SQL injection patterns.");
            errorBody.put("vector", sqlCheck.detectedVector());
            errorBody.put("path", request.getRequestURI());
            errorBody.put("traceId", traceId);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-VeriLogic-Trace-Id", traceId)
                    .body(errorBody);
        }

        // 3. Execute Statutory Underwriting & Merkle Attestation
        VerificationCertificate certificate = clearLedgerUseCase.underwriteStatement(payload, dryRun);
        double elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0;

        if (certificate.status() == com.verilogic.domain.model.ProofTrace.ProofStatus.REJECTED) {
            log.warn("<-- [API DECISION: REJECTED] Case [{}] | Decision: REJECTED | Latency: {}ms | Proof: {} | Trace: {}",
                    certificate.caseId(), String.format("%.2f", elapsedMs), certificate.merkleRootHash(), traceId);
        } else {
            log.info("<-- [API DECISION: {}] Case [{}] | Decision: {} | Latency: {}ms | Proof: {} | Trace: {}",
                    certificate.status(), certificate.caseId(), certificate.status(), String.format("%.2f", elapsedMs), certificate.merkleRootHash(), traceId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-VeriLogic-Decision", certificate.status().name());
        headers.add("X-VeriLogic-Certificate-Id", certificate.certificateId().toString());
        headers.add("X-VeriLogic-Case-Id", certificate.caseId());
        headers.add("X-VeriLogic-Merkle-Root", certificate.merkleRootHash());
        headers.add("X-VeriLogic-Execution-Time-Ms", String.format("%.2f", elapsedMs));
        headers.add("X-VeriLogic-Trace-Id", traceId);

        return ResponseEntity.ok()
                .headers(headers)
                .body(certificate);
    }

    /**
     * Direct submission of raw applicant dossier text.
     */
    @PostMapping("/cases/evaluate")
    public ResponseEntity<?> evaluateRawCase(
            @RequestBody EvaluateCaseRequest requestBody,
            HttpServletRequest request
    ) {
        long startTime = System.nanoTime();
        String clientIp = clientIpResolver.resolve(request);
        String traceId = "trc-" + UUID.randomUUID().toString().substring(0, 8);
        var command = requestBody.toCommand();

        log.info("--> [API REQUEST] Direct Dossier Submission | Client IP: {} | Requested By: '{}' | Trace: {}",
                clientIp, command.requestedBy(), traceId);

        if (lockoutService.isLockedOut(clientIp)) {
            long remaining = lockoutService.getRemainingLockoutSeconds(clientIp);
            log.warn("[SECURITY ALERT] [403 FORBIDDEN] Jailed Client IP [{}] attempted access on URI [{}]", clientIp, request.getRequestURI());

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", Instant.now().toString());
            errorBody.put("status", HttpStatus.FORBIDDEN.value());
            errorBody.put("error", "Forbidden - Client IP Jailed");
            errorBody.put("code", "IP_JAILED_BRUTE_FORCE");
            errorBody.put("message", "Too many malicious/failed requests. Lockout expires in " + remaining + " seconds.");
            errorBody.put("traceId", traceId);

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header("X-VeriLogic-Trace-Id", traceId)
                    .body(errorBody);
        }

        var sqlCheck = SqlInjectionDefenseGuard.scan(command.rawUnstructuredText());
        if (sqlCheck.injectionDetected()) {
            lockoutService.recordStrike(clientIp);
            log.warn("[SECURITY ALERT] [400 BAD_REQUEST] SQL Injection detected from IP [{}] | Vector: {}", clientIp, sqlCheck.detectedVector());

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", Instant.now().toString());
            errorBody.put("status", HttpStatus.BAD_REQUEST.value());
            errorBody.put("error", "Bad Request - Malicious Payload");
            errorBody.put("code", "SQL_INJECTION_DETECTED");
            errorBody.put("message", "Input text contains disallowed SQL injection patterns.");
            errorBody.put("vector", sqlCheck.detectedVector());
            errorBody.put("traceId", traceId);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-VeriLogic-Trace-Id", traceId)
                    .body(errorBody);
        }

        VerificationCertificate certificate = evaluateCaseUseCase.evaluateCase(command);
        double elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0;

        log.info("<-- [API DECISION: {}] Case [{}] | Latency: {}ms | Merkle: {} | Trace: {}",
                certificate.status(), certificate.caseId(), String.format("%.2f", elapsedMs), certificate.merkleRootHash(), traceId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-VeriLogic-Decision", certificate.status().name());
        headers.add("X-VeriLogic-Certificate-Id", certificate.certificateId().toString());
        headers.add("X-VeriLogic-Case-Id", certificate.caseId());
        headers.add("X-VeriLogic-Merkle-Root", certificate.merkleRootHash());
        headers.add("X-VeriLogic-Execution-Time-Ms", String.format("%.2f", elapsedMs));
        headers.add("X-VeriLogic-Trace-Id", traceId);

        return ResponseEntity.ok()
                .headers(headers)
                .body(certificate);
    }

    @GetMapping("/certificates/{certificateId}")
    public ResponseEntity<?> getCertificate(@PathVariable String certificateId) {
        return auditStoragePort.findCertificateById(certificateId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "code", "CERTIFICATE_NOT_FOUND",
                        "message", "No certificate found for id " + certificateId
                )));
    }

    @GetMapping("/certificates/case/{caseId}")
    public ResponseEntity<?> getCertificateByCase(@PathVariable String caseId) {
        return auditStoragePort.findCertificateByCaseId(caseId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "code", "CERTIFICATE_NOT_FOUND",
                        "message", "No certificate found for case " + caseId
                )));
    }

    @GetMapping("/ledger/recent")
    public ResponseEntity<?> recentCertificates(@RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return ResponseEntity.ok(Map.of(
                "count", auditStoragePort.findRecentCertificates(safeLimit).size(),
                "chainIntact", auditStoragePort.verifyFullLedgerChain(),
                "certificates", auditStoragePort.findRecentCertificates(safeLimit)
        ));
    }

    @GetMapping("/ledger/verify")
    public ResponseEntity<Map<String, Object>> verifyLedger() {
        boolean intact = auditStoragePort.verifyFullLedgerChain();
        return ResponseEntity.ok(Map.of(
                "chainIntact", intact,
                "tipHash", auditStoragePort.getLatestCertificateHash()
        ));
    }
}
