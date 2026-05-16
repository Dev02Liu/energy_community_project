# Final DISYS Specification Audit Report

Audit date: 2026-05-16  
Repository: `C:\dev\energy_community_project`  
Branch observed: `audit-latest-version`  
Audit mode: audit-only; no production code changes were made.

## 1. Executive Summary

Overall status: **MOSTLY READY**

Main conclusion: the project implements the required distributed Energy Community system with six independently startable applications, RabbitMQ-based service communication, PostgreSQL persistence, DB-backed REST endpoints, and a JavaFX GUI that calls REST only. All six module builds passed during this audit with **98 tests, 0 failures**. No confirmed P0 / zero-point technical failure was found.

P0 risks: **0 confirmed**.

P1 risks:

1. Full GUI-inclusive manual smoke test was not executed during this audit. Backend/message/database/REST smoke evidence exists, but visual GUI verification remains a final manual check.
2. Team-member commit requirement is locally plausible but not fully verifiable without the actual team roster and GitHub submission context.

Important correction after inspecting the official Markdown exports: the official GUI specification asks for historical data for a selected date range and explicitly describes aggregated historical values (`Community produced`, `Community used`, `Grid used`). Therefore the current aggregate-label GUI is specification-compatible. A per-hour `TableView` remains an optional demo/readability improvement, not a confirmed grading requirement.

Most important next actions:

1. Run `docs/smoke-test.md` once with the visible JavaFX GUI before submission and capture evidence.
2. In the demo, explicitly explain that the GUI historical section displays aggregated kWh totals for the selected range, matching the official specification.
3. Verify the real team roster against `git shortlog -sn --all` and update README Team Contributions with real names/usernames.
4. Be prepared to explain that `current_percentage` returns the latest/current row even though older percentage rows can remain in the table.
5. Keep Java 25 / Spring Boot 4.0.3 clearly documented for the final reviewer machine.
6. Be prepared to explain direct RabbitMQ queues and DB table naming deviations.

Final recommendation: **ready for final technical rehearsal, not yet final-signoff ready until the GUI smoke and team-commit checks are completed.**

## 2. Sources Inspected

| Source | Type | Inspected | Relevant Requirement Extracted |
|---|---|---:|---|
| `project-resources/projektgrading_schema.md` | official grading Markdown export | YES | Milestone and final must-haves, final weighting, GUI/REST/Producer/User/Usage/Percentage grading criteria, 0-point risk language. |
| `project-resources/projektspezifikationen_semesterprojekt.md` | official specification Markdown export | YES | Six independent components, exact message fields, Usage and Percentage formulas, GUI/REST requirements, historical data as selected-range aggregated values, official component flow. |
| `project-resources/LEcture-Materials/ProjektGrading.pdf` | official grading PDF | PARTIAL | PDF exists but has no machine-extractable text via `pypdf`; the readable official Markdown export above is now used as the authoritative grading text. |
| `project-resources/LEcture-Materials/Projektspezifikationen.pdf` | official specification PDF | PARTIAL | PDF exists but has no machine-extractable text via `pypdf`; the readable official Markdown export above is now used as the authoritative specification text. |
| `project-resources/ProjektGrading.extracted.txt` | extracted text | YES | File is effectively empty; no usable text. |
| `project-resources/Projektspezifikationen.extracted.txt` | extracted text | YES | File is effectively empty; no usable text. |
| `project-resources/DISYS_Semesterprojekt_Sehr_gut_Mapping.md` | project mapping | YES | Six components, must-haves, final component weights, REST endpoints, RabbitMQ flow, DB tables, calculation formulas, documentation expectations. |
| `project-resources/Lecture Transcripts.txt` | lecture transcript | YES | Lecturer states six independent components, REST API/JavaFX, RabbitMQ services, one DB, hourly aggregation, commit requirement, main grading risks. |
| `project-resources/github-issues-until-final-handin.md` | planning/reference | YES | Issues 1-15 map implementation hardening, tests, runbook, cleanup, docs. |
| `project-resources/LEcture-Materials/rest.pdf` | lecture slides | PARTIAL | Text extraction confirms REST/Spring Boot REST concepts; detailed slide content not exhaustively quoted. |
| `project-resources/LEcture-Materials/S4-DISYS_05_JavaFXBeginning.pdf` | lecture slides | PARTIAL | JavaFX Stage/Scene/Controls concepts readable. |
| `project-resources/LEcture-Materials/S4-DISYS_06_JavaFXProgramming.pdf` | lecture slides | PARTIAL | UI logic separation and REST client connection concepts readable. |
| `project-resources/LEcture-Materials/database-handson.pdf` | lecture slides | PARTIAL | PostgreSQL/JDBC/Flyway/JPA topics confirmed. |
| `project-resources/LEcture-Materials/database-theory.pdf` | lecture slides | PARTIAL | RDBMS/transaction context confirmed. |
| `project-resources/LEcture-Materials/Einheit_11-Testing.pdf` | lecture slides | PARTIAL | Unit/integration/contract/E2E testing taxonomy confirmed. |
| `project-resources/LEcture-Materials/scaling.pdf` | lecture slides | PARTIAL | Message queue / RabbitMQ scaling context confirmed. |
| `project-resources/LEcture-Materials/S4-DISYS_09_Design.pdf` | lecture slides | PARTIAL | UML/component diagram concepts confirmed. |
| `project-resources/Lecture-Code/disys26bwi1/...` | lecture code | YES | Spring Boot controllers, repositories, Flyway migrations, RabbitTemplate, `@RabbitListener` examples. |
| `README.MD` | project documentation | YES | Architecture, prerequisites, start order, REST endpoints, topology, DB schema, demo flow, known limitations. |
| `docs/documentation-overview.md` | project documentation | YES | System diagram and module documentation index. |
| `docs/message-contract.md` | project documentation | YES | Direct queue topology, JSON contracts, contract tests. |
| `docs/database-schema.md` | project documentation | YES | `hourly_usage` / `current_percentage` mapping and service responsibilities. |
| `docs/how-to-run.md` | project documentation | YES | Startup and verification steps. |
| `docs/smoke-test.md` | project documentation | YES | Full distributed smoke-test runbook. |
| `docs/final-readiness-check.md` | project documentation | YES | Final checklist and prior smoke evidence. |
| `docs/spec-code-mapping.md` | project documentation | YES | Requirement-to-code matrix and test counts. |
| `docs/project-audit-report.md` | previous audit | YES | Historical baseline; many earlier risks have since been addressed. |
| Six module docs in `docs/*.md` | project documentation | YES | Per-module tech stack, responsibilities, diagrams, commands. |

