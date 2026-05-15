# Project Specification To Code Mapping

This mapping is based on the project specification, grading schema, lecture transcripts, lecture code, lecture materials, and the repository state checked on 2026-05-15.

## Source Basis

- `project-resources/LEcture-Materials/Projektspezifikationen.pdf`
- `project-resources/LEcture-Materials/ProjektGrading.pdf`
- `project-resources/Lecture Transcripts.txt`
- `project-resources/Lecture-Code/disys26bwi1/disys26bwi1`
- Lecture materials for REST, JavaFX, PostgreSQL, JPA/Flyway, RabbitMQ/scaling, design, and testing.

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
| System can be built and run with no errors | Partly verified | `energy-producer` tests pass after the weather adapter change. Other modules should be retested as follow-up issues are completed. |
| Spring Boot used for REST API | Met | `rest-api` is Spring Boot Web MVC. |
| JavaFX used for GUI | Met | `energy-gui` is JavaFX. |
| RabbitMQ used between services | Met | Producer/user publish to RabbitMQ; usage consumes; usage publishes update event; percentage consumes update event. |
| GitHub repository link in submission | Not code-verifiable | Submission process criterion. |

Final grading components:

| Component | Grading Requirement | Current Status | Risk |
|---|---|---|---|
| JavaFX UI, 10% | Current pool/grid percent, refresh, date range, show data, historical produced/used/grid kWh | Implemented | Low. Text fields are acceptable; GUI client tests would reduce risk. |
| REST API, 10% | Reads DB instead of static sample data | Implemented | Low. Add endpoint contract tests. |
| Energy Producer, 10% | Random 1-5 sec interval, plausible kWh, weather data used | Implemented with smoke-test gap | Low. Open-Meteo adapter, fallback, calculator, and tests exist. Manual RabbitMQ smoke still needed. |
| Energy User, 10% | Random 1-5 sec interval, plausible kWh, time of day used | Implemented with test gap | Medium. Logic exists but deterministic fixed-clock tests are missing. |
| Usage Service, 30% | Receives production/usage messages, updates usage table correctly, sends update message | Implemented with test gaps | Medium. Formula matches spec example; Flyway migration and focused tests are missing. |
| Current Percentage Service, 30% | Receives update message and updates percentage table correctly | Implemented with test gaps | Medium. Formula matches spec; table currently stores one row per hour and REST reads latest. |

## Implementation Matrix

