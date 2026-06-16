# How to Run - Energy Community Project

This guide starts the complete system from scratch and verifies that every component works.

## Prerequisites

| Tool | Minimum Version | Purpose |
|---|---|---|
| Docker Desktop | 4.x | Runs PostgreSQL and RabbitMQ |
| JDK | 25 | Builds and runs all modules. Built with Java 25 and Spring Boot 4.0.3. |
| Maven Wrapper | included | No separate Maven install needed for the Spring modules |
| JavaFX SDK | bundled through Maven | Included in the GUI `pom.xml` |

Before the final demo, make sure no old REST API process is still using port `8080`.

## System Overview

```text
energy-producer  (publisher, no HTTP port) -> RabbitMQ energy_queue -> usage-service (worker, no HTTP port)
energy-user      (publisher, no HTTP port) -> RabbitMQ energy_queue -> usage-service (worker, no HTTP port)
usage-service           -> RabbitMQ percentage_update_queue -> percentage-service (worker, no HTTP port)
usage-service           -> PostgreSQL hourly_usage
percentage-service      -> PostgreSQL current_percentage
rest-api         (8080) -> reads PostgreSQL
energy-gui              -> calls rest-api over HTTP
```

## Step 1 - Start Infrastructure

Run from the repository root:

```powershell
docker compose up -d
docker compose ps
```

Verify:

- PostgreSQL: `localhost:5432`, database `energy_community`, user `user`, password `password`
- RabbitMQ Management UI: `http://localhost:15672`, user `guest`, password `guest`

## Step 2 - Start Backend Services

Start each service in a separate terminal.

### 1. Usage Service

```powershell
cd usage-service
.\mvnw.cmd spring-boot:run
```

Wait for: `Started UsageServiceApplication`

### 2. Percentage Service

```powershell
cd percentage-service
.\mvnw.cmd spring-boot:run
```

Wait for: `Started PercentageServiceApplication`

### 3. REST API

```powershell
cd rest-api
.\mvnw.cmd spring-boot:run
```

Wait for: `Started RestApiApplication`

Verify:

```powershell
curl http://localhost:8080/energy/current
```

## Step 3 - Start Simulators

### 4. Energy Producer

```powershell
cd energy-producer
.\mvnw.cmd spring-boot:run
```

The producer fetches the current solar radiation from Open-Meteo. If the API is unavailable, `WeatherClient` returns `0` W/m² and the producer keeps publishing at the minimum kWh value, so no extra configuration is needed to run offline.

### 5. Energy User

```powershell
cd energy-user
.\mvnw.cmd spring-boot:run
```

The user simulator publishes usage messages based on time of day.

## Step 4 - Verify RabbitMQ

Open `http://localhost:15672`.

Check:

| Queue | Expected Behavior |
|---|---|
| `energy_queue` | Receives producer/user messages and should usually drain quickly. |
| `percentage_update_queue` | Receives usage update messages and should usually drain quickly. |

Topology:

```text
Energy Producer -> energy_queue -> Usage Service
Energy User     -> energy_queue -> Usage Service
Usage Service   -> percentage_update_queue -> Percentage Service
```

Direct queues are intentional because each stream has one consumer. If another service later needs to observe the same stream, introduce an exchange and per-service queues.

## Step 5 - Verify PostgreSQL

Connect with a SQL client:

```text
Host:     localhost
Port:     5432
Database: energy_community
User:     user
Password: password
```

Queries:

```sql
SELECT * FROM hourly_usage ORDER BY hour DESC LIMIT 10;
SELECT * FROM current_percentage ORDER BY hour DESC LIMIT 10;
```

`hourly_usage` is the implemented name for the conceptual `energy_usage_hourly` table. See `docs/database-schema.md`.

## Step 6 - Verify REST API

Current data:

```powershell
curl http://localhost:8080/energy/current
```

Historical data:

```powershell
curl "http://localhost:8080/energy/historical?start=2026-05-16T00:00:00&end=2026-05-16T23:59:59"
```

The historical endpoint expects ISO local datetime values (the GUI sends them in this format).

Expected error cases:

- Invalid date format -> `400 Bad Request`
- `start` after `end` -> `400 Bad Request` (`EnergyReadService` rejects an inverted range; the GUI additionally validates it before sending)

## Step 7 - Start GUI

Start after `rest-api` is running:

```powershell
cd energy-gui
..\energy-producer\mvnw.cmd -f pom.xml javafx:run
```

Verify:

- current percentages load from `/energy/current`,
- historical data loads from `/energy/historical` and is shown as aggregate labels for the selected range,
- REST errors are shown in the GUI,
- GUI contains no database credentials and no direct database connection.

## Run All Automated Builds

Run from PowerShell:

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

## Stop Everything

Stop Spring Boot/JavaFX applications with `Ctrl+C` in their terminals.

Stop infrastructure:

```powershell
docker compose down
```

Delete local PostgreSQL data volume only when a clean database reset is intended:

```powershell
docker compose down -v
```

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| Port already in use | Old Java process still running | Stop old service process or restart terminal/IDE. |
| `Connection refused` on `5432` or `5672` | Docker containers not running | Run `docker compose up -d`. |
| REST returns zeros | No percentage row yet | Start producer, user, usage, and percentage services. |
| `current_percentage` is not updating | Percentage Service not running or update queue stuck | Check `percentage_update_queue` and percentage logs. |
| GUI cannot load data | REST API not running on port `8080` | Start `rest-api` and retry. |
| Producer logs "Weather API not reachable" | Open-Meteo unavailable | Expected; `WeatherClient` returns `0` W/m² and the producer keeps working. |