## 3. Requirement Baseline

Extracted baseline, ordered by source priority:

- The project must implement six independently startable components: Energy Producer, Energy User, Usage Service, Current Percentage Service, Spring Boot REST API, JavaFX GUI.
- The final system must use RabbitMQ for communication between Producer/User/Usage/Percentage services.
- The GUI communicates with the REST API over HTTP and must not connect directly to PostgreSQL.
- The final GUI must show current community pool usage and grid portion in percent, offer a refresh button, allow date-range selection, and show historical `community produced`, `community used`, and `grid used` data in kWh for that selected range. The official specification example describes these historical values as aggregated totals; tables, labels, or text areas are valid display components.
- The REST API is Spring Boot and exposes at least `GET /energy/current` and `GET /energy/historical?start=...&end=...`.
- The REST API reads DB-backed data and must not replace Usage/Percentage business logic.
- PostgreSQL is the shared persistence store; DB setup should be reproducible with Docker Compose and migrations.
- Producer/User messages are hourly input events; Usage Service aggregates them into hourly rows.
- User demand is covered by community energy first; residual demand becomes grid usage. There is no Grid message producer.
- Usage Service publishes an update message after valid DB updates; Percentage Service reacts to that update.
- The official specification describes `current_percentage` as current-hour percentage information; retaining older percentage rows is an implementation deviation unless explained as "latest row is current."
- Percentage formulas:
  - `community_depleted = community_used / community_produced * 100`, or `0` when produced is `0`.
  - `grid_portion = grid_used / (community_used + grid_used) * 100`, or `0` when total usage is `0`.
- Every team member must have real Git commits.
- Usage Service and Percentage Service are the highest grading-value components.
- Java/Spring versions are review-environment risks if undocumented, but not core grading targets if builds/start commands are clear.
- Direct RabbitMQ queues are acceptable if working and documented; exchange/routing-key topology is a design extension, not a confirmed hard requirement from the readable sources.

## 4. Critical Must-Have Checklist

| Requirement | Status | Evidence | Risk | Fix Needed |
|---|---|---|---|---|
| Independent components | PASS | Six directories with own `pom.xml` / entry point: `energy-producer`, `energy-user`, `usage-service`, `percentage-service`, `rest-api`, `energy-gui`; README component table and module docs. | P0 if absent; no confirmed issue. | None. |
| Build/run without errors | PASS | `clean package` passed for all six modules during audit; Docker Compose config valid; containers running. | Low. | Repeat before hand-in. |
| Spring Boot REST API | PASS | `rest-api/src/main/java/.../RestApiApplication.java`, `EnergyController`; endpoints `/energy/current`, `/energy/historical`. | Low. | None. |
| JavaFX GUI | PASS | `energy-gui` uses JavaFX, `MainApp`, `EnergyGuiApplication`, `EnergyDashboardController`; current values and selected-range historical aggregate values are displayed via REST. | Low; optional P2 readability improvement only. | None required by official spec. |
| RabbitMQ communication | PASS | `RabbitTemplate` in Producer/User/Usage; `@RabbitListener` in Usage/Percentage; queues documented. | Low. | None. |
| PostgreSQL | PASS | `docker-compose.yml`; JPA/Flyway in Usage/Percentage/REST; DB inspection shows `hourly_usage`, `current_percentage`, `flyway_schema_history`. | Low. | None. |
| GUI no direct DB access | PASS | `energy-gui/pom.xml` has JavaFX/Jackson only; no JPA/JDBC/PostgreSQL dependency found. | Low. | None. |
| REST API read-only for business data | PASS | No controller `save()` in production REST API; only repository reads in `EnergyController`. | Low. | Optional query service separation. |
| Producer/User no DB writes | PASS | Producer/User exclude JDBC/JPA autoconfig and have no DB dependencies; publish via RabbitMQ. | Low. | None. |
| Usage Service processes messages | PASS | `EnergyMessageListener` consumes `${app.queue.name}`; `HourlyUsageUpdateService` calculates, saves, publishes update. | Low. | None. |
| Percentage Service processes update events | PASS | `HourlyUsageUpdatedListener` consumes `${app.update-queue.name}` and delegates to calculation service. | Low. | None. |
| Git/team commits | PARTIAL | `git shortlog -sn --all`: OnlyMajorG 38, Yijie Liu 12, stefangirgis 7. Actual team roster not available. | P1 manual grading risk if any listed team member lacks real commits. | Verify roster and update README Team Contributions. |

