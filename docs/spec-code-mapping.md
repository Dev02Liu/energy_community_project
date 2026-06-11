# Project Specification To Code Mapping

This mapping reflects the repository state checked on 2026-05-15.

## Specification Summary

The project is an Energy Community distributed system. It consists of six independently startable applications plus shared infrastructure:

- `energy-producer`: publishes community production messages every few seconds. kWh must be plausible and weather-dependent.
- `energy-user`: publishes community usage messages every few seconds. kWh must be plausible and time-of-day-dependent.
- `usage-service`: consumes producer/user messages, aggregates events into hourly usage rows, applies community pool first, and writes `hourly_usage`.
- `percentage-service`: consumes update messages after usage changes, calculates current percentage values, and writes `current_percentage`.
- `rest-api`: Spring Boot REST API with `GET /energy/current` and `GET /energy/historical?start=...&end=...`; it reads from PostgreSQL only.
- `energy-gui`: JavaFX GUI; it calls only the REST API and does not access PostgreSQL or RabbitMQ.
- Infrastructure: one PostgreSQL database and one RabbitMQ broker.

## Grading Check

Mandatory criteria from final grading:

| Criterion | Current Status | Notes |
|---|---|---|
| Every component can be started independently | Met | Six independent Maven/Spring/JavaFX modules exist. A repeatable manual smoke-test runbook is documented in `docs/smoke-test.md`. |
| System can be built and run with no errors | Verified locally | All six modules build with `clean package` (`BUILD SUCCESS`). A backend/message/database/REST smoke test was executed with fresh Docker infrastructure; repeat the full manual GUI runbook before hand-in. |
| Spring Boot used for REST API | Met | `rest-api` is Spring Boot Web MVC. |
| JavaFX used for GUI | Met | `energy-gui` is JavaFX. |
| RabbitMQ used between services | Met | Producer/user publish to RabbitMQ; usage consumes; usage publishes update event; percentage consumes update event. |
| GitHub repository link in submission | Not code-verifiable | Submission process criterion. |

Final grading components:

| Component | Grading Requirement | Current Status | Risk |
|---|---|---|---|
| JavaFX UI, 10% | Current pool/grid percent, refresh, date range, show data, historical produced/used/grid kWh | Implemented | Low. The date range is chosen with a date picker plus an hour dropdown; historical values are shown as aggregate labels, not a per-hour table. |
| REST API, 10% | Reads DB instead of static sample data | Implemented | Low. Both required endpoints read from PostgreSQL, accept ISO date input, and return 400 on invalid dates. |
| Energy Producer, 10% | Random 1-5 sec interval, plausible kWh, weather data used | Implemented | Low. Open-Meteo solar-radiation client, calculator, and scheduler exist; verified through the smoke-test runbook. |
| Energy User, 10% | Random 1-5 sec interval, plausible kWh, time of day used | Implemented | Low. Time-of-day usage calculator and scheduler exist; verified through the smoke-test runbook. |
| Usage Service, 30% | Receives production/usage messages, updates usage table correctly, sends update message | Implemented | Low. Community pool first, grid fallback, hour bucketing, and user-before-producer ordering verified end-to-end against the spec example. |
| Current Percentage Service, 30% | Receives update message and updates percentage table correctly | Implemented | Low. Computes depletion and grid portion with two-decimal rounding; table semantics documented below. |

## Lecturer Risk Checklist

This section maps the lecturer's grading-risk comments to the current implementation.

| Lecturer Risk | Current Project Evidence | Status |
|---|---|---|
| Six components must be separate projects | `energy-producer`, `energy-user`, `usage-service`, `percentage-service`, `rest-api`, and `energy-gui` each have their own `pom.xml` and entry point. | Met |
| Independent startup is a possible 0-point risk | README lists one start command per component and the required startup order. | Met |
| Each team member needs own commits | Verified via `git shortlog -sn` (documented in `docs/final-regression-checklist.md` and `docs/final-readiness-check.md`); members are named in the submission text. | Manual check |
| Usage/Percentage calculations are major grading risk | Hourly bucketing, grid fallback, the `communityUsed <= communityProduced` invariant, user-before-producer ordering, division by zero, and percentage rounding are implemented and documented. | Met |
| Messages are aggregated hourly | Usage Service truncates minute/second/nano to the hour (`14:34 -> 14:00`). | Met |
| Producer uses weather; User uses time of day | Producer reads Open-Meteo solar radiation; User has a time-of-day calculator. | Met |
| Usage Service is central integration component | Usage consumes producer/user messages, writes `hourly_usage`, and publishes update messages. | Met |
| Percentage reacts after Usage update | Percentage consumes `percentage_update_queue`; it does not consume producer/user messages or poll as the main trigger. | Met |
| No Grid message | Grid usage is calculated from the uncovered user demand in Usage Service. | Met |
| Message order affects values | Documented: user-before-producer first assigns usage to grid, later producer increases produced for the same hour without retroactive rebalance. | Met |
| REST and UI are mandatory | Spring Boot REST API and JavaFX GUI exist and build independently. | Met |
| Two REST views are required | `GET /energy/current` and `GET /energy/historical?start=...&end=...` are implemented. | Met |
| Final presentation questions matter | README and `docs/final-readiness-check.md` include explanation prompts for RabbitMQ, table ownership, sync/async communication, and distributed-system flow. | Met |

