# How to Run – Energy Community Project

This guide walks through starting the complete system from scratch and verifying that every component works.

## Prerequisites

| Tool | Minimum Version | Purpose |
|---|---|---|
| Docker Desktop | 4.x | Runs PostgreSQL and RabbitMQ |
| JDK | 25 | Builds and runs all Spring Boot services |
| Maven Wrapper | included | No separate Maven install needed |
| JavaFX SDK | bundled via Maven | Included in the GUI pom.xml |

---

## System Overview

```
energy-producer  (port 8081)  →─┐
                                  ├── RabbitMQ: energy_queue ──→ usage-service (8083)
energy-user      (port 8082)  →─┘                                     │
                                                                        │ RabbitMQ: percentage_update_queue
                                                                        ↓
                                                              percentage-service (8084)
                                                                        │
                                                                   PostgreSQL
                                                                        │
                                                              rest-api (8080) ←── energy-gui (JavaFX)
```

---

## Step 1 – Start the Infrastructure

Open a terminal in the project root and run:

```powershell
docker compose up -d
```

Expected output:
```
Container energy-db  Started
Container energy-mq  Started
```

**Verify:**
- PostgreSQL: `localhost:5432`, database `energy_community`, user `user`, password `password`
- RabbitMQ Management UI: http://localhost:15672 → login `guest` / `guest`

---

## Step 2 – Start the Backend Services

Each service has its own `mvnw.cmd`. Start them in **separate terminals** in this order:

### 1. usage-service (port 8083)
```powershell
cd usage-service
.\mvnw.cmd spring-boot:run
```
Wait for: `Started UsageServiceApplication`

### 2. percentage-service (port 8084)
```powershell
cd percentage-service
.\mvnw.cmd spring-boot:run
```
Wait for: `Started PercentageServiceApplication`

### 3. rest-api (port 8080)
```powershell
cd rest-api
.\mvnw.cmd spring-boot:run
```
Wait for: `Started RestApiApplication`

**Verify REST API is up:**
```powershell
curl http://localhost:8080/energy/current
```
Expected response (no data yet):
```json
{"hour":"...","communityDepleted":0.0,"gridPortion":0.0}
```

---

## Step 3 – Start the Simulators

### 4. energy-producer (port 8081)
```powershell
cd energy-producer
.\mvnw.cmd spring-boot:run
```
Wait for: `Started EnergyProducerApplication`

The producer fetches real weather data from Open-Meteo (Vienna, 48.2082°N / 16.3738°E) and publishes a production message every ~1–5 seconds to `energy_queue`.

### 5. energy-user (port 8082)
```powershell
cd energy-user
.\mvnw.cmd spring-boot:run
```
Wait for: `Started EnergyUserApplication`

The user simulator publishes usage messages every ~1–5 seconds based on the current time of day (peak hours 7–10, 18–22 use 3× base load).

---

## Step 4 – Verify Data is Flowing

After ~10 seconds, call the current-percentage endpoint:

```powershell
curl http://localhost:8080/energy/current
```

Expected response with real data:
```json
{
  "hour": "2026-05-15T21:00",
  "communityDepleted": 93.55,
  "gridPortion": 24.52
}
```

- `communityDepleted` – percentage of community-produced energy that has been consumed
- `gridPortion` – percentage of total consumption that came from the grid

---

## Step 5 – Query Historical Data

The historical endpoint accepts ISO format or German date format (`dd.MM.yyyy HH:mm`):

```powershell
# ISO format
curl "http://localhost:8080/energy/historical?start=2026-05-15T00:00:00&end=2026-05-15T23:00:00"

# German format
curl "http://localhost:8080/energy/historical?start=15.05.2026+00%3A00&end=15.05.2026+23%3A00"
```

Example response:
```json
[
  {
    "hour": "2026-05-15T21:00",
    "communityProduced": 176.0,
    "communityUsed": 158.94,
    "gridUsed": 36.78
  }
]
```

**Error cases:**
- Invalid date format → `400 Bad Request`
- `start` after `end` → `400 Bad Request`

---

## Step 6 – Start the GUI

```powershell
cd energy-gui
.\mvnw.cmd javafx:run
```

The GUI window opens and immediately loads the current percentage data. Use the date range fields to load historical kWh data.

> The GUI connects to `http://localhost:8080` — the rest-api must be running first.

---

## Verify via RabbitMQ Management UI

Open http://localhost:15672 (login: `guest` / `guest`).

| What to check | Where |
|---|---|
| Queues exist | Queues → `energy_queue`, `percentage_update_queue` |
| Messages flowing | Queues → click a queue → Message rates graph shows activity |
| No messages stuck | `Messages Ready` should stay near 0 (consumed immediately) |

---

## Verify via PostgreSQL

Connect with any SQL client (e.g. DBeaver):

```
Host:     localhost
Port:     5432
Database: energy_community
User:     user
Password: password
```

Useful queries:
```sql
-- Latest hourly usage rows
SELECT * FROM hourly_usage ORDER BY hour DESC LIMIT 10;

-- Current percentage rows
SELECT * FROM current_percentage ORDER BY hour DESC LIMIT 5;
```

---

## Stop Everything

```powershell
# Stop the infrastructure
docker compose down

# The Spring Boot services can be stopped with Ctrl+C in each terminal
```

To also delete the PostgreSQL data volume:
```powershell
docker compose down -v
```

---

## Run All Automated Tests (without Docker)

Each module has isolated tests that use H2 in-memory and do not need RabbitMQ or PostgreSQL:

```powershell
cd energy-producer    && .\mvnw.cmd test   #  6 tests
cd energy-user        && .\mvnw.cmd test   #  4 tests
cd usage-service      && .\mvnw.cmd test   # 18 tests
cd percentage-service && .\mvnw.cmd test   # 12 tests
cd rest-api           && .\mvnw.cmd test   # 25 tests
```

All **65 tests** pass without any running infrastructure.

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `Connection refused` on port 5432 or 5672 | Docker containers not started | Run `docker compose up -d` |
| `usage-service` fails to start | PostgreSQL not ready yet | Wait 5 seconds and retry |
| `GET /current` always returns zeros | Simulators not running | Start energy-producer and energy-user |
| `communityDepleted` stays at 0 | percentage-service not running | Start percentage-service |
| GUI shows no data | rest-api not running or wrong port | Confirm rest-api is on port 8080 |
| Producer uses fallback weather | Open-Meteo API unreachable | Expected behavior — fallback simulates daylight/night correctly |