## 5. Component Mapping

| Expected Component | Actual Module | Main Class | Start Command | Status |
|---|---|---|---|---|
| Energy Producer | `energy-producer` | `com.energy_community_project.energy_producer.EnergyProducerApplication` | `cd energy-producer; .\mvnw.cmd spring-boot:run` | PASS |
| Energy User | `energy-user` | `com.energy_community_project.energy_user.EnergyUserApplication` | `cd energy-user; .\mvnw.cmd spring-boot:run` | PASS |
| Usage Service | `usage-service` | `com.energy_community_project.usage_service.UsageServiceApplication` | `cd usage-service; .\mvnw.cmd spring-boot:run` | PASS |
| Current Percentage Service | `percentage-service` | `com.energy_community_project.percentage_service.PercentageServiceApplication` | `cd percentage-service; .\mvnw.cmd spring-boot:run` | PASS |
| Spring Boot REST API | `rest-api` | `com.energy_community_project.rest_api.RestApiApplication` | `cd rest-api; .\mvnw.cmd spring-boot:run` | PASS |
| JavaFX GUI | `energy-gui` | `com.energy_community_project.gui.MainApp` | `cd energy-gui; ..\energy-producer\mvnw.cmd -f pom.xml javafx:run` | PASS |
| PostgreSQL | Docker Compose service `postgres` / container `energy-db` | n/a | `docker compose up -d` | PASS |
| RabbitMQ | Docker Compose service `rabbitmq` / container `energy-mq` | n/a | `docker compose up -d` | PASS |

## 6. Component Audits

### 6.1 Energy Producer

Expected behavior: independently start, calculate plausible weather-influenced production, publish `PRODUCER` messages with `type`, `association`, `kwh`, `datetime`, no DB writes.

Observed implementation:

- Spring Boot app: `EnergyProducerApplication`.
- Scheduling: `EnergyProducerScheduler` with `@Scheduled`.
- Message publishing: `EnergyProducerService#publishProductionData()` uses `RabbitTemplate.convertAndSend(queueName, msg)`.
- Weather abstraction: `WeatherClient`, `OpenMeteoWeatherClient`, `FallbackWeatherClient`, `ResilientWeatherClient`.
- Calculation: `WeatherProductionCalculator`.
- Config: `app.queue.name=energy_queue`, `server.port=8081`, `app.weather.mode=open-meteo`, fallback documented.
- DB access excluded by `spring.autoconfigure.exclude` and no JPA/PostgreSQL dependency.

Status: **PASS**

Risks:

- P2: uses Spring WebMVC and opens port `8081` although it is not a REST service. This is documented and works, but can create avoidable port conflicts.
- P3: runtime logs use `System.out.println`; acceptable for demo but less professional than structured logging.

Recommendations:

- Keep fallback weather mode for demo reliability.
- Be ready to explain why production kWh values are larger than the sample `0.003` values in the specification examples; they are plausible simulation units but not identical to sample scale.

### 6.2 Energy User

Expected behavior: independently start, calculate time-of-day-dependent usage, publish `USER` messages with required fields, no DB writes.

Observed implementation:

- Spring Boot app: `EnergyUserApplication`.
- Scheduling: `EnergyUserScheduler`.
- Message publishing: `EnergyUserService#publishUsageData()` uses `RabbitTemplate.convertAndSend`.
- Calculation: `EnergyUsageCalculator` with peak, normal and night multipliers.
- Randomness: `UsageVariationProvider` / `RandomUsageVariationProvider`.
- Config: `app.queue.name=energy_queue`, `server.port=8082`.
- DB access excluded by `spring.autoconfigure.exclude`.

Status: **PASS**

Risks:

- P2: peak windows differ slightly from the mapping example (`07-09`, `18-21`, night `23-05`), but they remain plausible and are documented in `docs/energy-user.md`.
- P2: Spring WebMVC dependency/port is unnecessary for a publisher-only service.

Recommendations:

- If challenged, explain the time profile as a documented implementation choice.

### 6.3 Usage Service

Expected behavior: consume Producer/User messages, aggregate to hour, split community/grid usage correctly, write DB, publish update message after valid processing, use transactions and tests.

Observed implementation:

- Listener: `EnergyMessageListener` with `@RabbitListener(queues = "${app.queue.name}")`.
- Business logic: `HourlyUsageUpdateService#handleEnergyMessage()` is `@Transactional`.
- Validation rejects null message, invalid type, invalid association, null datetime, negative/NaN/infinite kWh.
- Hour bucketing: `toHour()` sets minute/second/nano to zero.
- Producer handling increments `communityProduced`.
- User handling computes available community energy, community portion, grid portion.
- Persistence: `HourlyUsageRepository` / `HourlyUsageEntity`.
- Update publish: `RabbitTemplate.convertAndSend(updateQueueName, new HourlyUsageUpdatedMessage(hour))`.
- Tests: 26 module tests including message contract, migration, aggregation, invalid messages, update publishing.

Status: **PASS**

Risks:

- P2: DB writes and RabbitMQ publish occur in one method/transaction; RabbitMQ publish is not transactionally atomic with DB commit. For this course scope this is acceptable, but it is a real distributed-systems nuance to explain.

Recommendations:

- In review, state that message order affects values as the lecturer described; the tests cover user-before-producer behavior.

### 6.4 Current Percentage Service

Expected behavior: consume update event, read hourly usage row, compute percentages, handle division by zero, write `current_percentage`.

Observed implementation:

- Listener: `HourlyUsageUpdatedListener` with `@RabbitListener(queues = "${app.update-queue.name}")`.
- Service: `CurrentPercentageCalculationService#updateCurrentPercentage()` is `@Transactional`.
- Reads `hourly_usage`, writes/upserts `current_percentage`.
- Handles null hour and missing usage row by returning safely.
- Handles zero denominators and rounds to two decimals via `BigDecimal.setScale(2, HALF_UP)`.
- Tests: 14 module tests including spec example, zero cases, existing row update, contract deserialization.

Status: **PASS**

Risks:

- P2: `current_percentage` is stored as one row per processed hour, while `projektspezifikationen_semesterprojekt.md` says the table "only holds the information of the current hour." The REST API returns the latest row, so the user-facing current view is correct, but the persistence model is slightly more historical than the official wording.
- P3: `percentage-service/src/main/resources/application.properties` contains `app.queue.name=energy_queue` although this service does not consume that queue. It is documented as unused; remove later to reduce confusion.

Recommendations:

- Be ready to explain that `current_percentage` keeps one row per processed hour and REST treats the latest row as current, or change the service to keep only one current row if the lecturer expects the wording literally.

### 6.5 REST API

Expected behavior: Spring Boot REST API, two required GET endpoints, DB-backed read-only data, stable DTOs, validation and HTTP errors.

Observed implementation:

- Spring Boot app: `RestApiApplication`.
- Controller: `EnergyController` with `@RequestMapping("/energy")`.
- Endpoints:
  - `GET /energy/current`
  - `GET /energy/historical?start=...&end=...`
- Reads repositories: `CurrentPercentageRepository`, `HourlyUsageRepository`.
- DTO responses: `CurrentPercentageDTO`, `HistoricalUsageDTO`.
- Date parsing supports ISO and `dd.MM.yyyy HH:mm`.
- Invalid date and `start > end` return `400 Bad Request`.
- No production `save()` calls in controller.
- Tests: 25 module tests including endpoint contracts and H2-backed DB fixtures.

Status: **PASS**

Risks:

- P2: controller directly uses repositories instead of a query service. Not a grading failure; minor separation-of-concerns issue.
- P3: `spring.jpa.open-in-view` warning appears in tests. REST DTO mapping avoids entity exposure, so impact is low.

Recommendations:

- Keep REST read-only and do not add POST/PUT writes unless explicitly required.

### 6.6 JavaFX GUI

Expected behavior: independently start, JavaFX UI, REST-only, no DB access, current and historical data visible, date range input, graceful errors.

Observed implementation:

- Entry point: `MainApp`; JavaFX app: `EnergyGuiApplication`.
- UI/controller: `EnergyDashboardController`.
- HTTP client: `EnergyApiClient` using Java `HttpClient`.
- DTOs: `CurrentPercentageDTO`, `HistoricalUsageDTO`.
- Formatting: `EnergyValueFormatter`.
- Current button calls `/energy/current`.
- Historical button calls `/energy/historical`.
- Date parsing supports GUI and ISO formats.
- GUI has no JPA/JDBC/PostgreSQL/RabbitMQ dependencies.
- Tests: 21 tests for REST client parsing, URL encoding, date validation, formatting.

Status: **PASS**

Risk:

- P2: historical data is aggregated into three labels. After checking `projektspezifikationen_semesterprojekt.md`, this matches the official example GUI, which lists aggregated historical `Community produced`, `Community used`, and `Grid used` values for the selected date range. A per-hour table would improve inspectability but is not required by the official specification.
- P2: REST base URL is hardcoded to `http://localhost:8080` in `EnergyGuiApplication`.
- P2: empty historical result currently maps to the generic `"Error fetching data"` message, which is less clear than `"No data for selected range"`.

Recommendations:

- Keep the aggregate display and explain it clearly in the demo, or optionally add a per-hour table as a usability enhancement.
- Make API base URL configurable or document the hardcoded local demo assumption.

### 6.7 PostgreSQL

Expected behavior: shared DB, reproducible schema, Usage writes hourly usage, Percentage writes percentages, REST reads, GUI/Producer/User do not access DB.

