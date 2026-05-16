# Project Status

Checked on 2026-05-16 on branch `44-issue-81---readiness-fixes`.

## Overall State

The repository contains the six required applications plus Docker-based infrastructure:

- `energy-producer`
- `energy-user`
- `usage-service`
- `percentage-service`
- `rest-api`
- `energy-gui`
- PostgreSQL and RabbitMQ via `docker-compose.yml`

The current implementation follows the grading-critical architecture:

```text
Producer/User -> RabbitMQ -> Usage Service -> PostgreSQL
Usage Service -> RabbitMQ -> Percentage Service -> PostgreSQL
REST API -> read-only PostgreSQL access
JavaFX GUI -> REST API only
```

## Current Readiness

| Area | Status | Evidence |
|---|---|---|
| Independent components | Implemented | Six separate modules with own `pom.xml` and start commands. |
| Spring Boot REST API | Implemented | `GET /energy/current`, `GET /energy/historical`. |
| JavaFX GUI | Implemented | REST-only GUI, no DB dependency. |
| RabbitMQ flow | Implemented | Direct durable queues documented in `docs/message-contract.md`. |
| PostgreSQL/Flyway/JPA | Implemented | Flyway migrations and JPA schema validation in DB-backed modules. |
| Usage calculation | Implemented and hardened | Invalid messages are rejected before DB writes or update events. |
| Percentage calculation | Implemented and rounded | Persisted values are rounded to two decimals. |
| Documentation | Updated | README, module docs, runbook, message contracts, DB schema, final checklist. |

## Lecturer Risk Coverage

| Lecturer Risk | Current Status |
|---|---|
| Six separate applications | Covered by module structure and README component table. |
| Independent startup as 0-point risk | Covered by README start commands and `docs/final-readiness-check.md`. |
| Own commits per person | README documents `git shortlog -sn`; real-name mapping remains a final manual check. |
| Correct Usage/Percentage calculations | Covered by focused unit tests and hardened validation. |
| Hourly aggregation | Implemented and tested. |
| Weather-dependent Producer | Implemented with Open-Meteo and fallback. |
| Time-of-day-dependent User | Implemented and tested. |
| Usage Service central flow | Consumes producer/user messages, writes DB, publishes update. |
| Percentage after update message | Consumes `percentage_update_queue`; no direct service call. |
| No Grid message | Grid usage is calculated in Usage Service. |
| Message order matters | User-before-producer behavior is tested and documented. |
| REST + UI required | Implemented and tested/buildable. |
| Presentation explainability | README and final checklist include explanation prompts. |

## Known Limitations

- Java 25 and Spring Boot 4.0.3 are used and documented as prerequisites.
- RabbitMQ uses direct durable queues instead of an explicit exchange/routing-key topology; this is intentional for one consumer per stream.
- `hourly_usage` is the implementation name for the conceptual `energy_usage_hourly` table.
- JavaFX historical values are displayed as aggregate labels for the selected range, not as a per-hour table.
- Technical audit timestamp columns such as `updated_at` and `calculated_at` are omitted.
- Generated `target/` artifacts are ignored; verify with `git ls-files | Select-String "target"` before submission.

## Final Manual Checks Before Submission

Use `docs/smoke-test.md` as the authoritative manual runbook for the final distributed smoke test.

1. Run all module builds from README.
2. Start Docker and all six applications from a clean process list using the smoke-test runbook.
3. Verify RabbitMQ queues drain during normal operation.
4. Verify rows in `hourly_usage` and `current_percentage`.
5. Call both REST endpoints.
6. Open JavaFX GUI and verify current/historical display.
7. Run `git shortlog -sn` and make sure every team member is represented.
8. Remove generated build artifacts from Git tracking if still tracked.
