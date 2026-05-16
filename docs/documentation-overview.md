# Documentation Overview

This folder contains the technical and QA documentation for the Energy Community distributed system.

## System Architecture

```mermaid
flowchart LR
    Producer["energy-producer<br/>Spring Boot"]
    User["energy-user<br/>Spring Boot"]
    EnergyQueue["RabbitMQ<br/>energy_queue"]
    Usage["usage-service<br/>Spring Boot + JPA"]
    UsageTable["PostgreSQL<br/>hourly_usage"]
    UpdateQueue["RabbitMQ<br/>percentage_update_queue"]
    Percentage["percentage-service<br/>Spring Boot + JPA"]
    PercentageTable["PostgreSQL<br/>current_percentage"]
    Api["rest-api<br/>Spring Web MVC"]
    Gui["energy-gui<br/>JavaFX"]

    Producer --> EnergyQueue
    User --> EnergyQueue
    EnergyQueue --> Usage
    Usage --> UsageTable
    Usage --> UpdateQueue
    UpdateQueue --> Percentage
    Percentage --> UsageTable
    Percentage --> PercentageTable
    Api --> UsageTable
    Api --> PercentageTable
    Gui --> Api
```

## Module Documentation

| Module | Documentation | Runtime Responsibility |
|---|---|---|
| `energy-producer` | `docs/energy-producer.md` | Publishes weather-dependent `PRODUCER` messages to RabbitMQ. |
| `energy-user` | `docs/energy-user.md` | Publishes time-of-day-dependent `USER` messages to RabbitMQ. |
| `usage-service` | `docs/usage-service.md` | Consumes energy messages, updates hourly usage, publishes usage updates. |
| `percentage-service` | `docs/percentage-service.md` | Consumes usage updates, calculates current percentages, writes percentage data. |
| `rest-api` | `docs/rest-api.md` | Provides read-only DB-backed REST endpoints for GUI. |
| `energy-gui` | `docs/energy-gui.md` | JavaFX client that calls only the REST API. |

## Cross-Cutting Documentation

| Document | Purpose |
|---|---|
| `docs/message-contract.md` | RabbitMQ topology, JSON payloads, contract tests. |
| `docs/database-schema.md` | PostgreSQL table mapping and service read/write responsibilities. |
| `docs/how-to-run.md` | Detailed startup and troubleshooting instructions. |
| `docs/smoke-test.md` | Repeatable distributed smoke-test runbook. |
| `docs/final-readiness-check.md` | Final pre-submission checklist and last smoke evidence. |
| `docs/spec-code-mapping.md` | Mapping between specification/grading risks and implementation. |
| `docs/project-status.md` | Current readiness state and remaining manual checks. |
| `docs/project-audit-report.md` | Historical audit baseline. Some findings may have been resolved after the report. |

## QA Reading Path

For a reviewer or team member preparing for the final presentation:

1. Read `README.MD` for the high-level system and start commands.
2. Read this overview.
3. Read `docs/message-contract.md` and `docs/database-schema.md`.
4. Read the module document for the component you need to explain.
5. Execute `docs/smoke-test.md`.
6. Use `docs/final-readiness-check.md` as the final sign-off checklist.

