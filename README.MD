# Energy Community Project

Distributed Systems course project for simulating an energy community. The system is intentionally built from six independently startable applications that communicate through REST, RabbitMQ JSON messages, and a shared PostgreSQL database.

## Final Submission Summary

This project uses **Java 25** and **Spring Boot 4.0.3**. The project was built and tested with this stack. If another machine is used for the final demo, install Java 25 or adapt the project to Java 21 LTS before running the Maven commands.

The grading-critical architecture is:

```text
Energy Producer -> RabbitMQ -> Usage Service -> PostgreSQL
Energy User     -> RabbitMQ -> Usage Service -> PostgreSQL
Usage Service   -> RabbitMQ -> Percentage Service -> PostgreSQL
JavaFX GUI      -> REST API -> PostgreSQL
```

The GUI never connects to PostgreSQL directly. The REST API reads PostgreSQL and does not calculate or write the core business values. Producer and User services do not write to the database.

## Components

| Component | Path | Port | Start Command | Responsibility |
|---|---|---:|---|---|
| Energy Producer | `energy-producer` | 8081 | `.\mvnw.cmd spring-boot:run` | Publishes weather-dependent `PRODUCER` messages. |
| Energy User | `energy-user` | 8082 | `.\mvnw.cmd spring-boot:run` | Publishes time-of-day-dependent `USER` messages. |
| Usage Service | `usage-service` | 8083 | `.\mvnw.cmd spring-boot:run` | Consumes producer/user messages, aggregates hourly usage, writes `hourly_usage`, publishes usage updates. |
| Percentage Service | `percentage-service` | 8084 | `.\mvnw.cmd spring-boot:run` | Consumes usage updates, calculates rounded percentages, writes `current_percentage`. |
| REST API | `rest-api` | 8080 | `.\mvnw.cmd spring-boot:run` | Read-only API for current and historical energy data. |
| JavaFX GUI | `energy-gui` | n/a | `..\energy-producer\mvnw.cmd -f pom.xml javafx:run` | Desktop GUI that calls only the REST API. |

Each component has its own `pom.xml` and own application entry point. Start components in separate terminals for the final demo.

## Prerequisites

| Tool | Required | Purpose |
|---|---|---|
| JDK | Java 25 tested | Build and run all Java modules. |
| Maven Wrapper | included in service modules | Build/run without local Maven installation. |
| Docker Desktop | 4.x or newer | Runs PostgreSQL and RabbitMQ. |
| PostgreSQL | via Docker Compose | Shared persistence. |
| RabbitMQ | via Docker Compose | Asynchronous service communication. |
| JavaFX | Maven dependencies in `energy-gui` | Desktop GUI. |

## Infrastructure

Start PostgreSQL and RabbitMQ from the repository root:

```powershell
docker compose up -d
docker compose ps
```

Ports and credentials:

| Service | URL / Port | Credentials |
|---|---|---|
| PostgreSQL | `localhost:5432`, database `energy_community` | user `user`, password `password` |
| RabbitMQ AMQP | `localhost:5672` | user `guest`, password `guest` |
| RabbitMQ Management UI | `http://localhost:15672` | user `guest`, password `guest` |

## Build Commands

Run from a clean PowerShell terminal:

```powershell
cd energy-producer
.\mvnw.cmd clean package

cd ..\energy-user
.\mvnw.cmd clean package

cd ..\usage-service
.\mvnw.cmd clean package

cd ..\percentage-service
.\mvnw.cmd clean package

cd ..\rest-api
.\mvnw.cmd clean package

cd ..
.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml clean package
```

The tests are isolated. They use mocks or H2 where needed and do not require a live RabbitMQ or PostgreSQL instance.

## Start Order

Start in this order for the final demo:

```text
1. docker compose up -d
2. usage-service
3. percentage-service
4. rest-api
5. energy-producer
6. energy-user
7. energy-gui
```

Commands:

