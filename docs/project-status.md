# Project Status

Checked on 2026-05-15 after implementing deterministic producer/user scheduling and test behavior.

## Current Overall State

The repository contains the six required applications from the project specification plus Docker-based infrastructure:

- `energy-producer`
- `energy-user`
- `usage-service`
- `percentage-service`
- `rest-api`
- `energy-gui`
- PostgreSQL and RabbitMQ via `docker-compose.yml`

The architecture now follows the lecture feedback more closely:

- The GUI is split into entry point, controller, REST client, DTOs, and formatting service.
- Services do not use the former `shared` Maven module as a compile-time DTO dependency.
- RabbitMQ payloads are service-local Java DTOs with a central JSON contract in `docs/message-contract.md`.
- `energy-producer` now uses a weather abstraction instead of a raw random weather factor.
- `energy-producer` and `energy-user` now separate scheduled publishing from deterministic message creation.
- Producer/user tests disable scheduled publishing with `app.scheduling.enabled=false`, so no RabbitMQ broker is required.

## Grading Mapping

| Area | Weight | Status | Evidence | Remaining Work |
|---|---:|---|---|---|
| JavaFX UI | 10% | Implemented | `energy-gui` uses JavaFX and calls only REST through `EnergyApiClient`. | Add GUI client tests and possibly stronger date controls. |
| REST API | 10% | Implemented | `GET /energy/current` and `GET /energy/historical` read PostgreSQL-backed repositories. | Add MockMvc tests for success and invalid input cases. |
| Energy Producer | 10% | Implemented with smoke-test gap | Weather abstraction, Open-Meteo client, fallback client, production calculator, scheduler gating, and unit tests exist. | Run manual RabbitMQ smoke with live service. |
| Energy User | 10% | Implemented with smoke-test gap | Publishes `USER` messages, varies usage by time of day, and has fixed-input tests. | Run manual RabbitMQ smoke with live service. |
| Usage Service | 30% | Implemented with test gap | Consumes `EnergyMessage`, updates `hourly_usage`, applies community energy before grid, emits update message. | Add focused calculation tests and Flyway migration. |
| Percentage Service | 30% | Implemented with test gap | Consumes update message and writes `current_percentage` from `hourly_usage`. | Add focused formula tests and clarify current/latest table semantics. |

## Implemented Critical Points

### Weather-Based Producer

Implemented in `energy-producer`:

- `WeatherClient` interface
- `OpenMeteoWeatherClient`
- `FallbackWeatherClient`
- `ResilientWeatherClient`
- `WeatherSnapshot`
- `WeatherProductionCalculator`
- deterministic producer message creation through `EnergyProducerService#createProductionMessage`

The producer still publishes this RabbitMQ payload shape:

```json
{
  "type": "PRODUCER",
  "association": "COMMUNITY",
  "kwh": 18.7,
  "datetime": "2026-05-15T14:33:00"
}
```

Automated producer verification:

```powershell
cd energy-producer
.\mvnw.cmd test
```

Latest result: 6 tests run, 0 failures, 0 errors.

### GUI Clean-Code Feedback

Implemented in `energy-gui`:

- `MainApp` is no longer the full UI/HTTP/DTO/formatting implementation.
- JavaFX event handling is separated from HTTP code.
- HTTP calls, URL encoding, JSON parsing, and API errors live in `EnergyApiClient`.
- DTOs are GUI-local.
- Formatting is isolated in `EnergyValueFormatter`.

### Removed Shared Compile-Time Coupling

Implemented:

- Services use local message DTOs.
- RabbitMQ contracts are documented in `docs/message-contract.md`.
- The former `shared` module is not an active dependency for service messaging.

Guardrail:

- Do not reintroduce a shared DTO Maven module for service-to-service communication.
- If a JSON field changes, update `docs/message-contract.md` first, then update the local DTOs in the affected services.

### Deterministic Producer/User Tests

Implemented in `energy-producer`:

- scheduled publishing moved to `EnergyProducerScheduler`
- random 1-5 second delay moved to `SimulationDelayProvider`
- `EnergyProducerService#createProductionMessage` remains testable without RabbitMQ
- test config disables scheduled publishing through `app.scheduling.enabled=false`

Implemented in `energy-user`:

