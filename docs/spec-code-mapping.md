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
| System can be built and run with no errors | Verified locally | DB-backed modules pass Flyway-backed automated tests. A backend/message/database/REST smoke test was executed with fresh Docker infrastructure; repeat the full manual GUI runbook before hand-in. |
| Spring Boot used for REST API | Met | `rest-api` is Spring Boot Web MVC. |
| JavaFX used for GUI | Met | `energy-gui` is JavaFX. |
| RabbitMQ used between services | Met | Producer/user publish to RabbitMQ; usage consumes; usage publishes update event; percentage consumes update event. |
| GitHub repository link in submission | Not code-verifiable | Submission process criterion. |

Final grading components:

| Component | Grading Requirement | Current Status | Risk |
|---|---|---|---|
| JavaFX UI, 10% | Current pool/grid percent, refresh, date range, show data, historical produced/used/grid kWh | Implemented | Low. 22 automated tests cover HTTP client, date parsing, and value formatting. Historical values are currently shown as aggregate labels, not a per-hour table. |
| REST API, 10% | Reads DB instead of static sample data | Implemented | Low. Contract tests cover all required endpoints, both date formats, and error cases. |
| Energy Producer, 10% | Random 1-5 sec interval, plausible kWh, weather data used | Implemented | Low. Open-Meteo adapter, fallback, calculator, scheduler gating, contract tests, and smoke-test runbook exist. |
| Energy User, 10% | Random 1-5 sec interval, plausible kWh, time of day used | Implemented | Low. Usage calculator, fixed-clock tests, scheduler gating, contract tests, and smoke-test runbook exist. |
| Usage Service, 30% | Receives production/usage messages, updates usage table correctly, sends update message | Implemented | Low. Focused unit tests cover spec example, producer/user aggregation, grid fallback, hour bucketing, user-before-producer ordering, and invalid-message rejection. |
| Current Percentage Service, 30% | Receives update message and updates percentage table correctly | Implemented | Low. Focused unit tests cover spec example, zero production/usage, full depletion, existing row updates, null handling, and two-decimal rounding. Table semantics documented. |

## Lecturer Risk Checklist

This section maps the lecturer's grading-risk comments to the current implementation.

| Lecturer Risk | Current Project Evidence | Status |
|---|---|---|
| Six components must be separate projects | `energy-producer`, `energy-user`, `usage-service`, `percentage-service`, `rest-api`, and `energy-gui` each have their own `pom.xml` and entry point. | Met |
| Independent startup is a possible 0-point risk | README lists one start command per component and the required startup order. | Met |
| Each team member needs own commits | Verified via `git shortlog -sn` (documented in `docs/final-regression-checklist.md` and `docs/final-readiness-check.md`); members are named in the submission text. | Manual check |
| Usage/Percentage calculations are major grading risk | Focused tests cover hourly bucketing, grid fallback, invariant, user-before-producer ordering, division by zero, and percentage rounding. | Met |
| Messages are aggregated hourly | Usage Service truncates minute/second/nano to the hour; tests cover `14:34 -> 14:00`. | Met |
| Producer uses weather; User uses time of day | Producer has Open-Meteo plus fallback weather; User has time-of-day calculator and tests. | Met |
| Usage Service is central integration component | Usage consumes producer/user messages, writes `hourly_usage`, and publishes update messages. | Met |
| Percentage reacts after Usage update | Percentage consumes `percentage_update_queue`; it does not consume producer/user messages or poll as the main trigger. | Met |
| No Grid message | Grid usage is calculated from the uncovered user demand in Usage Service. | Met |
| Message order affects values | Documented and tested: user-before-producer first assigns usage to grid, later producer increases produced for the same hour without retroactive rebalance. | Met |
| REST and UI are mandatory | Spring Boot REST API and JavaFX GUI exist and build independently. | Met |
| Two REST views are required | `GET /energy/current` and `GET /energy/historical?start=...&end=...` are implemented and tested. | Met |
| Final presentation questions matter | README and `docs/final-readiness-check.md` include explanation prompts for RabbitMQ, table ownership, sync/async communication, and distributed-system flow. | Met |

## Implementation Matrix