```powershell
cd usage-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd percentage-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd rest-api
.\mvnw.cmd spring-boot:run
```

```powershell
cd energy-producer
.\mvnw.cmd spring-boot:run
```

```powershell
cd energy-user
.\mvnw.cmd spring-boot:run
```

```powershell
cd energy-gui
..\energy-producer\mvnw.cmd -f pom.xml javafx:run
```

Optional producer offline/weather fallback mode:

```powershell
cd energy-producer
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.weather.mode=fallback"
```

## REST API

Required endpoints:

```http
GET http://localhost:8080/energy/current
GET http://localhost:8080/energy/historical?start=2026-05-16T00:00:00&end=2026-05-16T23:59:59
```

The historical endpoint accepts ISO local datetime and the GUI format `dd.MM.yyyy HH:mm`.

Example:

```powershell
curl http://localhost:8080/energy/current
curl "http://localhost:8080/energy/historical?start=2026-05-16T00:00:00&end=2026-05-16T23:59:59"
```

Current response fields:

```json
{
  "hour": "2026-05-16T10:00",
  "communityDepleted": 100.0,
  "gridPortion": 5.63
}
```

Historical response fields:

```json
[
  {
    "hour": "2026-05-16T10:00",
    "communityProduced": 18.05,
    "communityUsed": 18.05,
    "gridUsed": 1.076
  }
]
```

## RabbitMQ Topology

The implementation uses durable direct queues. This is sufficient for the current project because each message stream has exactly one consumer service.

```text
Energy Producer -> energy_queue -> Usage Service
Energy User     -> energy_queue -> Usage Service
Usage Service   -> percentage_update_queue -> Percentage Service
```

Why RabbitMQ is used:

- Producer/User and Usage Service are asynchronously decoupled.
- Usage Service and Percentage Service are asynchronously decoupled.
- Producer/User do not need a direct reference to Usage Service.
- Percentage Service reacts to usage-update events instead of polling the database.

If more consumers need the same stream later, the direct queues can be replaced with an exchange and per-service queues, for example a topic exchange with routing keys such as `energy.producer.created`, `energy.user.created`, and `energy.usage.updated`.

Message contracts are documented in [docs/message-contract.md](docs/message-contract.md).

## Database Schema

The database schema is managed by Flyway in the DB-backed modules:

- `usage-service/src/main/resources/db/migration/V1__create_energy_tables.sql`
- `percentage-service/src/main/resources/db/migration/V1__create_energy_tables.sql`
- `rest-api/src/main/resources/db/migration/V1__create_energy_tables.sql`

Hibernate validates the schema:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Implemented tables:

| Table | Responsibility | Writer | Readers |
|---|---|---|---|
| `hourly_usage` | Aggregated hourly energy usage. Conceptually equivalent to `energy_usage_hourly` in the project mapping. | Usage Service | Percentage Service, REST API |
| `current_percentage` | Current/latest percentage values per hour. | Percentage Service | REST API |

Detailed schema mapping is documented in [docs/database-schema.md](docs/database-schema.md).

## Business Rules

Usage Service buckets every message to the full hour:

```text
2025-01-10T14:34 -> 2025-01-10T14:00
```

For `PRODUCER` messages:

```text
communityProduced += message.kwh
```

For `USER` messages:

```text
availableCommunityEnergy = communityProduced - communityUsed
communityPart = min(message.kwh, max(availableCommunityEnergy, 0))
gridPart = message.kwh - communityPart

communityUsed += communityPart
gridUsed += gridPart
```

Invariant:

```text
communityUsed <= communityProduced
```

There is no Grid Producer and no Grid Message. Grid energy is the fallback portion when community energy is not sufficient.

Percentage Service formulas:

```text
communityDepleted = communityUsed / communityProduced * 100
gridPortion = gridUsed / (communityUsed + gridUsed) * 100
```

Division by zero returns `0`, and persisted percentage values are rounded to two decimals.

## JavaFX GUI

The GUI:

- calls `GET /energy/current` for current percentages,
- calls `GET /energy/historical?start=...&end=...` for historical data,
- contains no PostgreSQL/JPA/RabbitMQ dependency,
- shows API errors in the UI,
- displays historical values as aggregate labels for the selected range.

The REST API must be running on `http://localhost:8080` before starting the GUI.

## Final Demo Flow

For the full repeatable manual smoke test, use [docs/smoke-test.md](docs/smoke-test.md).

1. Show the Git branch and repository.
2. Show that six component folders exist and each has its own `pom.xml`.
3. Run `docker compose ps` and show PostgreSQL/RabbitMQ.
4. Start `usage-service`.
5. Start `percentage-service`.
6. Start `rest-api`.
7. Start `energy-producer` and show `PRODUCER` logs.
8. Start `energy-user` and show `USER` logs.
9. Open RabbitMQ Management UI and show `energy_queue` / `percentage_update_queue`.
10. Query PostgreSQL:
    - `SELECT * FROM hourly_usage ORDER BY hour DESC LIMIT 10;`
    - `SELECT * FROM current_percentage ORDER BY hour DESC LIMIT 10;`
11. Call REST endpoints with curl/browser.
12. Open JavaFX GUI, refresh current values, and query historical data.
13. Explain the flow: `Producer/User -> RabbitMQ -> Usage -> DB -> RabbitMQ -> Percentage -> DB -> REST -> GUI`.

## Team Contributions

Each team member must have own Git commits. The lecturer explicitly treats missing individual commits as a personal grading risk. Verify before submission:

```powershell
git shortlog -sn
```

Observed local commit authors during audit:

| Git Author | Commits Observed | Main Contribution Notes |
|---|---:|---|
| OnlyMajorG | 32 | Verify real name and contributions before submission. |
| Yijie Liu | 12 | Verify real name and contributions before submission. |
| stefangirgis | 7 | Verify real name and contributions before submission. |

Replace or extend this table with final real names, GitHub usernames, and actual contribution areas before hand-in if the submission requires personal attribution.

## Known Limitations

- The project uses Java 25 and Spring Boot 4.0.3 instead of the lecture reference versions. This is documented as an environment prerequisite.
- RabbitMQ uses direct durable queues instead of an explicit exchange/routing-key topology. This is intentional because each stream currently has one consumer.
- The implemented table `hourly_usage` is conceptually the project mapping table `energy_usage_hourly`.
- Audit timestamp columns such as `updated_at` and `calculated_at` are intentionally omitted in the current schema because the grading-relevant values are `hour`, usage totals, and percentage totals.
- Authentication, Kubernetes, cloud deployment, API gateway, and OAuth are not implemented because they are outside the project scope.
- Generated Maven `target/` files should not be tracked in Git. Run the cleanup checklist before final submission.

## Documentation

- [docs/documentation-overview.md](docs/documentation-overview.md): documentation index and QA reading path.
- Module documentation:
  - [docs/energy-producer.md](docs/energy-producer.md)
  - [docs/energy-user.md](docs/energy-user.md)
  - [docs/usage-service.md](docs/usage-service.md)
  - [docs/percentage-service.md](docs/percentage-service.md)
  - [docs/rest-api.md](docs/rest-api.md)
  - [docs/energy-gui.md](docs/energy-gui.md)
- [docs/message-contract.md](docs/message-contract.md): RabbitMQ topology and JSON contracts.
- [docs/database-schema.md](docs/database-schema.md): implemented DB schema and conceptual mapping.
- [docs/how-to-run.md](docs/how-to-run.md): detailed runbook.
- [docs/smoke-test.md](docs/smoke-test.md): full distributed smoke-test runbook for RabbitMQ, PostgreSQL, REST, and JavaFX verification.
- [docs/final-readiness-check.md](docs/final-readiness-check.md): final pre-submission checklist.
- [docs/spec-code-mapping.md](docs/spec-code-mapping.md): specification-to-code mapping.
