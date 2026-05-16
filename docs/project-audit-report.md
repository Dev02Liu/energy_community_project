# DISYS Project Audit Report

Post-audit update on branch `44-issue-81---readiness-fixes`: Usage message validation, percentage rounding, RabbitMQ topology documentation, database schema mapping, README final-demo instructions, and the final readiness checklist have been updated after this audit. Treat the detailed findings below as the original audit baseline; use `README.MD`, `docs/message-contract.md`, `docs/database-schema.md`, `docs/spec-code-mapping.md`, and `docs/final-readiness-check.md` for the current readiness state.

## 1. Executive Summary

Overall readiness: **MOSTLY READY**

The project implements the required six applications as separate startable modules and the automated builds/tests pass locally. A live smoke test with PostgreSQL and RabbitMQ also verified the core path: producer/user messages -> RabbitMQ -> usage-service -> PostgreSQL -> update message -> percentage-service -> PostgreSQL -> REST API.

Main risks:

- The implementation uses **Java 25 and Spring Boot 4.0.3**, while the recommended/course-safe stack is Java 17/21 LTS and Spring Boot 3.x. This is a portability and review-environment risk.
- RabbitMQ works, but the implementation uses direct durable queues (`energy_queue`, `percentage_update_queue`) instead of the documented topic exchange/routing-key design from the very-good mapping.
- Database names/columns differ from the stricter target schema: code uses `hourly_usage`, not `energy_usage_hourly`, and omits `updated_at` / `calculated_at`.
- JavaFX historical data is loaded via REST, but displayed as aggregate labels rather than a per-hour table.
- Default service ports were already occupied during audit; isolated startup worked with port overrides, but the final demo should start from a clean machine or document how to stop old services.

Critical 0-point risks:

- **No confirmed P0 fail found.** The mandatory architecture exists, builds locally, uses Spring Boot REST, JavaFX, RabbitMQ, and PostgreSQL.
- GitHub submission itself is an external/manual check, but the local remote and commit history are verifiable.

Recommended next actions:

1. Confirm with the lecturer whether Java 25 / Spring Boot 4 is acceptable; otherwise downgrade to Java 21 LTS and Spring Boot 3.4/3.5.
2. Add or document the expected RabbitMQ exchange/routing topology, or explicitly justify the direct-queue design.
3. Align DB schema names/columns with the official spec if exact table/column names are expected.
4. Improve GUI historical display to show per-hour rows in a JavaFX `TableView`.
5. Run one clean default-port demo before submission and record the exact commands/results.

## 2. Source Documents Used

Inspected project resources:

- `project-resources/DISYS_Semesterprojekt_Sehr_gut_Mapping.md`
- `project-resources/github-issues-until-final-handin.md`
- `project-resources/Lecture Transcripts.txt`
- `project-resources/LEcture-Materials/ProjektGrading.pdf` (present; text extraction yielded no readable text)
- `project-resources/LEcture-Materials/Projektspezifikationen.pdf` (present; text extraction yielded no readable text)
- `project-resources/Lecture-Code/disys26bwi1/...` for lecture-style Spring Boot/JPA/Flyway reference structure

Additional implementation docs inspected:

- `README.MD`
- `docs/message-contract.md`
- `docs/how-to-run.md`
- `docs/spec-code-mapping.md`
- `docs/project-status.md`

Transcript evidence used:

- Lines 751-779 describe JavaFX GUI -> REST API -> database, four RabbitMQ services, one shared DB, one RabbitMQ instance, six independent projects.
- Lines 755-769 describe weather-dependent producer messages, time-of-day user messages, Usage Service consuming both and publishing update messages, and Percentage Service consuming updates.
- Lines 873-875 emphasize independent components, Git commits, and calculation correctness as major grading risks.

## 3. Critical Must-Have Checklist

