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
| Every component can be started independently | Mostly met | Six independent Maven/Spring/JavaFX modules exist. Full runtime smoke with Docker still needs to be repeated before hand-in. |
| System can be built and run with no errors | Partly verified | DB-backed modules pass Flyway-backed automated tests. Full Docker runtime smoke still needs to be executed. |
| Spring Boot used for REST API | Met | `rest-api` is Spring Boot Web MVC. |
| JavaFX used for GUI | Met | `energy-gui` is JavaFX. |
| RabbitMQ used between services | Met | Producer/user publish to RabbitMQ; usage consumes; usage publishes update event; percentage consumes update event. |
| GitHub repository link in submission | Not code-verifiable | Submission process criterion. |

Final grading components:

| Component | Grading Requirement | Current Status | Risk |
|---|---|---|---|
| JavaFX UI, 10% | Current pool/grid percent, refresh, date range, show data, historical produced/used/grid kWh | Implemented | Low. 21 automated tests cover HTTP client, date parsing, and value formatting. |
| REST API, 10% | Reads DB instead of static sample data | Implemented | Low. Contract tests cover all required endpoints, both date formats, and error cases. |
| Energy Producer, 10% | Random 1-5 sec interval, plausible kWh, weather data used | Implemented with smoke-test gap | Low. Open-Meteo adapter, fallback, calculator, scheduler gating, and tests exist. Manual RabbitMQ smoke still needed. |
| Energy User, 10% | Random 1-5 sec interval, plausible kWh, time of day used | Implemented with smoke-test gap | Low. Usage calculator, fixed-clock tests, scheduler gating, and message creation tests exist. Manual RabbitMQ smoke still needed. |
| Usage Service, 30% | Receives production/usage messages, updates usage table correctly, sends update message | Implemented | Low. 16 focused unit tests cover spec example, producer/user aggregation, grid fallback, hour bucketing, and null handling. |
| Current Percentage Service, 30% | Receives update message and updates percentage table correctly | Implemented | Low. 10 focused unit tests cover spec example, zero production/usage, full depletion, existing row updates, and null handling. Table semantics documented. |

## Implementation Matrix

| Aspect | Specification / Lecture Expectation | Current Code | Status | Gap / Next Step |
|---|---|---|---|---|
| Infrastructure | PostgreSQL and RabbitMQ via Docker | `docker-compose.yml` starts PostgreSQL `energy_community` and RabbitMQ management | Implemented | Add smoke-test instructions for verifying queues, rows, REST JSON, GUI labels. |
| Independent components | Six applications start independently | Six separate Maven modules/apps exist; `shared` removed from active dependencies | Implemented | Avoid reintroducing shared compile-time DTO modules. |
| Message contract | JSON messages documented as contracts, not shared Java dependency | `docs/message-contract.md` documents `EnergyMessage` and `HourlyUsageUpdatedMessage`; local DTOs per service | Implemented | Add serialization tests for required fields and timestamp format. |
| RabbitMQ topology | Producer/user messages to usage; usage update message to percentage | `energy_queue` for producer/user, `percentage_update_queue` for derived update events | Implemented | Exchange/per-service queues only needed if more consumers must observe the same stream. |
| Energy Producer weather | Produced kWh must be weather-dependent | `WeatherClient`, `OpenMeteoWeatherClient`, `FallbackWeatherClient`, `ResilientWeatherClient`, and `WeatherProductionCalculator` are used by `EnergyProducerService` | Implemented | Manual live smoke with RabbitMQ and optional Open-Meteo availability check. |
| Energy Producer message | Sends `PRODUCER`, `association`, `kwh`, `datetime` every few seconds | `EnergyProducerService#createProductionMessage` creates contract-compliant messages; `EnergyProducerScheduler` preserves random 1-5 second delay and can be disabled in tests | Implemented | Manual RabbitMQ smoke still needed. |
| Energy User | Time-of-day-dependent usage message every few seconds | `EnergyUsageCalculator` models peak/off-peak/night usage; `EnergyUserService#createUsageMessage` creates contract-compliant messages with fixed-clock testability | Implemented | Manual RabbitMQ smoke still needed. |
| Usage Service calculation | Aggregate by hour; community pool first; overflow to grid | `HourlyUsageUpdateService` buckets to hour and updates `communityProduced`, `communityUsed`, `gridUsed` | Implemented | Add unit tests for spec example and edge cases. |
| Usage Service persistence | Writes usage table in PostgreSQL | JPA entity/repository for `hourly_usage`; Flyway `V1__create_energy_tables.sql`; Hibernate `ddl-auto=validate` | Implemented | Fresh Docker PostgreSQL smoke still needed. |
| Percentage Service calculation | Calculate current community depleted and grid portion | `CurrentPercentageCalculationService` computes `communityUsed / produced * 100` and `gridUsed / totalUsed * 100` | Implemented | Add tests for zero production, zero usage, full depletion, grid usage. |
| Percentage persistence | Writes current percentage table | JPA entity/repository for `current_percentage`; Flyway `V1__create_energy_tables.sql`; Hibernate `ddl-auto=validate` | Implemented | Decide if table should keep only latest row or latest-per-hour history. |
| REST current endpoint | `GET /energy/current` returns current hour percentage | Reads latest `current_percentage` row from DB; fallback returns zeros | Implemented | Controller mock tests + H2 contract tests cover all cases. Semantics documented in `current_percentage Table Semantics` section. |
| REST historical endpoint | `GET /energy/historical?start=...&end=...` returns usage for period | Reads `hourly_usage` by date range; supports ISO and `dd.MM.yyyy HH:mm`; invalid range returns 400 | Implemented | H2 contract tests cover ISO format, German format, out-of-range exclusion, 400 on invalid date, and 400 on start-after-end. |
| JavaFX GUI | REST-only dashboard with current and historical display | Split into `app`, `controller`, `client`, `dto`, `service`; uses async HTTP and `Platform.runLater` | Implemented | Date input validated before API call; invalid format shows error in UI. |
| GUI clean code | View/controller/client/DTO/formatting separated | `MainApp` is only entry point; HTTP/Jackson are in `EnergyApiClient` | Implemented | Keep UI class from accumulating business or HTTP logic. |
| Tests | Context, unit, integration, contract tests from lecture testing material | Producer weather/message tests, user usage/message tests, Flyway-backed repository tests, REST contract tests, GUI client/validation/formatter tests | Implemented | 86 tests across all modules, 0 failures. |
| Build artifacts | No generated files committed | `.gitignore` ignores `target/`, but some `target` files may already be tracked | Partial | Remove tracked build artifacts from Git index in a separate cleanup commit. |