| Aspect | Specification / Lecture Expectation | Current Code | Status | Gap / Next Step |
|---|---|---|---|---|
| Infrastructure | PostgreSQL and RabbitMQ via Docker | `docker-compose.yml` starts PostgreSQL `energy_community` and RabbitMQ management | Implemented | Add smoke-test instructions for verifying queues, rows, REST JSON, GUI labels. |
| Independent components | Six applications start independently | Six separate Maven modules/apps exist; `shared` removed from active dependencies | Implemented | Avoid reintroducing shared compile-time DTO modules. |
| Message contract | JSON messages documented as contracts, not shared Java dependency | `docs/message-contract.md` documents `EnergyMessage` and `HourlyUsageUpdatedMessage`; local DTOs per service | Implemented | Add serialization tests for required fields and timestamp format. |
| RabbitMQ topology | Producer/user messages to usage; usage update message to percentage | `energy_queue` for producer/user, `percentage_update_queue` for derived update events | Implemented | Exchange/per-service queues only needed if more consumers must observe the same stream. |
| Energy Producer weather | Produced kWh must be weather-dependent | `WeatherClient`, `OpenMeteoWeatherClient`, `FallbackWeatherClient`, `ResilientWeatherClient`, and `WeatherProductionCalculator` are used by `EnergyProducerService` | Implemented | Manual live smoke with RabbitMQ and optional Open-Meteo availability check. |
| Energy Producer message | Sends `PRODUCER`, `association`, `kwh`, `datetime` every few seconds | `EnergyProducerService#createProductionMessage` creates contract-compliant messages; scheduler preserves random 1-5 second sleep | Implemented | Scheduler determinism can be improved in a separate issue. |
| Energy User | Time-of-day-dependent usage message every few seconds | Sends `USER` messages; higher usage morning/evening, lower at night | Implemented | Extract clock/random collaborators for deterministic tests. |
| Usage Service calculation | Aggregate by hour; community pool first; overflow to grid | `HourlyUsageUpdateService` buckets to hour and updates `communityProduced`, `communityUsed`, `gridUsed` | Implemented | Add unit tests for spec example and edge cases. |
| Usage Service persistence | Writes usage table in PostgreSQL | JPA entity/repository for `hourly_usage`; Hibernate `ddl-auto=update` | Partial | Add Flyway migration and set production `ddl-auto=none` or `validate`. |
| Percentage Service calculation | Calculate current community depleted and grid portion | `CurrentPercentageCalculationService` computes `communityUsed / produced * 100` and `gridUsed / totalUsed * 100` | Implemented | Add tests for zero production, zero usage, full depletion, grid usage. |
| Percentage persistence | Writes current percentage table | JPA entity/repository for `current_percentage`; Hibernate `ddl-auto=update` | Partial | Add Flyway migration; decide if table should keep only latest row or latest-per-hour history. |
| REST current endpoint | `GET /energy/current` returns current hour percentage | Reads latest `current_percentage` row from DB; fallback returns zeros | Implemented | Add controller test and clarify latest/current semantics. |
| REST historical endpoint | `GET /energy/historical?start=...&end=...` returns usage for period | Reads `hourly_usage` by date range; supports ISO and `dd.MM.yyyy HH:mm`; invalid range returns 400 | Implemented | Add MockMvc tests for valid/invalid formats and inclusive boundaries. |
| JavaFX GUI | REST-only dashboard with current and historical display | Split into `app`, `controller`, `client`, `dto`, `service`; uses async HTTP and `Platform.runLater` | Implemented | Optional: replace text date fields with controls if time permits. |
| GUI clean code | View/controller/client/DTO/formatting separated | `MainApp` is only entry point; HTTP/Jackson are in `EnergyApiClient` | Implemented | Keep UI class from accumulating business or HTTP logic. |
| Tests | Context, unit, integration, contract tests from lecture testing material | Producer weather/calculation/message tests exist; other modules mostly have startup tests | Partial | Add behavior tests across usage, percentage, REST, GUI. |
| Build artifacts | No generated files committed | `.gitignore` ignores `target/`, but some `target` files may already be tracked | Partial | Remove tracked build artifacts from Git index in a separate cleanup commit. |

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

## Critical Gaps To Address Next

1. **Flyway migrations**
   - Lecture code uses `src/main/resources/db/migration/V...__description.sql`.
   - Current DB schema is produced by Hibernate `ddl-auto=update`.
   - Recommended solution: add migrations for `hourly_usage` and `current_percentage`, then use `ddl-auto=validate` or `none` in runtime config.

2. **Focused behavior tests**
   - The grading allows point deductions for badly written code and failure to explain the system.
   - Tests should make the formulas defensible:
     - Given produced `18.05`, community used `18.02`, grid used `1.056` and a new user message of `0.05` at `14:34`, resulting row must be community used `18.05` and grid used `1.076`.
     - Percentage must produce `100.00` community depleted and approximately `5.63` grid portion for the spec example.

3. **Full distributed smoke test**
   - Automated producer tests pass, but the full Docker plus six-component flow should be manually verified before hand-in.
   - Check RabbitMQ queues, PostgreSQL rows, REST JSON, and GUI labels.

4. **Current percentage semantics**
   - Current code stores rows per hour and REST returns the latest row.
   - Decide whether this is the intended model or whether `current_percentage` should contain exactly one row.

## Lecture Concepts Applied

- Distributed systems: independent components cooperate toward one visible system.
- SOA/microservices: cohesive components with minimal explicit interfaces.
- REST: client-server separation, stable URLs, JSON DTOs, HTTP error semantics.
- JavaFX: MVC/MVVM-inspired separation of entry point, controller, client, DTOs, and formatting.
- Persistence: PostgreSQL, JPA entities/repositories, and planned Flyway migrations for versioned schema.
- Messaging: RabbitMQ broker decouples producers and consumers; `RabbitTemplate` and `@RabbitListener` follow lecture code.
- Testing: context tests, calculation unit tests, endpoint tests, persistence integration tests, and manual end-to-end smoke checks all have distinct roles.

## Recommended Implementation Order

1. Add Flyway migrations for `hourly_usage` and `current_percentage`.
2. Add calculation tests for `usage-service` and `percentage-service`.
3. Add REST controller tests for current/historical endpoints and invalid dates.
4. Add GUI client tests for JSON parsing and URL encoding.
5. Add manual smoke-test runbook.
6. Remove tracked `target/` artifacts from Git index.