| Requirement | Status | Evidence | Risk |
|---|---|---|---|
| Every component can be started independently | PASS | Six independent Maven projects exist: `energy-producer`, `energy-user`, `usage-service`, `percentage-service`, `rest-api`, `energy-gui`; each has its own `pom.xml` and main class. | Low. Default port conflicts can occur if old services are still running. |
| System can be built with no errors | PASS | `clean package` passed for all six modules during audit. | Low locally; Java 25 requirement is a portability risk. |
| System can run with infrastructure | PASS | Docker Compose config valid; PostgreSQL and RabbitMQ were running; isolated end-to-end smoke passed. | Medium until repeated on a clean demo machine. |
| Spring Boot REST API | PASS | `rest-api/src/main/java/.../RestApiApplication.java`; `EnergyController` exposes `/energy/current` and `/energy/historical`. | Low. Controller directly uses repositories. |
| JavaFX GUI | PASS | `energy-gui` uses JavaFX `Application`, `Stage`, controls, and REST client. | Medium. Historical display is aggregate labels, not tabular rows. |
| RabbitMQ communication | PASS | Producer/user publish with `RabbitTemplate`; usage consumes with `@RabbitListener`; usage publishes update; percentage consumes update. | Medium. No exchange/routing-key topology. |
| PostgreSQL setup | PASS | `docker-compose.yml` starts PostgreSQL; DB-backed modules use PostgreSQL config and Flyway migrations. | Medium. Schema names/columns differ from target mapping. |
| GUI does not access DB directly | PASS | `energy-gui` contains JavaFX, `HttpClient`, Jackson only; no JDBC/JPA/PostgreSQL dependency found. | Low. |
| REST API reads DB and does not calculate core business values | PASS | REST maps `current_percentage` and `hourly_usage` rows to DTOs; no write endpoint or calculation logic found. | Low. Repository has write capability by type but controller does not call writes. |
| GitHub/team evidence | PASS / NOT VERIFIABLE | Remote: `https://github.com/Dev02Liu/energy_community_project.git`; `git shortlog` shows multiple contributors. Submission link in Moodle remains manual. | Low/manual. |

## 4. Component Mapping

| Expected Component | Actual Module/Path | Start Command | Status |
|---|---|---|---|
| Energy Producer | `energy-producer` | `cd energy-producer; .\mvnw.cmd spring-boot:run` | PASS |
| Energy User | `energy-user` | `cd energy-user; .\mvnw.cmd spring-boot:run` | PASS |
| Usage Service | `usage-service` | `cd usage-service; .\mvnw.cmd spring-boot:run` | PASS |
| Current Percentage Service | `percentage-service` | `cd percentage-service; .\mvnw.cmd spring-boot:run` | PASS |
| Spring Boot REST API | `rest-api` | `cd rest-api; .\mvnw.cmd spring-boot:run` | PASS |
| JavaFX GUI | `energy-gui` | `cd energy-gui; ..\energy-producer\mvnw.cmd -f pom.xml javafx:run` or Maven installed `mvn javafx:run` | PASS |
| PostgreSQL | `docker-compose.yml` service `postgres` | `docker compose up -d` | PASS |
| RabbitMQ | `docker-compose.yml` service `rabbitmq` | `docker compose up -d` | PASS |
| Shared code | `shared` only contains generated `target` artifacts | No active start command | PARTIAL / cleanup needed |

## 5. Module Audit

### Energy Producer

Purpose: sends weather-dependent production messages to RabbitMQ.

Observed implementation:

- Spring Boot application with scheduling in `EnergyProducerApplication` and `EnergyProducerScheduler`.
- Publishes local `EnergyMessage` via `RabbitTemplate.convertAndSend(queueName, msg)` in `EnergyProducerService`.
- `createProductionMessage()` sets `type=PRODUCER`, `association=COMMUNITY`, `kwh`, and `datetime`.
- Weather integration exists through `WeatherClient`, `OpenMeteoWeatherClient`, `FallbackWeatherClient`, `ResilientWeatherClient`, and `WeatherProductionCalculator`.
- Fallback behavior is implemented when Open-Meteo fails.

Requirements fulfilled:

- Independent app.
- RabbitMQ publish.
- Regular scheduled sending with random 1-5 second wait wrapped around a 1-second scheduled trigger.
- Weather affects kWh.
- No API key required for Open-Meteo.

