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
| JavaFX UI, 10% | Current pool/grid percent, refresh, date range, show data, historical produced/used/grid kWh | Implemented | Low. Text fields are acceptable; GUI client tests would reduce risk. |
| REST API, 10% | Reads DB instead of static sample data | Implemented | Low. Add endpoint contract tests. |
| Energy Producer, 10% | Random 1-5 sec interval, plausible kWh, weather data used | Implemented with smoke-test gap | Low. Open-Meteo adapter, fallback, calculator, scheduler gating, and tests exist. Manual RabbitMQ smoke still needed. |
| Energy User, 10% | Random 1-5 sec interval, plausible kWh, time of day used | Implemented with smoke-test gap | Low. Usage calculator, fixed-clock tests, scheduler gating, and message creation tests exist. Manual RabbitMQ smoke still needed. |
| Usage Service, 30% | Receives production/usage messages, updates usage table correctly, sends update message | Implemented with test gaps | Medium. Formula matches spec example; focused calculation tests are missing. Flyway schema is implemented. |
| Current Percentage Service, 30% | Receives update message and updates percentage table correctly | Implemented with test gaps | Medium. Formula matches spec; Flyway schema is implemented; current/latest table semantics still need clarification. |

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
| REST current endpoint | `GET /energy/current` returns current hour percentage | Reads latest `current_percentage` row from DB; fallback returns zeros | Implemented | Add controller test and clarify latest/current semantics. |
| REST historical endpoint | `GET /energy/historical?start=...&end=...` returns usage for period | Reads `hourly_usage` by date range; supports ISO and `dd.MM.yyyy HH:mm`; invalid range returns 400 | Implemented | Add MockMvc tests for valid/invalid formats and inclusive boundaries. |
| JavaFX GUI | REST-only dashboard with current and historical display | Split into `app`, `controller`, `client`, `dto`, `service`; uses async HTTP and `Platform.runLater` | Implemented | Optional: replace text date fields with controls if time permits. |
| GUI clean code | View/controller/client/DTO/formatting separated | `MainApp` is only entry point; HTTP/Jackson are in `EnergyApiClient` | Implemented | Keep UI class from accumulating business or HTTP logic. |
| Tests | Context, unit, integration, contract tests from lecture testing material | Producer weather/message tests, user usage/message tests, and Flyway-backed repository tests exist | Partial | Add behavior tests across usage, percentage, REST, GUI. |
| Build artifacts | No generated files committed | `.gitignore` ignores `target/`, but some `target` files may already be tracked | Partial | Remove tracked build artifacts from Git index in a separate cleanup commit. |

## Automated Project Test Run

Executed without starting RabbitMQ or PostgreSQL:

| Module | Command | Result |
|---|---|---|
| `energy-producer` | `.\mvnw.cmd test` | Build success, 6 tests |
| `energy-user` | `.\mvnw.cmd test` | Build success, 4 tests |
| `usage-service` | `.\mvnw.cmd test` | Build success, 2 tests, Flyway-backed H2 schema |
| `percentage-service` | `.\mvnw.cmd test` | Build success, 2 tests, Flyway-backed H2 schema |
| `rest-api` | `.\mvnw.cmd test` | Build success, 2 tests, Flyway-backed H2 schema |
| `energy-gui` | `.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml test` | Build success, no tests present |

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

## Critical Gaps To Address Next

1. **Focused behavior tests**
   - The grading allows point deductions for badly written code and failure to explain the system.
   - Tests should make the formulas defensible:
     - Given produced `18.05`, community used `18.02`, grid used `1.056` and a new user message of `0.05` at `14:34`, resulting row must be community used `18.05` and grid used `1.076`.
     - Percentage must produce `100.00` community depleted and approximately `5.63` grid portion for the spec example.

2. **Full distributed smoke test**
   - Automated producer/user tests pass, but the full Docker plus six-component flow should be manually verified before hand-in.
   - Check RabbitMQ queues, PostgreSQL rows, REST JSON, and GUI labels.

3. **Current percentage semantics**
   - Current code stores rows per hour and REST returns the latest row.
   - Decide whether this is the intended model or whether `current_percentage` should contain exactly one row.

## Lecture Concepts Applied

- Distributed systems: independent components cooperate toward one visible system.
- SOA/microservices: cohesive components with minimal explicit interfaces.
- REST: client-server separation, stable URLs, JSON DTOs, HTTP error semantics.
- JavaFX: MVC/MVVM-inspired separation of entry point, controller, client, DTOs, and formatting.
- Persistence: PostgreSQL, JPA entities/repositories, and Flyway migrations for versioned schema.
- Messaging: RabbitMQ broker decouples producers and consumers; `RabbitTemplate` and `@RabbitListener` follow lecture code.
- Testing: context tests, calculation unit tests, endpoint tests, persistence integration tests, and manual end-to-end smoke checks all have distinct roles.

## Recommended Implementation Order

1. Add calculation tests for `usage-service` and `percentage-service`.
2. Add REST controller tests for current/historical endpoints and invalid dates.
3. Add GUI client tests for JSON parsing and URL encoding.
4. Add manual smoke-test runbook.
5. Remove tracked `target/` artifacts from Git index.