Observed implementation:

- Docker Compose service `postgres`, container `energy-db`, DB `energy_community`.
- Flyway migrations in Usage, Percentage and REST modules.
- Runtime schema inspected during audit:
  - `flyway_schema_history`
  - `hourly_usage`
  - `current_percentage`
- Latest DB rows from prior smoke exist:
  - `hourly_usage`: `2026-05-16 11:00:00 | 228.52500000000003 | 36.633326434041905 | 4.404378895368673`
  - `current_percentage`: `2026-05-16 11:00:00 | 16.03 | 10.73`

Status: **PASS**

Risks:

- P2: schema uses `DOUBLE PRECISION`, while the target mapping uses `NUMERIC`. For this small simulation the behavior is acceptable; floating-point artifacts are visible in DB values.
- P2: table name is `hourly_usage` rather than conceptual `energy_usage_hourly`; documented in `docs/database-schema.md`.
- P3: `updated_at` / `calculated_at` audit columns are omitted; documented as intentional.

Recommendations:

- If exact schema names/types are challenged, point to `docs/database-schema.md`; optionally migrate to `NUMERIC(12,6)` / `NUMERIC(8,2)` later.

### 6.8 RabbitMQ

Expected behavior: producer/user publish to RabbitMQ, usage consumes, usage publishes update, percentage consumes update; contracts documented.

Observed implementation:

- Queues:
  - `energy_queue`
  - `percentage_update_queue`
- Queue beans are durable (`new Queue(queueName, true)`).
- Producer/User publish to `energy_queue`.
- Usage consumes `energy_queue` and publishes `percentage_update_queue`.
- Percentage consumes `percentage_update_queue`.
- JSON converter configured in relevant services.
- RabbitMQ Management API during audit showed both queues, `messages=0`, durable classic queues, historical publish/ack stats from prior smoke; no consumers because services were not running during that check.
- `docs/message-contract.md` documents topology, contracts and direct-queue justification.
- Contract tests exist in Producer/User/Usage/Percentage modules.

Status: **PASS**

Risks:

- P2: no exchanges/routing keys. This is acceptable because direct queues are documented and no readable official source proved exchanges are mandatory.
- P2: no retry/dead-letter topology. Not required for grading, but useful to mention as out of scope.

Recommendations:

- Explain direct queues as one-consumer streams and a deliberate simplification.

## 7. Business Logic Audit

Producer logic: **PASS**. `EnergyProducerService#createProductionMessage()` obtains weather from `WeatherClient`, calculates production with `WeatherProductionCalculator`, creates `type=PRODUCER`, `association=COMMUNITY`, `kwh`, `datetime`.

User logic: **PASS**. `EnergyUserService#createUsageMessage()` uses current time, random variation, `EnergyUsageCalculator`, then creates `type=USER`, `association=COMMUNITY`, `kwh`, `datetime`.

Hourly aggregation: **PASS**. `HourlyUsageUpdateService#toHour()` truncates minute, second and nano. Tests cover same-hour and different-hour behavior.

Usage calculation: **PASS**. User messages consume available community energy first, then grid residual. The invariant `communityUsed <= communityProduced` is preserved by construction.

Community/grid split: **PASS**. `availableCommunityEnergy = max(produced - communityUsed, 0)`, `communityPortion = min(requested, available)`, `gridPortion = max(requested - communityPortion, 0)`.

Current percentage calculation: **PASS**. Formula matches expected semantics and tests include the spec example `18.05 / 18.05`, `1.076 / 19.126`.

Division-by-zero handling: **PASS**. Produced `0` returns community depleted `0`; total used `0` returns grid portion `0`.

Invalid message handling: **PASS**. Malformed messages are rejected without DB writes or update messages.

Edge case coverage: **PASS**. Tests cover producer aggregation, user no production, less/more than available community energy, full depletion, user-before-producer, invalid type, invalid kWh, null datetime, invalid association.

Residual risk: **P2**. Double precision storage exposes floating-point decimal tails in DB rows; not a functional error.

## 8. Architecture Audit

Distributed-system assessment: **PASS**. The system is composed of separate applications communicating through REST, RabbitMQ and a shared PostgreSQL database.

Component boundaries:

- Producer/User publish only messages.
- Usage owns hourly usage writes.
- Percentage owns current percentage writes.
- REST reads DB and exposes HTTP.
- GUI calls REST and has no DB dependency.

Synchronous communication:

- JavaFX GUI -> REST API via HTTP GET.

Asynchronous communication:

- Producer/User -> RabbitMQ -> Usage.
- Usage -> RabbitMQ -> Percentage.

Database ownership/read-write matrix:

| Table | Writer | Readers |
|---|---|---|
| `hourly_usage` | Usage Service | Percentage Service, REST API |
| `current_percentage` | Percentage Service | REST API |

Direct dependency risks:

- No shared DTO module dependency was found.
- No Producer/User DB dependency was found.
- No GUI DB/RabbitMQ dependency was found.

Documentation quality:

- README, `documentation-overview.md`, `message-contract.md`, `database-schema.md`, `smoke-test.md`, module docs and Mermaid diagrams exist.
- Previous audit report is correctly marked as historical baseline.