Missing requirements / defects:

- Uses Spring WebMVC and opens a web port although no REST endpoint is needed; this caused port-conflict failures during default-port smoke when old services were already running.
- No serialization compatibility test proving producer JSON deserializes into usage-service DTO.
- kWh values are relatively large (`10.0` to `30.0`) compared to examples like `0.003`; this may be acceptable as simulation scale but should be explained.

Tests:

- 6 tests passed: context, message creation, weather calculator, fallback behavior.

Recommendations:

- Consider `spring.main.web-application-type=none` for producer/user if no HTTP endpoint is required.
- Add message contract serialization tests across producer/user/usage DTOs.
- Document the chosen kWh scale explicitly.

### Energy User

Purpose: sends time-of-day-dependent consumption messages to RabbitMQ.

Observed implementation:

- Spring Boot application with scheduler.
- `EnergyUserService#createUsageMessage()` sets `type=USER`, `association=COMMUNITY`, `kwh`, and `datetime`.
- `EnergyUsageCalculator` applies peak multipliers for 07:00-09:00 and 18:00-21:00, night multiplier for 23:00-05:00, default otherwise.

Requirements fulfilled:

- Independent app.
- RabbitMQ publish.
- Regular scheduled sending.
- Time of day influences usage.
- Message format matches required fields.

Missing requirements / defects:

- Peak windows differ slightly from the mapping text (`05:00-09:00`, `16:00-22:00`); the implemented windows are narrower.
- Like producer, opens a web port despite not needing a REST API.
- No serialization compatibility test with usage-service DTO.

Tests:

- 4 tests passed: context, usage calculator, message creation.

Recommendations:

- Align peak-hour windows with the official spec if exact hours are expected.
- Consider non-web Spring Boot mode for the simulator.

### Usage Service

Purpose: consumes producer/user messages, aggregates hourly values, writes PostgreSQL, publishes update messages.

Observed implementation:

- `EnergyMessageListener` uses `@RabbitListener(queues = "${app.queue.name}")`.
- Listener delegates to `HourlyUsageUpdateService`; business logic is not in the listener.
- `HourlyUsageUpdateService#handleEnergyMessage()` is `@Transactional`.
- Producer messages increment `communityProduced`.
- User messages calculate available community energy, split into community and grid portions, save the row, then publish `HourlyUsageUpdatedMessage`.

Requirements fulfilled:

- Independent Spring Boot application.
- Consumes producer and user messages from RabbitMQ.
- Aggregates to hour with minute/second/nano truncation.
- Preserves `community_used <= community_produced` for normal non-negative inputs.
- Writes PostgreSQL via JPA/Flyway schema.
- Publishes update message after processing.
- Unit tests cover the specified example and edge cases.

Missing requirements / defects:

- Unknown message types still cause `save()` and update publish without a data change.
- Negative `kwh` is not rejected. Malformed negative producer messages can reduce `communityProduced`; malformed negative user messages can reduce `communityUsed`.
- RabbitMQ publish happens inside the DB transaction without outbox/after-commit synchronization. For this course project this is likely acceptable, but it is not a strong transactional messaging pattern.
- No explicit optimistic/pessimistic locking on the hourly row. Default single consumer is probably fine; multiple service instances could lose updates.

Tests:

- 18 tests passed, including hourly bucketing, producer aggregation, user split, grid fallback, invariant check, spec example, null handling, and update message publish.

Recommendations:

- Reject `kwh < 0` and unknown types before saving/publishing.
- Add a short code comment or README note explaining single-consumer concurrency assumptions.
- Optionally publish update after transaction commit for stronger consistency.

### Current Percentage Service

Purpose: consumes usage-update messages, reads hourly usage, calculates percentages, writes current percentage rows.

Observed implementation:

- `HourlyUsageUpdatedListener` consumes `${app.update-queue.name}`.
- `CurrentPercentageCalculationService#updateCurrentPercentage()` is `@Transactional`.
- Reads `hourly_usage` row, computes:
  - `communityDepleted = communityUsed / produced * 100`, or 0 if produced is 0.
  - `gridPortion = gridUsed / (communityUsed + gridUsed) * 100`, or 0 if total usage is 0.