| Aspect | Specification / Lecture Expectation | Current Code | Status | Gap / Next Step |
|---|---|---|---|---|
| Infrastructure | PostgreSQL and RabbitMQ via Docker | `docker-compose.yml` starts PostgreSQL `energy_community` and RabbitMQ management | Implemented | Full manual smoke-test instructions are documented in `docs/smoke-test.md`. |
| Independent components | Six applications start independently | Six separate Maven modules/apps exist; `shared` removed from active dependencies | Implemented | Avoid reintroducing shared compile-time DTO modules. |
| Message contract | JSON messages documented as contracts, not shared Java dependency | `docs/message-contract.md` documents `EnergyMessage` and `HourlyUsageUpdatedMessage`; local DTOs per service; service-local contract tests cover required fields | Implemented | Keep contract tests updated when message fields change. |
| RabbitMQ topology | Producer/user messages to usage; usage update message to percentage | `energy_queue` for producer/user, `percentage_update_queue` for derived update events | Implemented | Exchange/per-service queues only needed if more consumers must observe the same stream. |
| Energy Producer weather | Produced kWh must be weather-dependent | `WeatherClient`, `OpenMeteoWeatherClient`, `FallbackWeatherClient`, `ResilientWeatherClient`, and `WeatherProductionCalculator` are used by `EnergyProducerService` | Implemented | Use fallback mode for deterministic demos if Open-Meteo is unavailable. |
| Energy Producer message | Sends `PRODUCER`, `association`, `kwh`, `datetime` every few seconds | `EnergyProducerService#createProductionMessage` creates contract-compliant messages; `EnergyProducerScheduler` preserves random 1-5 second delay and can be disabled in tests | Implemented | Covered by unit, contract, and smoke-test documentation. |
| Energy User | Time-of-day-dependent usage message every few seconds | `EnergyUsageCalculator` models peak/off-peak/night usage; `EnergyUserService#createUsageMessage` creates contract-compliant messages with fixed-clock testability | Implemented | Covered by unit, contract, and smoke-test documentation. |
| Usage Service calculation | Aggregate by hour; community pool first; overflow to grid | `HourlyUsageUpdateService` buckets to hour and updates `communityProduced`, `communityUsed`, `gridUsed`; invalid messages are rejected before DB writes or update events | Implemented | Keep validation tests in place when changing message contracts. |
| Usage Service persistence | Writes usage table in PostgreSQL | JPA entity/repository for `hourly_usage`; Flyway `V1__create_energy_tables.sql`; Hibernate `ddl-auto=validate` | Implemented | Fresh Docker PostgreSQL smoke evidence is recorded; repeat `docs/smoke-test.md` before hand-in. |
| Percentage Service calculation | Calculate current community depleted and grid portion | `CurrentPercentageCalculationService` computes `communityUsed / produced * 100` and `gridUsed / totalUsed * 100`, then rounds persisted values to two decimals | Implemented | Keep formula and rounding tests in place. |
| Percentage persistence | Writes current percentage table | JPA entity/repository for `current_percentage`; stale rows are removed when the current hour is recalculated | Implemented | Only current-hour percentage data remains. |
| REST current endpoint | `GET /energy/current` returns current hour percentage | Reads the exact current-hour `current_percentage` row from DB; fallback returns zeros | Implemented | Controller mock tests + H2 contract tests cover all cases. |
| REST historical endpoint | `GET /energy/historical?start=...&end=...` returns usage for period | Reads `hourly_usage` by date range; supports ISO and `dd.MM.yyyy HH:mm`; invalid range returns 400 | Implemented | H2 contract tests cover ISO format, German format, out-of-range exclusion, 400 on invalid date, and 400 on start-after-end. |
| JavaFX GUI | REST-only dashboard with current and historical display | Split into `app`, `controller`, `client`, `dto`, `service`; uses async HTTP and `Platform.runLater` | Implemented | Date input validated before API call; invalid format shows error in UI. |
| GUI clean code | View/controller/client/DTO/formatting separated | `MainApp` is only entry point; HTTP/Jackson are in `EnergyApiClient` | Implemented | Keep UI class from accumulating business or HTTP logic. |
| Tests | Context, unit, integration, contract tests from lecture testing material | Producer weather/message/contract tests, user usage/message/contract tests, Usage/Percentage calculation and contract tests, Flyway-backed repository tests, REST contract tests, GUI client/validation/formatter tests | Implemented | 103 tests across all modules, 0 failures. |
| Build artifacts | No generated files committed | `.gitignore` ignores `target/`; tracked GUI target artifacts were removed from Git index | Implemented | Verify again with `git ls-files | Select-String "target"` before submission. |

## Automated Project Test Run

Executed without starting RabbitMQ or PostgreSQL:

| Module | Command | Tests | Notes |
|---|---|---|---|
| `energy-producer` | `.\mvnw.cmd test` | 8 | Weather adapter, fallback, bounded random production variation, message contract |
| `energy-user` | `.\mvnw.cmd test` | 5 | Usage calculator, fixed-clock message creation, message contract |
| `usage-service` | `.\mvnw.cmd test` | 27 | Schema migration + calculation/validation tests + message contract |
| `percentage-service` | `.\mvnw.cmd test` | 16 | Schema migration + percentage calculation tests + message contract |
| `rest-api` | `.\mvnw.cmd test` | 25 | Schema migration + controller mock tests + H2 contract tests + latest-row behavior |
| `energy-gui` | `.\mvnw -f .\energy-gui\pom.xml test` | 22 | API client (HTTP server), date validation, value formatter |

**Total: 103 tests, 0 failures** across all modules without any running infrastructure.

No RabbitMQ `Connection refused` stack trace occurred in producer/user tests after disabling scheduled publishers in test configuration.

