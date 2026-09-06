# Contributing to VeriLogic

Thanks for helping improve the engine. This repo is a multi-module Maven project with a framework-free domain core.

## Development setup

1. Install **Java 21 LTS** and **Maven 3.9+** (or use the included `./mvnw`).
2. Clone the repository and run the test suite:

```bash
./mvnw -B test
```

3. Start the workbench:

```bash
./mvnw -pl verilogic-ui -am spring-boot:run
```

The UI is at [http://localhost:8080](http://localhost:8080). Valkey and Ollama are optional; the engine falls back to in-memory locks and regex extraction when they are not running.

## Project layout

| Module | Responsibility |
|---|---|
| `verilogic-domain` | Pure Java rules, models, MC/DC tables. No Spring. |
| `verilogic-application` | Use cases, ports, pipeline orchestration. |
| `verilogic-infrastructure` | Adapters for Valkey, Ollama, and the in-memory ledger. |
| `verilogic-ui` | Vaadin workbench and REST API. |

Domain classes must stay free of Spring, Jakarta, Vaadin, and Redis imports. `HexagonalArchitectureArchTest` enforces this.

## Pull requests

- Keep changes focused and covered by tests.
- Prefer extending an existing rule or adapter over adding a new framework.
- Update the README if you change an API contract.
- Do not commit secrets, local `target/` output, or IDE files.

## Reporting security issues

Do not open a public issue for vulnerabilities. See [SECURITY.md](SECURITY.md).