## Automated Project Test Run

Executed without starting RabbitMQ or PostgreSQL:

| Module | Command | Tests | Notes |
|---|---|---|---|
| `energy-producer` | `.\mvnw.cmd test` | 6 | Weather adapter, fallback, production calculator |
| `energy-user` | `.\mvnw.cmd test` | 4 | Usage calculator, fixed-clock message creation |
| `usage-service` | `.\mvnw.cmd test` | 18 | Schema migration + 16 focused unit tests for aggregation and grid fallback |
| `percentage-service` | `.\mvnw.cmd test` | 12 | Schema migration + 10 focused unit tests for percentage calculation |
| `rest-api` | `.\mvnw.cmd test` | 25 | Schema migration + controller mock tests + H2 contract tests + latest-row behavior |
| `energy-gui` | `.\mvnw -f .\energy-gui\pom.xml test` | 21 | API client (HTTP server), date validation, value formatter |

**Total: 86 tests, 0 failures** across all modules without any running infrastructure.

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
app.production.min-kwh=10.0
app.production.max-kwh=30.0
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

**Chosen behavior: Option A — one row per hour, REST returns the row with the highest hour.**

### Rationale

The `current_percentage` table uses `hour` (a `LocalDateTime` truncated to the full hour) as its primary key. The `percentage-service` writes or overwrites one row per hour whenever a usage update message arrives. `GET /energy/current` calls `findFirstByOrderByHourDesc()`, which returns the row with the highest hour value — always the most recently updated hour.

This is consistent with the `hourly_usage` table (same primary key pattern) and keeps the history for debugging without requiring a schema change or special singleton logic.

### Invariants enforced by design

| Rule | How it holds |
|---|---|
| Each hour has at most one row | `hour` is the primary key — duplicate saves are upserts |
| `GET /current` always returns the latest data | `findFirstByOrderByHourDesc()` orders by PK descending |
| No ambiguous data is created | Same hour → same PK → same row is overwritten |

### Tests

- `CurrentPercentageLatestRowTest` (H2): proves `findFirstByOrderByHourDesc()` returns the highest-hour row when multiple rows exist, that a single row is returned correctly, and that saving the same hour twice results in exactly one row (upsert).
- `EnergyControllerTest` (MockMvc): proves `GET /energy/current` returns the latest row and falls back to zeros when no data exists.

## Remaining Gaps

1. **Full distributed smoke test** — A live end-to-end run was completed successfully (see `docs/how-to-run.md`). GUI showed correct real-time values. Recommended: repeat once more immediately before hand-in.

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
8. ✅ Live end-to-end smoke test with Docker — document in `docs/how-to-run.md`
9. ✅ Add GUI client tests and improve date range input robustness (energy-gui, 21 tests)
