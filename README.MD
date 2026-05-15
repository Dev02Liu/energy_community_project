# Energy Community Project

Distributed Systems course project for simulating an energy community. The system is built as independently startable components that communicate through explicit REST and RabbitMQ JSON contracts.

## Architecture

```text
energy-producer -> RabbitMQ -> usage-service -> PostgreSQL
energy-user     -> RabbitMQ -> usage-service -> PostgreSQL
usage-service   -> RabbitMQ -> percentage-service -> PostgreSQL
energy-gui      -> REST API -> PostgreSQL
```

The JavaFX GUI only talks to the REST API. It does not access RabbitMQ or PostgreSQL directly. Services do not depend on a shared Java DTO module; RabbitMQ payloads are documented in [docs/message-contract.md](docs/message-contract.md).

## Tech Stack

- Java 25
- Spring Boot 4
- Spring AMQP with RabbitMQ
- Spring Web MVC for REST
- Spring Data JPA with PostgreSQL
- JavaFX 25 for the desktop GUI
- Jackson for JSON serialization
- Maven / Maven Wrapper
- Docker Compose for PostgreSQL and RabbitMQ
- JUnit and Mockito for tests

## Components

| Component | Port | Responsibility |
|---|---:|---|
| `energy-producer` | `8081` | Publishes weather-dependent `PRODUCER` messages to RabbitMQ. |
| `energy-user` | `8082` | Publishes time-of-day-dependent `USER` messages to RabbitMQ. |
| `usage-service` | `8083` | Consumes producer/user messages, aggregates hourly usage, writes `hourly_usage`, and emits update messages. |
| `percentage-service` | `8084` | Consumes usage update messages, calculates current percentages, and writes `current_percentage`. |
| `rest-api` | `8080` | Reads PostgreSQL and exposes the GUI-facing REST endpoints. |
| `energy-gui` | n/a | JavaFX dashboard for current percentages and historical usage data. |

## Infrastructure

Start PostgreSQL and RabbitMQ from the repository root:

```powershell
docker-compose up -d
```

Ports:

- PostgreSQL: `localhost:5432`
- RabbitMQ Java clients: `localhost:5672`
- RabbitMQ Management UI: `http://localhost:15672` with `guest` / `guest`

## Weather-Based Production

`energy-producer` uses a small weather abstraction:

- `WeatherClient` is the interface used by the production service.
- `OpenMeteoWeatherClient` reads cloud cover, daylight, and shortwave radiation from Open-Meteo.
- `FallbackWeatherClient` provides deterministic local weather values for offline demos and tests.
- `ResilientWeatherClient` uses Open-Meteo by default and falls back automatically if the API is unavailable.
- `WeatherProductionCalculator` maps the weather snapshot to a plausible kWh value.

Relevant configuration in `energy-producer/src/main/resources/application.properties`:

```properties
app.production.min-kwh=10.0
app.production.max-kwh=30.0
app.weather.mode=open-meteo
app.weather.latitude=48.2082
app.weather.longitude=16.3738
app.weather.open-meteo.base-url=https://api.open-meteo.com/v1/forecast
```

## Deterministic Simulation And Scheduling

Producer and user message creation are testable without RabbitMQ:

- `energy-producer` separates message creation from scheduled publishing.
- `energy-user` separates usage calculation, random variation, clock access, and scheduled publishing.
- Both modules use `app.scheduling.enabled` to enable or disable scheduled runtime publishing.
- Tests set `app.scheduling.enabled=false`, so Spring context tests do not attempt RabbitMQ publishing when no broker is running.

Runtime scheduling remains enabled by default:

```properties
app.scheduling.enabled=true
app.scheduling.fixed-delay-ms=1000
```

The scheduler still waits a random 1-5 seconds before publishing each producer/user event.

For offline mode:

```powershell
cd energy-producer
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.weather.mode=fallback"
```

## REST API

`rest-api` exposes the required GUI endpoints:

```http
GET http://localhost:8080/energy/current
GET http://localhost:8080/energy/historical?start=2026-05-15T00:00:00&end=2026-05-15T23:00:00
```

The historical endpoint also accepts the GUI format `dd.MM.yyyy HH:mm`, for example:

```http
GET http://localhost:8080/energy/historical?start=15.05.2026%2000:00&end=15.05.2026%2023:00
```

## Running The System

Start the infrastructure first:

```powershell
docker-compose up -d
```

Then start the services in separate terminals:

```powershell
cd usage-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd percentage-service
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
cd rest-api
.\mvnw.cmd spring-boot:run
```

Start the GUI after `rest-api` is running:

```powershell
cd energy-gui
mvn compile javafx:run
```

On Windows, the helper script can start the Spring services:

```powershell
.\start-services.ps1
```

## Testing

Run tests module by module:

```powershell
cd energy-producer
.\mvnw.cmd test

cd ..\energy-user
.\mvnw.cmd test

cd ..\usage-service
.\mvnw.cmd test

cd ..\percentage-service
.\mvnw.cmd test

cd ..\rest-api
.\mvnw.cmd test

cd ..
.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml test
```

The current producer test suite verifies:

- weather-to-kWh mapping with fixed weather input
- fallback behavior when the external weather client fails
- generated producer messages contain `type`, `association`, `kwh`, and `datetime`
- tests do not call the public weather API

The current user test suite verifies:

- morning and evening peak usage are higher than night/daytime usage for fixed inputs
- generated user messages contain `type`, `association`, `kwh`, and `datetime`
- tests do not require RabbitMQ because scheduled publishing is disabled

Latest automated project check:

- `energy-producer`: 6 tests, build success
- `energy-user`: 4 tests, build success
- `usage-service`: 1 context test, build success with H2
- `percentage-service`: 1 context test, build success with H2
- `rest-api`: 1 context test, build success with H2
- `energy-gui`: compiles successfully; no GUI tests exist yet

No local RabbitMQ or PostgreSQL instance is required for these automated checks.

Recommended final verification:

1. Start Docker infrastructure.
2. Start all Spring services.
3. Confirm `energy_queue` and `percentage_update_queue` activity in RabbitMQ Management UI.
4. Confirm rows in PostgreSQL tables `hourly_usage` and `current_percentage`.
5. Call `/energy/current` and `/energy/historical`.
6. Start the JavaFX GUI and verify current and historical values are displayed.

## Documentation

- [docs/message-contract.md](docs/message-contract.md): RabbitMQ JSON contracts and guardrails.
- [docs/spec-code-mapping.md](docs/spec-code-mapping.md): detailed specification-to-code mapping.
- [docs/project-status.md](docs/project-status.md): current implementation status, completed items, and remaining work.

## Known Remaining Work

- Add Flyway migrations and move runtime schema management away from `spring.jpa.hibernate.ddl-auto=update`.
- Add focused behavior tests for `usage-service`, `percentage-service`, `rest-api`, and `energy-gui`.
- Clarify whether `current_percentage` should keep one row per hour or exactly one current row.
- Run and document a full distributed smoke test before final hand-in.
- Remove any already tracked Maven `target/` artifacts from the Git index in a dedicated repository hygiene change.