## Implementation Matrix

| Aspect | Specification / Lecture Expectation | Current Code | Status | Gap / Next Step |
|---|---|---|---|---|
| Infrastructure | PostgreSQL and RabbitMQ via Docker | `docker-compose.yml` starts PostgreSQL `energy_community` and RabbitMQ management | Implemented | Full manual smoke-test instructions are documented in `docs/smoke-test.md`. |
| Independent components | Six applications start independently | Six separate Maven modules/apps exist; `shared` removed from active dependencies | Implemented | Avoid reintroducing shared compile-time DTO modules. |
| Message contract | JSON messages documented as contracts, not shared Java dependency | `docs/message-contract.md` documents `EnergyMessage` and `HourlyUsageUpdatedMessage`; local DTOs per service | Implemented | Keep `docs/message-contract.md` updated when message fields change. |
| RabbitMQ topology | Producer/user messages to usage; usage update message to percentage | `energy_queue` for producer/user, `percentage_update_queue` for derived update events | Implemented | Exchange/per-service queues only needed if more consumers must observe the same stream. |
| Energy Producer weather | Produced kWh must be weather-dependent | `WeatherClient` (Open-Meteo solar radiation) and `WeatherProductionCalculator` are used by `EnergyProducerService` | Implemented | If Open-Meteo is unreachable, `WeatherClient` returns `0` W/m² and the producer keeps running. |
| Energy Producer message | Sends `PRODUCER`, `association`, `kwh`, `datetime` every few seconds | `EnergyProducerService#createProductionMessage` creates contract-compliant messages; `EnergyProducerScheduler` preserves the random 1-5 second delay | Implemented | Message shape documented in `docs/message-contract.md`. |
| Energy User | Time-of-day-dependent usage message every few seconds | `EnergyUsageCalculator` models peak/off-peak/night usage; `EnergyUserService#createUsageMessage` creates contract-compliant messages with the current timestamp | Implemented | Message shape documented in `docs/message-contract.md`. |
| Usage Service calculation | Aggregate by hour; community pool first; overflow to grid | `HourlyUsageUpdateService` buckets to hour and updates `communityProduced`, `communityUsed`, `gridUsed` | Implemented | Verified end-to-end against the spec example through RabbitMQ and the DB. |
| Usage Service persistence | Writes usage table in PostgreSQL | JPA entity/repository for `hourly_usage`; Flyway `V1__create_energy_tables.sql`; Hibernate `ddl-auto=validate` | Implemented | Fresh Docker PostgreSQL smoke evidence is recorded; repeat `docs/smoke-test.md` before hand-in. |
| Percentage Service calculation | Calculate current community depleted and grid portion | `CurrentPercentageCalculationService` computes `communityUsed / produced * 100` and `gridUsed / totalUsed * 100`, then rounds persisted values to two decimals | Implemented | Formula and rounding documented in `docs/database-schema.md`. |
| Percentage persistence | Writes current percentage table | Service clears `current_percentage` and saves the latest calculated row | Implemented | REST and GUI display the latest calculated percentage values. |
| REST current endpoint | `GET /energy/current` returns current percentage data | Reads the latest `current_percentage` row from DB; fallback returns zeros | Implemented | Newest row is selected via `findFirstByOrderByHourDesc()`. |
| REST historical endpoint | `GET /energy/historical?start=...&end=...` returns usage for period | Reads `hourly_usage` by date range; `start`/`end` bind to `LocalDateTime`, so an invalid date returns 400 | Implemented | A reversed range returns an empty list. |
| JavaFX GUI | REST-only dashboard with current and historical display | Split into `view` (FXML), `app`, `controller`, `client`, and `dto`; uses async HTTP and `Platform.runLater` | Implemented | Date range is selected via a `DatePicker` and an hour `ComboBox` (0–23, fixed in code, no DB coupling); errors are shown in the UI. |
| GUI clean code | View/controller/client/DTO separation | Layout lives in `energy-view.fxml` (loaded via `FXMLLoader`); HTTP/Jackson are in `EnergyApiClient`; the controller only handles `@FXML` actions and label updates, with row aggregation extracted into the pure `summarize` method | Implemented | Keep UI class from accumulating business or HTTP logic. |
| Build artifacts | No generated files committed | `.gitignore` ignores `target/`; tracked GUI target artifacts were removed from Git index | Implemented | Verify again with `git ls-files | Select-String "target"` before submission. |

## Project Build Run

Each module builds independently without starting RabbitMQ or PostgreSQL:

| Module | Command | Result |
|---|---|---|
| `energy-producer` | `.\mvnw.cmd clean package` | `BUILD SUCCESS` |
| `energy-user` | `.\mvnw.cmd clean package` | `BUILD SUCCESS` |
| `usage-service` | `.\mvnw.cmd clean package` | `BUILD SUCCESS` |
| `percentage-service` | `.\mvnw.cmd clean package` | `BUILD SUCCESS` |
| `rest-api` | `.\mvnw.cmd clean package` | `BUILD SUCCESS` |
| `energy-gui` | `.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml clean package` | `BUILD SUCCESS` |

## Flyway Migration Details

The DB-backed modules use the lecture-style migration pattern:

```text
src/main/resources/db/migration/V1__create_energy_tables.sql
```

The migration creates:

```sql
hourly_usage(hour, community_produced, community_used, grid_used)
current_percentage(hour, community_depleted, grid_portion)
```

The implementation table `hourly_usage` is conceptually equivalent to the project mapping name `energy_usage_hourly`. The shorter name is used consistently across Flyway, JPA, REST, Usage Service, and Percentage Service. Detailed responsibilities are documented in `docs/database-schema.md`.

Runtime schema generation is disabled as a source of truth:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

`usage-service`, `percentage-service`, and `rest-api` each include the same V1 migration so whichever DB-backed service starts first can initialize an empty PostgreSQL database. Services started after that validate the same Flyway schema history.

## Weather Adapter Details

The producer uses this flow:

```text
EnergyProducerService
  -> WeatherClient (current solar radiation from Open-Meteo)
  -> WeatherProductionCalculator
  -> EnergyMessage
  -> RabbitMQ energy_queue
```

`WeatherClient` returns `0` W/m² if the Open-Meteo API cannot be reached, so the producer keeps
publishing (at the minimum kWh) even offline.

Configuration:

```properties
app.production.min-kwh=0.001
app.production.max-kwh=0.006
app.weather.latitude=48.2082
app.weather.longitude=16.3738
```

## Deterministic Scheduling And Simulation Details

Producer and user scheduled publishing follow the same simple pattern:

```text
Scheduler
  -> short random sleep
  -> Service#create...Message
  -> RabbitTemplate.convertAndSend
```

Runtime properties:

```properties
app.scheduling.fixed-delay-ms=1000
```

`energy-user` has one small calculator collaborator:

- `EnergyUsageCalculator` for time-of-day multipliers

## current_percentage Table Semantics

**Chosen behavior: the table stores the latest calculated percentage row.**

### Rationale

The `percentage-service` receives an update event with an hour, reads the matching `hourly_usage` row, calculates the two percentages, clears `current_percentage`, and saves one row for that calculated hour. `GET /energy/current` reads the newest row and returns zeros if no percentage data exists.

This keeps the REST/GUI display simple: there is only one current display value to explain.

### Invariants enforced by design

| Rule | How it holds |
|---|---|
| Each hour has at most one row | `hour` is the primary key — duplicate saves are upserts |
| `GET /current` has a simple read path | Repository lookup uses `findFirstByOrderByHourDesc()` |
| No ambiguous data is created | Same hour → same PK → same row is overwritten |

## Remaining Gaps

1. **Full distributed smoke test** — Backend/message/database/REST smoke was completed successfully and recorded in `docs/final-readiness-check.md`; the repeatable manual GUI-inclusive runbook is `docs/smoke-test.md`. Recommended: repeat once more immediately before hand-in.

2. **Build artifact cleanup** — Check that no `target/` directories are tracked in Git before submission.

## Lecture Concepts Applied

- Distributed systems: independent components cooperate toward one visible system.
- SOA/microservices: cohesive components with minimal explicit interfaces.
- REST: client-server separation, stable URLs, JSON DTOs, HTTP error semantics.
- JavaFX: MVC/MVVM-inspired separation of entry point, FXML view, controller, client, and DTOs.
- Persistence: PostgreSQL, JPA entities/repositories, and Flyway migrations for versioned schema.
- Messaging: RabbitMQ broker decouples producers and consumers; `RabbitTemplate` and `@RabbitListener` follow lecture code.
- Verification: manual end-to-end smoke checks against the spec example, documented in `docs/smoke-test.md`.

## Recommended Implementation Order

1. ✅ Add Flyway migrations for project database schema
2. ✅ Make producer and user scheduling behavior deterministic
3. ✅ Add weather-based production adapter for energy-producer
4. ✅ Implement usage aggregation and grid fallback in usage-service
5. ✅ Implement current percentage calculation in percentage-service
6. ✅ Clarify and enforce current_percentage table semantics
7. ✅ Implement REST API endpoints reading from PostgreSQL
8. ✅ Live end-to-end smoke test with Docker — documented in `docs/final-readiness-check.md`; repeatable manual runbook added in `docs/smoke-test.md`
9. ✅ Implement the JavaFX GUI with an FXML view and REST-only client
10. ✅ Document the RabbitMQ JSON message contracts for producer/user/usage/percentage modules
11. ✅ Add per-module technical documentation with Mermaid diagrams in `docs/`