Status: **PASS**

## 9. REST API Audit

Endpoints:

| Endpoint | Status | Evidence |
|---|---|---|
| `GET /energy/current` | PASS | `EnergyController#getCurrentPercentage()` |
| `GET /energy/historical?start=...&end=...` | PASS | `EnergyController#getHistoricalData()` |

Request parameters:

- `historical.start`: required string; ISO or `dd.MM.yyyy HH:mm`.
- `historical.end`: required string; ISO or `dd.MM.yyyy HH:mm`.

Response structures:

- Current: `hour`, `communityDepleted`, `gridPortion`.
- Historical: `hour`, `communityProduced`, `communityUsed`, `gridUsed`.

Status codes:

- Valid requests return JSON.
- Invalid dates throw `ResponseStatusException(BAD_REQUEST)`.
- `start > end` throws `BAD_REQUEST`.

DB access:

- `CurrentPercentageRepository#findFirstByOrderByHourDesc()`.
- `HourlyUsageRepository#findByHourBetween(...)`.

Read-only verification:

- No production REST controller `save()` calls found.
- REST tests save fixtures only in test code.

Status: **PASS**

## 10. GUI Audit

REST usage: **PASS**. `EnergyApiClient` uses Java `HttpClient` and Jackson to call REST endpoints.

No DB access verification: **PASS**. GUI dependencies are JavaFX and Jackson; searches found no JDBC/JPA/PostgreSQL usage in GUI.

Current display: **PASS**. Current values update `communityPoolLabel` and `gridPortionLabel`.

Historical display: **PASS**. Returned rows are summed and displayed as aggregate kWh labels for the selected date range. This matches the official specification example, which asks the GUI to show aggregated historical `Community produced`, `Community used`, and `Grid used` values.

TableView or equivalent: **NOT REQUIRED**. The milestone grading text allows tables, labels or text areas; the final specification example uses aggregated values. No `TableView` is therefore a usability limitation, not a requirement miss.

Error handling: **PARTIAL**. API errors are shown, but empty historical result uses a generic error message.

Base URL config: **PARTIAL**. Hardcoded `http://localhost:8080`; documented but not configurable.

Status: **PASS with P2 usability/configuration risks**

## 11. RabbitMQ Audit

Queues:

- `energy_queue`: Producer/User publish, Usage consumes.
- `percentage_update_queue`: Usage publishes, Percentage consumes.

Exchanges/routing keys:

- None custom. Direct queue publishing is used.

Producers:

- `EnergyProducerService`
- `EnergyUserService`
- `HourlyUsageUpdateService` for update event.

Consumers:

- `EnergyMessageListener`
- `HourlyUsageUpdatedListener`

Message contracts:

- `EnergyMessage`: `type`, `association`, `kwh`, `datetime`.
- `HourlyUsageUpdatedMessage`: `hour`.
- Documented in `docs/message-contract.md`.
- Protected by service-local JSON contract tests.

Direct queue justification:

- Documented: one consumer per stream; direct queues keep architecture simple while preserving asynchronous decoupling.

Status: **PASS**

## 12. Database Audit

Schema:

```sql
hourly_usage(hour, community_produced, community_used, grid_used)
current_percentage(hour, community_depleted, grid_portion)
```

Migrations:

- `usage-service/src/main/resources/db/migration/V1__create_energy_tables.sql`
- `percentage-service/src/main/resources/db/migration/V1__create_energy_tables.sql`
- `rest-api/src/main/resources/db/migration/V1__create_energy_tables.sql`

Entities:

- Usage: `HourlyUsageEntity`.
- Percentage: `HourlyUsageEntity`, `CurrentPercentageEntity`.
- REST: `HourlyUsageEntity`, `CurrentPercentageEntity`.

Repositories:

- JPA repositories in DB-backed modules.

Flyway/JPA alignment:

- `ddl-auto=validate` in runtime properties.
- Flyway creates schema.
- H2 migration tests confirm schema creation in tests.

Table responsibility matrix:

| Table | Columns | Writer | Readers | Status |
|---|---|---|---|---|
| `hourly_usage` | `hour`, `community_produced`, `community_used`, `grid_used` | Usage Service | Percentage Service, REST API | PASS |
| `current_percentage` | `hour`, `community_depleted`, `grid_portion` | Percentage Service | REST API | PASS |

Naming deviations:

- `hourly_usage` instead of `energy_usage_hourly`: documented.
- `current_percentage` stores rows by `hour`; official text says the table only holds current-hour information. REST resolves this by returning the latest row, but the persistence behavior should be explained during review.
- No `updated_at` / `calculated_at`: documented.
- `DOUBLE PRECISION` instead of `NUMERIC`: documented less explicitly; P2 precision/review risk.

Status: **PASS with P2 schema-precision/documentation risk**

## 13. Testing Audit

Tests found:

- Unit tests for Producer weather/message and User usage/message.
- Usage calculation and validation tests.
- Percentage calculation tests.
- REST controller/endpoint contract tests with H2.
- Repository/Flyway migration tests.
- GUI HTTP client, date validation and formatter tests.
- RabbitMQ JSON contract tests.