- Saves/overwrites `current_percentage` row by hour.

Requirements fulfilled:

- Independent Spring Boot app.
- Consumes update messages.
- Reads correct hourly row.
- Writes percentage table.
- Handles division by zero.
- Tests cover expected formulas and edge cases.

Missing requirements / defects:

- Values are stored as raw `double`; not rounded to two decimals in persistence. Tests assert approximate values but the DB may contain many decimal places.
- Missing update message with null hour is ignored, which is fine, but no warning is logged.

Tests:

- 12 tests passed: spec example, normal case, zero production, zero usage, full depletion, existing-row overwrite, missing row, null hour.

Recommendations:

- Round persisted percentages to two decimals if the grader expects numeric output like `5.63`.
- Log ignored update messages at warning/debug level.

### REST API

Purpose: read-only API for GUI current and historical data.

Observed implementation:

- Spring Boot `RestApiApplication`.
- `EnergyController` exposes:
  - `GET /energy/current`
  - `GET /energy/historical?start=...&end=...`
- Reads `CurrentPercentageRepository.findFirstByOrderByHourDesc()`.
- Reads `HourlyUsageRepository.findByHourBetween(start, end)`.
- Supports ISO datetime and GUI format `dd.MM.yyyy HH:mm`.
- Invalid date and `start > end` return 400 via `ResponseStatusException`.

Requirements fulfilled:

- Spring Boot REST.
- Required endpoints implemented.
- Reads PostgreSQL tables.
- Returns DTOs, not entities.
- No writes found in controller.
- Contract tests cover success and error cases.

Missing requirements / defects:

- Controller directly contains query and mapping logic; no separate query service layer.
- `/energy/current` fallback returns `LocalDateTime.now()` and zeros when no DB row exists. That is demo-friendly but may be semantically less clear than 404 or explicit empty state.
- `spring.jpa.open-in-view` warning appears; not a functional issue.

Tests:

- 25 tests passed: repository migration, latest-row behavior, MockMvc/controller contracts, invalid dates, start-after-end.

Recommendations:

- Extract a read-only query service for cleaner review explanation.
- Add `spring.jpa.open-in-view=false`.

### JavaFX GUI

Purpose: desktop UI that retrieves current/historical values via REST.

Observed implementation:

- `MainApp` launches `EnergyGuiApplication`.
- `EnergyGuiApplication` builds a JavaFX scene and creates `EnergyDashboardController`.
- `EnergyApiClient` uses Java `HttpClient` and Jackson to call REST.
- UI has refresh, start/end text fields, and show-data action.
- No DB, JPA, JDBC, PostgreSQL, or RabbitMQ imports/dependencies found.

Requirements fulfilled:

- Independent JavaFX app.
- Calls REST API over HTTP.
- Displays current community pool and grid portion.
- Supports start/end input and loads historical data.
- Handles REST errors by updating labels.

Missing requirements / defects:

- Historical data is aggregated into three labels, not displayed per hourly row. This is below the “very good” table expectation.
- REST base URL is hardcoded to `http://localhost:8080`.
- Empty historical result displays “Error fetching data”, which is misleading.

Tests:

- 21 tests passed: API client parsing/encoding/errors, date input validation, value formatting.

Recommendations:

- Add a `TableView<HistoricalUsageDTO>` with hour, produced, community used, grid used.
- Make REST base URL configurable via system property/env var.
- Display “No data for selected range” for empty historical responses.

## 6. Business Logic Audit

Usage Service calculation:

- `hour = datetime.withMinute(0).withSecond(0).withNano(0)` matches hourly aggregation.
- Producer messages add `message.kwh` to `communityProduced`.
- User messages compute available community energy as `max(communityProduced - communityUsed, 0)`.
- User community portion is `min(requestedKwh, availableCommunityEnergy)`.
- User grid portion is `max(requestedKwh - communityPortion, 0)`.
- This matches the required formula for normal non-negative messages.

