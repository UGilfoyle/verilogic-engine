# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| `1.0.0-SNAPSHOT` (main) | Yes |

## Reporting a vulnerability

Please report security issues privately through [GitHub Security Advisories](https://github.com/UGilfoyle/verilogic-engine/security/advisories/new).

Include:

- A description of the issue and its impact
- Steps to reproduce, or a proof of concept against a local instance you own
- Affected module or endpoint if known

You should receive an acknowledgment within a few days. Please do not open a public issue or pull request that discloses an exploitable vulnerability.

## Hardening notes

- `X-Forwarded-For` is ignored unless `TRUST_FORWARDED_HEADERS=true` (enable only behind a trusted reverse proxy).
- CORS origins are configured with `CORS_ALLOWED_ORIGINS`; the default is `http://localhost:8080`.
- SQL-injection and prompt-injection scanners are defense-in-depth. They are not a substitute for parameterized queries or a real WAF.
- The default audit ledger is in-memory. Do not treat a single-process demo deployment as a production system of record.