Tests run during this audit:

| Module | Test Command | Result | Notes |
|---|---|---|---|
| `energy-producer` | `.\mvnw.cmd clean package` | PASS, 7 tests | Weather, fallback, production calculator, message contract. |
| `energy-user` | `.\mvnw.cmd clean package` | PASS, 5 tests | Usage calculator, fixed-clock message creation, message contract. |
| `usage-service` | `.\mvnw.cmd clean package` | PASS, 26 tests | Flyway/H2 migration, aggregation, invalid messages, update publish, contract. |
| `percentage-service` | `.\mvnw.cmd clean package` | PASS, 14 tests | Flyway/H2 migration, formulas, rounding, contract. |
| `rest-api` | `.\mvnw.cmd clean package` | PASS, 25 tests | Endpoint contracts, date validation, latest row, migration. |
| `energy-gui` | `..\energy-producer\mvnw.cmd -f pom.xml clean package` | PASS, 21 tests | HTTP parsing, URL encoding, date validation, formatting. |
| Docker | `docker compose config; docker compose ps` | PASS | Config valid; `energy-db` and `energy-mq` running. |

Total: **98 tests, 0 failures**.

Warnings observed:

- Mockito dynamic agent warnings on Java 25.
- H2 version newer than Flyway verified version.
- GUI Maven encoding warnings.

These are P3/P2 tooling warnings, not functional failures.

Missing / recommended tests:

- P1/P2: manual visible JavaFX E2E verification with screenshots/log notes.
- P2: listener-level AMQP conversion/integration test with actual Spring AMQP test container is optional.

## 14. Documentation Audit

README status: **PASS**. Covers goal, architecture, components, prerequisites, Java/Spring versions, Docker, build/start commands, ports, REST, RabbitMQ, DB, GUI, demo flow, known limitations, team contribution check.

How-to-run status: **PASS**. Contains stepwise startup and verification.

Smoke-test runbook: **PASS**. `docs/smoke-test.md` covers start order, RabbitMQ UI, SQL, REST, GUI verification and evidence capture.

Message contract docs: **PASS**. `docs/message-contract.md` describes queue topology, payloads, fields, validation and contract tests.

DB schema docs: **PASS**. `docs/database-schema.md` maps implemented names to conceptual spec names and read/write responsibilities.

Architecture docs: **PASS**. `docs/documentation-overview.md` and per-module docs include Mermaid diagrams.

Module docs: **PASS**. Six module files exist and explain tech stack/components/flows.

Previous audit docs: **PASS**. `docs/project-audit-report.md` is marked as historical baseline.

Documentation gaps:

- P1: README Team Contributions still needs real-name/team roster confirmation.
- P2: GUI historical display should be described consistently as selected-range aggregate values, because that is what the official specification example requires.

## 15. End-to-End Readiness

Executed during this audit:

- `docker compose config`
- `docker compose ps`
- DB table inspection via `docker exec energy-db psql ...`
- RabbitMQ queue inspection via Management API
- All module `clean package` builds

Observed outputs:

- Docker containers:
  - `energy-db` running on `5432`
  - `energy-mq` running on `5672` and `15672`
- PostgreSQL tables:
  - `flyway_schema_history`
  - `hourly_usage`
  - `current_percentage`
- DB rows from prior smoke:
  - `hourly_usage`: latest `2026-05-16 11:00:00`
  - `current_percentage`: latest `2026-05-16 11:00:00`, values `16.03`, `10.73`
- RabbitMQ queues:
  - `energy_queue`
  - `percentage_update_queue`
  - both durable, zero messages at inspection time, no active consumers because services were not running.

Not executed during this audit:

- Full live Producer/User/Usage/Percentage/REST flow from fresh DB.
- Visible JavaFX GUI verification.

Existing evidence:

- `docs/final-readiness-check.md` records a prior fresh-DB backend/message/database/REST smoke test.

Remaining manual checks:

1. Run `docs/smoke-test.md` with all six components.
2. Verify JavaFX labels visually.
3. Capture RabbitMQ, DB, REST and GUI evidence for final presentation.

## 16. Prioritized Fix List

### P0 - Critical, possible 0-point issue

No confirmed P0 issues.

Manual P0-sensitive checks:

- Problem: actual team roster is unknown to this audit.
- Evidence: local `git shortlog -sn --all` shows three authors: OnlyMajorG, Yijie Liu, stefangirgis.
- Affected files: `README.MD` Team Contributions section.
- Required fix: verify every submitted team member maps to one of these authors and has real code/docs contributions.
- Acceptance criteria: README lists real names, GitHub usernames and main contributions; `git shortlog -sn --all` supports the claim.

### P1 - High grading risk

1. Problem: full visible GUI E2E smoke was not executed in this audit.
   - Evidence: backend smoke documented, but current audit only ran builds and inspected DB/RabbitMQ state.
   - Affected files: `docs/smoke-test.md`, `docs/final-readiness-check.md`.
   - Required fix: execute `docs/smoke-test.md` end-to-end, including JavaFX.
   - Acceptance criteria: final checklist marks GUI current and historical display observed; screenshots/log notes saved if useful.

