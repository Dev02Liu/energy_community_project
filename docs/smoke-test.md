# Distributed Smoke Test Runbook

Use this runbook before the final hand-in and before the live presentation. It proves the complete distributed flow:

```text
Energy Producer -> RabbitMQ -> Usage Service -> PostgreSQL
Energy User     -> RabbitMQ -> Usage Service -> PostgreSQL
Usage Service   -> RabbitMQ -> Percentage Service -> PostgreSQL
REST API        -> PostgreSQL
JavaFX GUI      -> REST API
```

The goal is not to test internals. The goal is to show that all six independently startable components work together through RabbitMQ, PostgreSQL, REST, and JavaFX.

## 1. Prerequisites

Required on the demo machine:

- Windows PowerShell.
- Docker Desktop running.
- Java 25 installed and on `PATH`.
- Repository checked out locally.
- Ports free: `5432`, `5672`, `15672`, `8080`, `8081`, `8082`, `8083`, `8084`.
- No old Java service processes running.

Check ports:

```powershell
netstat -ano | findstr ":8080 :8081 :8082 :8083 :8084 :5432 :5672 :15672"
```

If old Java service processes are still running, close the related terminals or stop them before starting.

## 2. Optional Fresh Database Reset

Use this before the final demo if you want a clean database.

Warning: this deletes the Docker PostgreSQL volume for this project.

```powershell
docker compose down -v
docker compose up -d
docker compose ps
```

For a non-destructive run, use only:

```powershell
docker compose up -d
docker compose ps
```

Expected containers:

```text
energy-db   Up   5432
energy-mq   Up   5672, 15672
```

## 3. Build All Components

From the repository root:

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

Expected result: all modules end with `BUILD SUCCESS`.

## 4. Start Order

Open one PowerShell terminal per component. Start in this order.

### Terminal 1 - Usage Service

```powershell
cd C:\dev\energy_community_project\usage-service
.\mvnw.cmd spring-boot:run
```

Expected:

- Spring Boot starts on port `8083`.
- Listener waits on `energy_queue`.

### Terminal 2 - Percentage Service

```powershell
cd C:\dev\energy_community_project\percentage-service
.\mvnw.cmd spring-boot:run
```

Expected:

- Spring Boot starts on port `8084`.
- Listener waits on `percentage_update_queue`.

### Terminal 3 - REST API

```powershell
cd C:\dev\energy_community_project\rest-api
.\mvnw.cmd spring-boot:run
```

Expected:

- Spring Boot starts on port `8080`.
- API is ready for `GET /energy/current` and `GET /energy/historical`.

### Terminal 4 - Energy Producer

Normal weather mode:

```powershell
cd C:\dev\energy_community_project\energy-producer
.\mvnw.cmd spring-boot:run
```

Offline/fallback mode for a deterministic demo without relying on Open-Meteo availability:

```powershell
cd C:\dev\energy_community_project\energy-producer
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.weather.mode=fallback"
```

Expected:

- Spring Boot starts on port `8081`.
- Logs show `PRODUCER` messages being sent.

### Terminal 5 - Energy User

```powershell
cd C:\dev\energy_community_project\energy-user
.\mvnw.cmd spring-boot:run
```

Expected:

- Spring Boot starts on port `8082`.
- Logs show `USER` messages being sent.

### Terminal 6 - JavaFX GUI

Start the GUI after the REST API is running:

```powershell
cd C:\dev\energy_community_project\energy-gui
..\energy-producer\mvnw.cmd -f pom.xml javafx:run
```

Expected:

- JavaFX window opens.
- GUI does not ask for database credentials.
- GUI can refresh current values and query historical values through REST.

## 5. RabbitMQ Verification

Open RabbitMQ Management UI:

```text
http://localhost:15672
```

Credentials:

```text
guest / guest
```

Go to **Queues and Streams** and verify:

| Queue | Expected |
|---|---|
| `energy_queue` | Exists, has `1` consumer from Usage Service, messages should drain quickly |
| `percentage_update_queue` | Exists, has `1` consumer from Percentage Service, messages should drain quickly |

PowerShell API check:

```powershell
curl.exe -u guest:guest http://localhost:15672/api/queues/%2f
```

Expected:

- `energy_queue` appears.
- `percentage_update_queue` appears.
- `messages` and `messages_unacknowledged` should normally be `0` or quickly returning to `0`.
- `consumers` should be `1` for both queues while Usage and Percentage are running.