- scheduled publishing moved to `EnergyUserScheduler`
- random 1-5 second delay moved to `SimulationDelayProvider`
- usage variation moved to `UsageVariationProvider`
- time-of-day calculation moved to `EnergyUsageCalculator`
- `EnergyUserService#createUsageMessage` is testable with a fixed `Clock`
- test config disables scheduled publishing through `app.scheduling.enabled=false`

Automated verification:

```powershell
cd energy-producer
.\mvnw.cmd test

cd energy-user
.\mvnw.cmd test
```

Latest result:

- `energy-producer`: 6 tests run, 0 failures, 0 errors.
- `energy-user`: 4 tests run, 0 failures, 0 errors.
- No RabbitMQ `Connection refused` stack trace occurred in either module.

### Automated Project Test Run

Executed without starting RabbitMQ or PostgreSQL:

| Module | Command | Result | Notes |
|---|---|---|---|
| `energy-producer` | `.\mvnw.cmd test` | Build success, 6 tests | Scheduling disabled in test config. |
| `energy-user` | `.\mvnw.cmd test` | Build success, 4 tests | Scheduling disabled in test config. |
| `usage-service` | `.\mvnw.cmd test` | Build success, 1 context test | Uses H2 test database. |
| `percentage-service` | `.\mvnw.cmd test` | Build success, 1 context test | Uses H2 test database. |
| `rest-api` | `.\mvnw.cmd test` | Build success, 1 context test | Uses H2 test database. |
| `energy-gui` | `.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml test` | Build success | Compiles; no GUI tests exist yet. |

The first GUI command attempt with plain `mvn test` failed because `mvn` is not on the local shell `PATH`. The module itself compiled successfully when run through a Maven Wrapper from another module.

## Remaining High-Priority Work

### 1. Add Flyway Migrations

Current state:

- Runtime services still use `spring.jpa.hibernate.ddl-auto=update`.

Required next step:

- Add versioned migrations for:
  - `hourly_usage`
  - `current_percentage`
- Switch runtime schema handling to `validate` or `none`.

Why it matters:

- The lecture code uses versioned database migrations.
- Final grading expects a clean, explainable persistence setup.

### 2. Add Usage Service Behavior Tests

Current state:

- Core behavior exists, but tests are mainly startup-level.

Required next step:

- Test hourly bucketing.
- Test producer aggregation.
- Test user consumption against available community production.
- Test grid fallback.
- Cover the specification example.

### 3. Add Percentage Service Behavior Tests

Current state:

- Calculation behavior exists, but edge cases are not covered.

Required next step:

- Test normal percentage calculation.
- Test zero production.
- Test zero total usage.
- Test full community depletion.
- Test the specification example.

### 4. Add REST API Contract Tests

Current state:

- Endpoints exist and read repositories.

Required next step:

- Add MockMvc tests for:
  - `/energy/current`
  - `/energy/historical` with ISO input
  - `/energy/historical` with GUI date format
  - invalid date format
  - `start > end`

### 5. Clarify `current_percentage` Semantics

Current state:

- `percentage-service` stores one row per hour.
- `rest-api` returns the latest row.

Decision still needed:

- Keep one row per hour and document "latest row" semantics, or
- store exactly one current row and overwrite it.

### 6. Full Distributed Smoke Test

Current state:

- All modules compile or test successfully in the automated no-infrastructure check.
- A full Docker + all services + GUI smoke test was not executed in this implementation pass.

Required next step:

- Start PostgreSQL and RabbitMQ.
- Start all services.
- Confirm RabbitMQ message flow.
- Confirm PostgreSQL rows.
- Confirm REST JSON.
- Confirm GUI display.

## Verification Commands

Producer:

```powershell
cd energy-producer
.\mvnw.cmd test
```

Search for forbidden shared DTO coupling:

```powershell
rg "com.energy_community_project.shared|:shared|<artifactId>shared</artifactId>" -n -g pom.xml -g *.java
```

Run infrastructure:

```powershell
docker-compose up -d
```

Manual REST checks after the system has produced data:

```powershell
Invoke-RestMethod http://localhost:8080/energy/current
Invoke-RestMethod "http://localhost:8080/energy/historical?start=2026-05-15T00:00:00&end=2026-05-15T23:00:00"
```