### P2 - Quality/review risk

1. Problem: REST API controller directly uses repositories.
   - Evidence: `EnergyController` injects repositories directly.
   - Affected files: `rest-api/src/main/java/.../EnergyController.java`.
   - Recommended fix: optional read-only query service for separation.

2. Problem: GUI REST base URL is hardcoded.
   - Evidence: `EnergyGuiApplication.BASE_URL = "http://localhost:8080"`.
   - Affected files: `energy-gui/src/main/java/.../EnergyGuiApplication.java`.
   - Recommended fix: make configurable via system property/env var; keep default local URL.

3. Problem: DB uses `DOUBLE PRECISION`; spec mapping suggests numeric precision.
   - Evidence: Flyway `V1__create_energy_tables.sql`.
   - Affected files: DB migrations/entities.
   - Recommended fix: leave as documented for submission stability or migrate to `NUMERIC` in a new migration if exact precision is demanded.

4. Problem: direct queue topology lacks exchange/routing keys.
   - Evidence: `RabbitMqConfig` declares durable queues only.
   - Affected files: RabbitMQ config, docs.
   - Recommended fix: no urgent change; explain direct queue design. Add exchange only if lecturer explicitly expects it.

5. Problem: Producer/User include WebMVC and open service ports though they do not expose REST APIs.
   - Evidence: `spring-boot-starter-webmvc`, `server.port=8081/8082`.
   - Affected files: Producer/User `pom.xml` and properties.
   - Recommended fix: acceptable if documented; optionally remove WebMVC after regression testing.

6. Problem: empty historical GUI result displays generic error text.
   - Evidence: `showHistoricalUsage()` calls `showHistoricalError(ERROR_MESSAGE)` for empty entries.
   - Affected files: `EnergyDashboardController.java`.
   - Recommended fix: use `"No data for selected range"`.

7. Problem: JavaFX historical data is not per-hour inspectable.
   - Evidence: `EnergyDashboardController#showHistoricalUsage()` sums returned rows into labels; no `TableView`.
   - Affected files: `energy-gui/src/main/java/.../EnergyDashboardController.java`, `docs/energy-gui.md`.
   - Recommended fix: optional per-hour `TableView<HistoricalUsageDTO>` if the team wants stronger demo visibility.
   - Reason: not required by `projektspezifikationen_semesterprojekt.md`, which describes aggregated historical values for the selected range.

8. Problem: `current_percentage` stores percentage rows per processed hour, while the official specification says the table only holds current-hour information.
   - Evidence: `CurrentPercentageCalculationService` upserts by update `hour`; REST returns the latest row.
   - Affected files: `percentage-service/src/main/java/.../CurrentPercentageCalculationService.java`, `rest-api/src/main/java/.../CurrentPercentageRepository.java`, `docs/database-schema.md`.
   - Recommended fix: leave as documented if accepted by the team, because the REST current endpoint is correct; otherwise change persistence to maintain only the latest/current row and retest.

### P3 - Optional improvement

1. Problem: JavaFX Maven build emits platform encoding warnings.
   - Recommended improvement: add `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`.

2. Problem: Spring test warnings for Mockito dynamic agent on Java 25.
   - Recommended improvement: configure Mockito agent when upgrading test setup; not urgent for submission.

3. Problem: `percentage-service` has unused `app.queue.name`.
   - Recommended improvement: remove property or keep documented as unused.

4. Problem: demo credentials are hardcoded.
   - Recommended improvement: acceptable for local course demo; for production use environment variables.

5. Problem: prior official PDFs were not fully text-readable in this audit.
   - Recommended improvement: keep OCR/text exports of grading/spec PDFs in `project-resources` for future audits.

## 17. Final Recommendation

The project is **mostly ready for final submission** from a technical perspective. It satisfies the major must-haves: independent components, Spring Boot REST API, JavaFX GUI, RabbitMQ service communication, PostgreSQL persistence, Flyway migrations, DB-backed REST reads, no GUI DB access, no Producer/User DB writes, and correct Usage/Percentage calculations.

Must be completed before submission:

1. Run the full manual `docs/smoke-test.md` with the visible JavaFX GUI.
2. Verify and document every real team member's commits.
3. During the demo, present the historical GUI values as selected-range aggregate kWh totals, which matches the official specification wording.

Can remain documented as limitations:

- Java 25 / Spring Boot 4.0.3, as long as the reviewer machine has Java 25.
- Direct durable RabbitMQ queues instead of exchange/routing-key topology.
- `hourly_usage` as the implementation name for conceptual `energy_usage_hourly`.
- No authentication/cloud/Kubernetes/API gateway.
- Omitted technical timestamp columns.

Each team member must be able to explain:

- why the system is distributed,
- which six components are independently startable,
- synchronous REST vs asynchronous RabbitMQ,
- why GUI uses REST and not DB,
- why Producer/User do not write DB,
- how Usage Service aggregates by hour,
- how `communityUsed <= communityProduced` is preserved,
- how grid usage is calculated as residual demand,
- how Percentage Service reacts to update messages,
- what the two DB tables store,
- what the RabbitMQ queues carry,
- how to start and smoke-test the system.