Current Percentage calculation:

- `communityDepleted = communityUsed / communityProduced * 100`, with zero-production guard.
- `gridPortion = gridUsed / (communityUsed + gridUsed) * 100`, with zero-total guard.
- The spec example `18.05 / 18.05` and `1.076 / (18.05 + 1.076)` is covered by test and passes.

Edge cases:

- Covered: no production but usage, enough community energy, partial community/grid split, full depletion, multiple same-hour updates, different hour bucketing, missing percentage input row, zero denominators.
- Not covered/handled: negative `kwh`, unknown message types causing unnecessary update events, malformed association values.

Database updates:

- Usage writes `hourly_usage`.
- Percentage writes `current_percentage`.
- REST reads both.
- Producer/user do not write DB.

Message publishing:

- Usage publishes update after every valid typed or unknown-typed message path after save.
- For invalid null message/type/datetime, no publish occurs.

## 7. REST API Audit

Endpoints:

- `GET /energy/current`
- `GET /energy/historical?start=...&end=...`

Request parameters:

- `historical.start`: ISO local datetime or `dd.MM.yyyy HH:mm`.
- `historical.end`: ISO local datetime or `dd.MM.yyyy HH:mm`.

Response structures:

- Current: `hour`, `communityDepleted`, `gridPortion`.
- Historical: list of `hour`, `communityProduced`, `communityUsed`, `gridUsed`.

DB access behavior:

- `CurrentPercentageRepository` and `HourlyUsageRepository` are Spring Data JPA repositories.
- Controller reads latest current row and range-filtered usage rows.

Read-only verification:

- No REST `POST`, `PUT`, `PATCH`, or `DELETE` mappings found.
- No `save()` calls found in `EnergyController`.
- The API still has repository write methods available by type; this is normal for Spring Data but should be explained as not exposed.

## 8. GUI Audit

REST usage:

- `EnergyApiClient#fetchCurrentPercentage()` calls `/energy/current`.
- `EnergyApiClient#fetchHistoricalUsage()` calls `/energy/historical` with URL-encoded start/end values.

Displayed data:

- Current: community depleted and grid portion.
- Historical: sums of produced, community used, and grid used for the selected range.

Historical query behavior:

- Start/end fields default to today start and now.
- GUI validates strict German datetime and also accepts ISO local datetime.
- Start-after-end is blocked client-side.

Direct DB access check:

- PASS. No GUI DB dependency or direct JDBC/JPA access found.

Error handling:

- REST non-2xx becomes `EnergyApiException`.
- UI labels show error messages.
- Improvement needed for empty historical results.

## 9. RabbitMQ Audit

Exchanges:

- No explicit exchange configured.

Queues:

- `energy_queue`: producer/user publish; usage-service consumes.
- `percentage_update_queue`: usage-service publishes; percentage-service consumes.
- Both queues are declared durable in configuration.

Routing keys:

- No routing keys. Direct queue publishing is used via `RabbitTemplate.convertAndSend(queueName, msg)`.

Message contracts:

- Energy message fields: `type`, `association`, `kwh`, `datetime`.
- Update message field: `hour`.
- Contracts documented in `docs/message-contract.md`.

Producers:

- `energy-producer` publishes `PRODUCER`.
- `energy-user` publishes `USER`.
- `usage-service` publishes usage update.

Consumers:

- `usage-service` consumes energy messages.
- `percentage-service` consumes update messages.

Runtime smoke:

- Isolated audit queues drained to zero.
- Usage and percentage logs showed DB updates.
- REST API on isolated port returned current/historical data from PostgreSQL.

Risks:

- The direct-queue design is simpler than the recommended `energy.events.topic` exchange with routing keys. It works for one consumer per stream, but if the official spec requires exchange/routing keys literally, this is a grading risk.

## 10. Database Audit

Schema:

- Flyway migration creates:
  - `hourly_usage(hour, community_produced, community_used, grid_used)`
  - `current_percentage(hour, community_depleted, grid_portion)`

Migrations:

- Present in `usage-service`, `percentage-service`, and `rest-api`.
- `spring.jpa.hibernate.ddl-auto=validate` is used.

Entities:

- Usage service maps `HourlyUsageEntity` to `hourly_usage`.
- Percentage service maps `HourlyUsageEntity` and `CurrentPercentageEntity`.
- REST API maps read-side entities for both tables.

Repositories:

- Usage: `HourlyUsageRepository`.
- Percentage: `HourlyUsageRepository`, `CurrentPercentageRepository`.
- REST: `HourlyUsageRepository`, `CurrentPercentageRepository`.

Table responsibilities:

- `hourly_usage`: written by Usage Service, read by Percentage Service and REST API.
- `current_percentage`: written by Percentage Service, read by REST API.

Risks:

- Target mapping uses `energy_usage_hourly` with `updated_at` and `current_percentage.calculated_at`. This implementation uses `hourly_usage` and omits audit timestamp columns.
- Numeric types are `DOUBLE PRECISION`, not `NUMERIC(12,6)` / `NUMERIC(8,2)`. This is acceptable for a small demo but less exact than the target schema.

## 11. Build and Test Results

Commands run:

| Command | Result |
|---|---|
| `cd energy-producer; .\mvnw.cmd clean package` | PASS, 6 tests, build success |
| `cd energy-user; .\mvnw.cmd clean package` | PASS, 4 tests, build success |
| `cd usage-service; .\mvnw.cmd clean package` | PASS, 18 tests, build success |
| `cd percentage-service; .\mvnw.cmd clean package` | PASS, 12 tests, build success |
| `cd rest-api; .\mvnw.cmd clean package` | PASS, 25 tests, build success |
| `.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml clean package` | PASS, 21 tests, build success |
| `docker compose config` | PASS, config valid |
| `docker compose ps` | PASS, `energy-db` and `energy-mq` running |

Total automated tests observed: **86 passed, 0 failed**.

Warnings observed:

- Mockito dynamic agent warning on Java 25.
- Flyway warns H2 2.4 is newer than verified version in tests.
- Hibernate dialect deprecation warnings.
- REST API warning: `spring.jpa.open-in-view` enabled.
- Initial GUI command with `..\mvnw.cmd` failed because root has no `mvnw.cmd`; using an existing module wrapper with `-f energy-gui/pom.xml` worked.

## 12. End-to-End Test Result

Executed:

1. Verified Docker Compose infrastructure was running:
   - `energy-db` on `5432`
   - `energy-mq` on `5672` and management UI on `15672`
2. Started built JARs for:
   - `usage-service`
   - `percentage-service`
   - `rest-api`
   - `energy-producer`
   - `energy-user`
3. First attempt showed default ports `8080`, `8081`, and `8082` already occupied by existing processes.
4. Repeated with isolated audit queues and alternate/random ports:
   - Usage and percentage started with audit queue names.
   - REST API started on `18080`.
   - Producer/user started with random ports and audit queue name.
5. Observed producer/user logs sending messages.
6. Observed usage-service and percentage-service Hibernate update statements.
7. RabbitMQ audit queues had `messages=0`, indicating messages were consumed.
8. REST API returned data:
   - `/energy/current`: current hour and percentage values.
   - `/energy/historical`: current-day hourly usage row.

Result: **PASS with caveat**. The isolated runtime flow works, but the default-port demo environment had pre-existing Java services. Before final submission, repeat from a clean process list.

## 13. Prioritized Fix List

### P0 - Critical, possible 0-point issue

No confirmed P0 issue found.

Manual P0 checks still required:

- `GitHub repository link` submitted in Moodle.
- Every team member can explain their commits/code in the review.
- Repeat clean default-port demo before presentation.

### P1 - High grading risk

Affected path: all `pom.xml`

Problem: Project uses Java 25 and Spring Boot 4.0.3, while the course-safe stack is Java 17/21 LTS and Spring Boot 3.x.

Recommended change: Confirm acceptability with lecturer, or downgrade to Java 21 LTS and Spring Boot 3.x.