## 6. Database Verification

Run these commands from the repository root or any PowerShell terminal.

Check Flyway and tables:

```powershell
docker exec energy-db psql -U user -d energy_community -c "\dt"
```

Expected tables include:

```text
flyway_schema_history
hourly_usage
current_percentage
```

Check hourly usage rows:

```powershell
docker exec energy-db psql -U user -d energy_community -c "SELECT hour, community_produced, community_used, grid_used FROM hourly_usage ORDER BY hour DESC LIMIT 10;"
```

Expected:

- At least one row after Producer/User have run for a short time.
- `hour` is truncated to the full hour, for example `2026-05-16 11:00:00`.
- `community_used <= community_produced`.
- `grid_used` is non-negative.

Check percentage rows:

```powershell
docker exec energy-db psql -U user -d energy_community -c "SELECT hour, community_depleted, grid_portion FROM current_percentage ORDER BY hour DESC LIMIT 10;"
```

Expected:

- At least one row for the latest updated hour.
- `community_depleted` is rounded to two decimals.
- `grid_portion` is rounded to two decimals.

## 7. REST API Verification

Call current data:

```powershell
curl http://localhost:8080/energy/current
```

Expected shape:

```json
{
  "hour": "2026-05-16T11:00",
  "communityDepleted": 16.03,
  "gridPortion": 10.73
}
```

Call historical data. Adjust the date to the current demo date if needed:

```powershell
curl "http://localhost:8080/energy/historical?start=2026-05-16T00:00:00&end=2026-05-16T23:59:59"
```

Expected shape:

```json
[
  {
    "hour": "2026-05-16T11:00",
    "communityProduced": 228.525,
    "communityUsed": 36.633326434041905,
    "gridUsed": 4.404378895368673
  }
]
```

Invalid range check:

```powershell
curl -i "http://localhost:8080/energy/historical?start=2026-05-17T00:00:00&end=2026-05-16T00:00:00"
```

Expected:

```text
HTTP/1.1 400
```

## 8. JavaFX GUI Verification

With all services running:

1. Open the JavaFX GUI.
2. Click the refresh/current action.
3. Verify that the current section shows:
   - hour,
   - community depleted percentage,
   - grid portion percentage.
4. Enter a historical range that includes the current date.
5. Example values:

```text
Start: 2026-05-16T00:00:00
End:   2026-05-16T23:59:59
```

or, if using the German input format:

```text
Start: 16.05.2026 00:00
End:   16.05.2026 23:59
```

6. Click the historical/show-data action.
7. Verify that the GUI displays:
   - community produced,
   - community used,
   - grid used,
   - no database connection prompt or database error.

If the selected time range has no data, the GUI should show a controlled empty/error message instead of crashing.

## 9. Presentation Evidence To Capture

Useful screenshots or notes for the final review:

- RabbitMQ Queues page showing `energy_queue` and `percentage_update_queue`.
- Producer/User logs showing sent messages.
- Usage Service logs showing consumed messages and saved hourly values.
- Percentage Service logs showing update handling.
- PostgreSQL query result for `hourly_usage`.
- PostgreSQL query result for `current_percentage`.
- REST `/energy/current` JSON.
- REST `/energy/historical` JSON.
- JavaFX GUI current and historical display.

## 10. Stop Services

Stop Java services by pressing `Ctrl+C` in each service terminal.

Keep Docker running if you want to inspect DB/RabbitMQ after the run. Otherwise stop infrastructure:

```powershell
docker compose down
```

To reset the DB after the demo:

```powershell
docker compose down -v
```

## 11. Last Verified Smoke Evidence

An automated backend/message/database/REST smoke test was executed on 2026-05-16 with a fresh Docker PostgreSQL volume.

Observed:

- `energy_queue`: `1` consumer, `0` messages, `0` unacknowledged.
- `percentage_update_queue`: `1` consumer, `0` messages, `0` unacknowledged.
- `GET /energy/current` returned DB-backed JSON.
- `GET /energy/historical` returned an hourly row.
- `hourly_usage` and `current_percentage` contained matching latest-hour rows.

Artifact:

```text
C:\dev\energy_community_project\.agent-local\smoke-20260516-114338\smoke-result.json
```

The JavaFX GUI was not opened during that automated smoke run. Execute section 8 manually before the final hand-in to prove the GUI labels visually.