## Flyway Migration Details

The DB-backed modules now use the lecture-style migration pattern:

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

The producer now uses this flow:

```text
EnergyProducerService
  -> WeatherClient
  -> ResilientWeatherClient
  -> OpenMeteoWeatherClient or FallbackWeatherClient
  -> WeatherProductionCalculator
  -> EnergyMessage
  -> RabbitMQ energy_queue
```

Configuration:

```properties
app.production.min-kwh=0.001
app.production.max-kwh=0.006
app.weather.mode=open-meteo
app.weather.latitude=48.2082
app.weather.longitude=16.3738
app.weather.open-meteo.base-url=https://api.open-meteo.com/v1/forecast
```

Test mode uses:

```properties
app.weather.mode=fallback
```

Automated verification:

```powershell
cd energy-producer
.\mvnw.cmd test
```

Latest result: 6 tests, 0 failures, 0 errors.

## Deterministic Scheduling And Simulation Details

Producer and user scheduled publishing now follow the same pattern:

```text
Scheduler
  -> SimulationDelayProvider
  -> Service#create...Message
  -> RabbitTemplate.convertAndSend
```

Runtime properties:

```properties
app.scheduling.enabled=true
app.scheduling.fixed-delay-ms=1000
```

Test properties:

```properties
app.scheduling.enabled=false
```

`energy-user` now has deterministic collaborators:

- `EnergyUsageCalculator` for time-of-day multipliers
- `UsageVariationProvider` for random kWh variation
- injected `Clock` for fixed timestamp tests

Automated verification:

```powershell
cd energy-user
.\mvnw.cmd test
```

Latest result: 4 tests, 0 failures, 0 errors.

## current_percentage Table Semantics (Issue 6 Decision)

**Chosen behavior: only the current-hour row is retained and returned.**

### Rationale

The `current_percentage` table uses `hour` (a `LocalDateTime` truncated to the full hour) as its primary key. The `percentage-service` ignores delayed updates for older hours and removes stale percentage rows when it recalculates the current hour. `GET /energy/current` reads the exact current-hour row and returns zeros if no such row exists.

This keeps the required current-hour semantics without a schema change or special singleton row.

### Invariants enforced by design

| Rule | How it holds |
|---|---|
| Each hour has at most one row | `hour` is the primary key — duplicate saves are upserts |
| `GET /current` never returns stale data | Repository lookup uses the exact current-hour PK |
| No ambiguous data is created | Same hour → same PK → same row is overwritten |

### Tests

- `CurrentPercentageLatestRowTest` (H2): proves repository lookup selects the requested current-hour row and that saving the same hour twice results in exactly one row (upsert).
- `EnergyControllerTest` (MockMvc): proves `GET /energy/current` returns the current-hour row and falls back to zeros when no current-hour data exists.

## Remaining Gaps

1. **Full distributed smoke test** — Backend/message/database/REST smoke was completed successfully and recorded in `docs/final-readiness-check.md`; the repeatable manual GUI-inclusive runbook is `docs/smoke-test.md`. Recommended: repeat once more immediately before hand-in.

2. **Build artifact cleanup** — Check that no `target/` directories are tracked in Git before submission.

## Lecture Concepts Applied

- Distributed systems: independent components cooperate toward one visible system.
- SOA/microservices: cohesive components with minimal explicit interfaces.
- REST: client-server separation, stable URLs, JSON DTOs, HTTP error semantics.
- JavaFX: MVC/MVVM-inspired separation of entry point, controller, client, DTOs, and formatting.
- Persistence: PostgreSQL, JPA entities/repositories, and Flyway migrations for versioned schema.
- Messaging: RabbitMQ broker decouples producers and consumers; `RabbitTemplate` and `@RabbitListener` follow lecture code.
- Testing: context tests, calculation unit tests, endpoint tests, persistence integration tests, and manual end-to-end smoke checks all have distinct roles.

## Recommended Implementation Order

1. ✅ Add Flyway migrations for project database schema
2. ✅ Make producer and user scheduling/test behavior deterministic
3. ✅ Add weather-based production adapter for energy-producer
4. ✅ Add focused tests for usage aggregation and grid fallback (usage-service, 18 tests)
5. ✅ Add focused tests for current percentage calculation (percentage-service, 12 tests)
6. ✅ Clarify and enforce current_percentage table semantics
7. ✅ Add REST API contract tests with MockMvc and H2 fixture rows (rest-api, 25 tests)
8. ✅ Live end-to-end smoke test with Docker — documented in `docs/final-readiness-check.md`; repeatable manual runbook added in `docs/smoke-test.md`
9. ✅ Add GUI client tests and improve date range input robustness (energy-gui, 22 tests)
10. ✅ Add service-local RabbitMQ JSON contract tests for producer/user/usage/percentage modules
11. ✅ Add per-module technical documentation with Mermaid diagrams in `docs/`
