package com.verilogic.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilogic.infrastructure.security.BruteForceLockoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End REST API &amp; Security Gateway Integration Tests.
 * Validates underwrite flow, network telemetry headers, error contracts,
 * SQL injection neutralization, and brute-force lockout defenses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ClearLedgerRestControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BruteForceLockoutService lockoutService;

    @BeforeEach
    void setUp() {
        lockoutService.reset("127.0.0.1");
        lockoutService.reset("10.0.0.5");
        lockoutService.reset("192.168.1.99");
    }

    @Test
    @DisplayName("GET /api/v1/health returns 200 OK with security and telemetry information")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("VeriLogic-ClearLedger-Bridge"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @DisplayName("POST /api/v1/underwrite/clearledger certifies clean, balanced bank statement")
    void testUnderwriteCleanStatement_Approved() throws Exception {
        Map<String, Object> payload = Map.of(
                "bankName", "HDFC Bank",
                "accountNumber", "9812739123",
                "accountHolder", "Priya Nair",
                "totalCredits", 95000.0,
                "totalDebits", 21000.0,
                "isBalanced", true,
                "loanAmountRequested", 280000.0,
                "hasGuarantor", true,
                "monthsOfHistory", 6,
                "fraud", Map.of(
                        "overallRiskScore", 5,
                        "riskLevel", "LOW",
                        "documentAuthenticity", 99.5,
                        "isTamperedPDF", false,
                        "averageBankBalance", 62000.0,
                        "salaryDetected", true,
                        "salaryAmount", 15800.0,
                        "inwardBouncesCount", 0
                )
        );

        mockMvc.perform(post("/api/v1/underwrite/clearledger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-VeriLogic-Decision", "CERTIFIED"))
                .andExpect(header().exists("X-VeriLogic-Certificate-Id"))
                .andExpect(header().exists("X-VeriLogic-Merkle-Root"))
                .andExpect(header().exists("X-VeriLogic-Execution-Time-Ms"))
                .andExpect(header().exists("X-VeriLogic-Trace-Id"))
                .andExpect(jsonPath("$.status").value("CERTIFIED"))
                .andExpect(jsonPath("$.caseId", startsWith("CL-")))
                .andExpect(jsonPath("$.merkleRootHash", not(emptyString())));
    }

    @Test
    @DisplayName("POST /api/v1/underwrite/clearledger rejects tampered PDF with forensic safety stop")
    void testUnderwriteTamperedStatement_Rejected() throws Exception {
        Map<String, Object> payload = Map.of(
                "bankName", "HDFC Bank",
                "accountNumber", "9812739123",
                "accountHolder", "Suspicious Actor",
                "totalCredits", 95000.0,
                "totalDebits", 21000.0,
                "isBalanced", true,
                "loanAmountRequested", 280000.0,
                "hasGuarantor", false,
                "monthsOfHistory", 6,
                "fraud", Map.of(
                        "overallRiskScore", 90,
                        "riskLevel", "CRITICAL",
                        "documentAuthenticity", 35.0,
                        "isTamperedPDF", true,
                        "producerDetected", "Canva PDF",
                        "tamperingFlags", java.util.List.of("Font Inconsistency"),
                        "averageBankBalance", 500.0,
                        "salaryDetected", false,
                        "salaryAmount", 0.0,
                        "inwardBouncesCount", 4
                )
        );

        mockMvc.perform(post("/api/v1/underwrite/clearledger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-VeriLogic-Decision", "REJECTED"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /api/v1/underwrite/clearledger neutralizes SQL Injection with 400 Bad Request")
    void testSqlInjectionNeutralization() throws Exception {
        Map<String, Object> payload = Map.of(
                "bankName", "HDFC Bank",
                "accountNumber", "123",
                "accountHolder", "test'; DROP TABLE certificates; --",
                "totalCredits", 10000.0,
                "totalDebits", 5000.0,
                "isBalanced", true
        );

        mockMvc.perform(post("/api/v1/underwrite/clearledger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SQL_INJECTION_DETECTED"))
                .andExpect(jsonPath("$.vector", containsString("DROP")));
    }

    @Test
    @DisplayName("Brute force abuse triggers 403 Forbidden IP lockout")
    void testBruteForceLockoutDefense() throws Exception {
        String attackerIp = "192.168.1.99";

        // Send 5 malicious SQL injection requests to trigger lockout threshold
        for (int i = 0; i < 5; i++) {
            Map<String, Object> payload = Map.of(
                    "bankName", "HDFC",
                    "accountNumber", "123",
                    "accountHolder", "admin' OR 1=1 --"
            );
            mockMvc.perform(post("/api/v1/underwrite/clearledger")
                            .header("X-Forwarded-For", attackerIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest());
        }

        // 6th request from jailed IP must receive 403 Forbidden
        Map<String, Object> cleanPayload = Map.of(
                "bankName", "HDFC",
                "accountNumber", "123",
                "accountHolder", "innocent_attempt",
                "isBalanced", true
        );
        mockMvc.perform(post("/api/v1/underwrite/clearledger")
                        .header("X-Forwarded-For", attackerIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cleanPayload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IP_JAILED_BRUTE_FORCE"))
                .andExpect(jsonPath("$.lockoutRemainingSeconds").value(greaterThan(0)));
    }

    @Test
    @DisplayName("Malformed JSON payload returns structured 400 error with trace ID")
    void testMalformedJsonHandling() throws Exception {
        mockMvc.perform(post("/api/v1/underwrite/clearledger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json-body}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-VeriLogic-Trace-Id"))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON_PAYLOAD"))
                .andExpect(jsonPath("$.path").value("/api/v1/underwrite/clearledger"));
    }

    @Test
    @DisplayName("POST /api/v1/cases/evaluate executes raw text underwriting")
    void testDirectDossierEvaluation() throws Exception {
        Map<String, Object> command = Map.of(
                "rawUnstructuredText", "Applicant: Morgan Vance, Credit Score: 760 FICO, Monthly Income: $14,000, Monthly Debt: $2,500, Requested Loan: $250,000, Guarantor: Yes",
                "requestedBy", "underwriter-lead",
                "dryRun", false
        );

        mockMvc.perform(post("/api/v1/cases/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-VeriLogic-Decision", "CERTIFIED"))
                .andExpect(jsonPath("$.status").value("CERTIFIED"));
    }
}