Reason: Build may fail on grading machines or diverge from lecture examples.

Affected path: `usage-service/src/main/resources/db/migration/V1__create_energy_tables.sql`, `percentage-service/...`, `rest-api/...`

Problem: Schema uses `hourly_usage` and lacks `updated_at` / `calculated_at`; target mapping expects `energy_usage_hourly` and timestamp audit columns.

Recommended change: Align schema names/columns with official spec if exact names are required; otherwise document equivalence prominently.

Reason: DB schema mismatch can create review friction and possible grading deductions.

Affected path: RabbitMQ config in `usage-service`, `percentage-service`, producer/user services, `docs/message-contract.md`

Problem: Direct queues are used instead of topic exchange/routing keys.

Recommended change: Either implement `energy.events.topic` with routing keys or document direct-queue design as a deliberate simplification and confirm it is accepted.

Reason: The very-good mapping expects exchange/routing-key terminology and routing.

Affected path: `energy-gui/src/main/java/com/energy_community_project/gui/controller/EnergyDashboardController.java`

Problem: Historical data is aggregated into labels, not shown per hour.

Recommended change: Add `TableView` for hourly historical rows.

Reason: The GUI requirement expects historical data to be dynamically visible; table display is stronger for review.

### P2 - Quality improvement

Affected path: `usage-service/src/main/java/.../HourlyUsageUpdateService.java`

Problem: Unknown message types still save and publish updates; negative kWh is not rejected.

Recommended change: Validate `type`, `association`, and `kwh >= 0` before processing.

Reason: Protects business invariant against malformed messages.

Affected path: `percentage-service/src/main/java/.../CurrentPercentageCalculationService.java`

Problem: Percentages are stored as raw doubles without two-decimal rounding.

Recommended change: Round persisted values to two decimals or use `BigDecimal`.

Reason: Spec examples and UI expectations use consistent two-decimal percentages.

Affected path: `energy-producer`, `energy-user`

Problem: Simulator apps open web ports without exposing endpoints.

Recommended change: Set `spring.main.web-application-type=none`, or keep ports but document why.

Reason: Avoids demo port conflicts.

Affected path: `rest-api/src/main/java/.../EnergyController.java`

Problem: Controller directly queries repositories and maps DTOs.

Recommended change: Extract read-only query service.

Reason: Cleaner architecture and easier code-review explanation.

Affected path: `energy-gui/src/main/java/.../EnergyGuiApplication.java`

Problem: REST base URL is hardcoded.

Recommended change: Read base URL from system property or env var with localhost default.

Reason: Easier demo if API port changes.

Affected path: `energy-gui/src/main/java/.../EnergyDashboardController.java`

Problem: Empty historical result shows “Error fetching data”.

Recommended change: Show “No data for selected range”.

Reason: Better demo behavior.

Affected path: repository root / `shared`

Problem: `shared` contains generated `target` artifacts and no active source module; `git ls-files` shows tracked `energy-gui/target` artifacts.

Recommended change: Remove tracked build artifacts from Git index in a dedicated cleanup commit.

Reason: Keeps submission clean and avoids generated-file diffs.

### P3 - Optional improvement

Affected path: all messaging DTO modules

Problem: No cross-service JSON serialization compatibility test.

Recommended change: Add tests using JSON samples from `docs/message-contract.md`.

Reason: Protects local DTO compatibility without reintroducing shared compile-time coupling.

Affected path: `docs/`

Problem: Architecture diagram exists in prose/docs but could be stronger as a renderable Mermaid component diagram.

Recommended change: Add `docs/architecture.md` with component and sequence diagrams.

Reason: Helps presentation and review.

## 14. Final Recommendation

The project is **not in a 0-point danger state** based on this audit. It is functionally close to ready and the most important grading components, Usage Service and Current Percentage Service, are implemented and tested well.

For a “Sehr gut” submission, address or explicitly document the P1 risks before hand-in, especially Java/Spring version compatibility, RabbitMQ topology, DB schema naming/columns, and GUI historical display. After those are settled, run a clean final demo from stopped Java processes and record the evidence.
