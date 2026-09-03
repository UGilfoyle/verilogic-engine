# VeriLogic

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.3-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vaadin 24](https://img.shields.io/badge/Vaadin-24.4-00B4F0?style=flat&logo=vaadin&logoColor=white)](https://vaadin.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> **Institutional Underwriting &amp; Regulatory Compliance Engine**  
> Fast, reliable loan decisioning paired with forensic bank statement verification and tamper-proof audit receipts.

---

## Overview

VeriLogic is an automated credit underwriting platform built for commercial banks, credit unions, and fintech lenders. It automates credit policy evaluation, verifies bank statements against fraud, and generates permanent, audit-ready records for bank examiners and regulators.

By combining deterministic rule verification with forensic bank statement intelligence, VeriLogic enables financial institutions to disburse capital with full regulatory confidence in under one millisecond.

---

## Key Capabilities

- **Instant Credit Decisioning:** Evaluates lending criteria, debt-to-income limits, and cash reserve requirements in `< 1 millisecond`.
- **Forensic Bank Statement Ingestion:** Directly connects with forensic engines (like ClearLedger) to detect tampered PDFs, ghost transactions, and balance discontinuities.
- **Automated Statutory Compliance:** Built-in verification for:
  - **Qualified Mortgage (12 CFR § 1026.43(e)):** Hard ceiling on 43% DTI unless supported by verified cash reserves and guarantors.
  - **Basel III Liquidity Coverage (BCBS d238):** Solvency and liquid reserve verification.
  - **Anti-Money Laundering (31 U.S.C. § 5313):** Flags abnormal income-to-debt velocity anomalies.
- **Tamper-Proof Audit Trail:** Every underwriting decision receives an immutable cryptographic fingerprint chained sequentially to previous records, guaranteeing non-repudiation during regulatory audits.
- **Enterprise Security Hardened:**
  - **DDoS Mitigation:** High-throughput sliding-window token bucket rate limiter (100 req/sec per IP).
  - **SQL Injection Defense:** Real-time payload inspection rejecting malicious database injection vectors.
  - **Brute-Force Protection:** Automated IP lockout with exponential backoff on repeated abusive attempts.

---

## Quickstart Guide

### Prerequisites
- **Java 21 LTS** or higher
- **Apache Maven 3.9+**

### 1. Build the Platform
```bash
git clone https://github.com/UGilfoyle/verilogic-engine.git
cd verilogic-engine
mvn clean install -DskipTests
```

### 2. Launch the Application
```bash
java -jar verilogic-ui/target/verilogic-ui-1.0.0-SNAPSHOT.jar
```

Once started, open your browser and navigate to:
```
http://localhost:8080
```

---

## REST API Reference

VeriLogic exposes clean, hardened REST endpoints for core banking and loan origination system (LOS) integrations.

### 1. Underwrite Bank Statement
Submits a verified financial statement payload for automated underwriting.

- **Endpoint:** `POST /api/v1/underwrite/clearledger`
- **Headers:** `Content-Type: application/json`

#### Request Payload
```json
{
  "bankName": "HDFC Bank",
  "accountNumber": "9018420911",
  "accountHolder": "Rajesh Sharma",
  "totalCredits": 80400.0,
  "totalDebits": 16800.0,
  "loanAmountRequested": 300000.0,
  "hasGuarantor": true,
  "monthsOfHistory": 6,
  "fraud": {
    "overallRiskScore": 8,
    "riskLevel": "LOW",
    "documentAuthenticity": 99.8,
    "isTamperedPDF": false,
    "averageBankBalance": 54200.0,
    "salaryDetected": true,
    "salaryAmount": 13400.0,
    "inwardBouncesCount": 0
  }
}
```

#### Response (200 OK)
```json
{
  "certificateId": "ddc81d9d-05fa-422b-b46d-6b7437746e2a",
  "caseId": "CL-6c22b4b8",
  "status": "CERTIFIED",
  "canonicalInputHash": "ce3348d7c357389964e2c1aaf8f46885a30309e415a7eeb772d156607112e336",
  "merkleRootHash": "1eba820d649ac09324153f56c82bb4de292a280814bf1835c3dc48c59fd8ca64",
  "issuedAt": "2026-09-03T20:46:37.386590Z",
  "issuer": "VeriLogic-Engine/v1.0"
}
```

---

### 2. Underwrite Loan Application Text
Submits raw loan memo text or applicant details.

- **Endpoint:** `POST /api/v1/cases/evaluate`
- **Headers:** `Content-Type: application/json`

```json
{
  "rawUnstructuredText": "Applicant: Morgan Vance, Credit Score: 745 FICO, Monthly Income: $12,500, Monthly Debt: $2,800, Requested Loan: $280,000, Guarantor: Yes",
  "submitterId": "loan-officer-102",
  "dryRun": false
}
```

---

## Security Specifications

| Layer | Policy | Action |
|---|---|---|
| **DDoS Rate Limiting** | 100 requests/sec per client IP (Burst: 150) | Returns `HTTP 429 Too Many Requests` with `Retry-After` header. |
| **SQL Injection Guard** | Scans all incoming parameters and JSON payloads | Rejects union, stacked, and blind injection vectors (`HTTP 400 Bad Request`). |
| **Abuse Lockout** | 5 flagged attempts within 60 seconds | Jails IP for 15 minutes with exponential backoff on repeat offenses (`HTTP 403 Forbidden`). |
| **Prompt Sanitization** | Neutralizes prompt overrides and strips hidden Unicode tokens | Prevents conversational model manipulation. |

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
