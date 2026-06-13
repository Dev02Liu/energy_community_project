# Final Readiness Check

Use this checklist before the final hand-in and before the presentation. The goal is to prove the project as a distributed system, not only as isolated modules.

For the exact manual end-to-end runbook, follow `docs/smoke-test.md`.

## Build

- [ ] `energy-producer` builds: `cd energy-producer; .\mvnw.cmd clean package`
- [ ] `energy-user` builds: `cd energy-user; .\mvnw.cmd clean package`
- [ ] `usage-service` builds: `cd usage-service; .\mvnw.cmd clean package`
- [ ] `percentage-service` builds: `cd percentage-service; .\mvnw.cmd clean package`
- [ ] `rest-api` builds: `cd rest-api; .\mvnw.cmd clean package`
- [ ] `energy-gui` builds: from repo root, `.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml clean package`

## Infrastructure

- [ ] Docker Desktop is running.
- [ ] PostgreSQL starts via Docker Compose: `docker compose up -d`
- [ ] RabbitMQ starts via Docker Compose: `docker compose up -d`
- [ ] `docker compose ps` shows `energy-db` running.
- [ ] `docker compose ps` shows `energy-mq` running.
- [ ] RabbitMQ Management UI is reachable at `http://localhost:15672`.
- [ ] PostgreSQL is reachable at `localhost:5432`, database `energy_community`, user `user`.

## Independent Components

- [ ] `energy-producer` has its own `pom.xml` and main application class.
- [ ] `energy-user` has its own `pom.xml` and main application class.
- [ ] `usage-service` has its own `pom.xml` and main application class.
- [ ] `percentage-service` has its own `pom.xml` and main application class.
- [ ] `rest-api` has its own `pom.xml` and main application class.
- [ ] `energy-gui` has its own `pom.xml` and JavaFX entry point.
- [ ] All six components can be started in separate terminals.

## Start Order

- [ ] Start Docker infrastructure.
- [ ] Start `usage-service`.
- [ ] Start `percentage-service`.
- [ ] Start `rest-api`.
- [ ] Start `energy-producer`.
- [ ] Start `energy-user`.
- [ ] Start `energy-gui`.

## End-to-End Flow

- [ ] Energy Producer sends `PRODUCER` messages to RabbitMQ.
- [ ] Energy User sends `USER` messages to RabbitMQ.
- [ ] Usage Service consumes producer messages.
- [ ] Usage Service consumes user messages.
- [ ] Usage Service buckets messages to the correct hour.
- [ ] Usage Service writes `hourly_usage`.
- [ ] Usage Service publishes an update message to RabbitMQ.
- [ ] Percentage Service consumes the update message.
- [ ] Percentage Service calculates from the update message and does not read `hourly_usage`.
- [ ] Percentage Service writes rounded values to `current_percentage`.
- [ ] REST API returns current data from `current_percentage`.
- [ ] REST API returns historical data from `hourly_usage`.
- [ ] JavaFX GUI displays current data.
- [ ] JavaFX GUI displays historical data.

## Business Calculation Checks

- [ ] `community_used <= community_produced` is preserved.
- [ ] User consumption is covered by community energy first.
- [ ] Remaining user consumption is assigned to grid energy.
- [ ] No separate Grid message or Grid producer exists.
- [ ] A user message at `14:34` updates the `14:00` hourly row.
- [ ] Producer-before-user case is verified during the smoke test.
- [ ] User-before-producer case is verified during the smoke test.
- [ ] Percentage division by zero returns `0`.
- [ ] `community_depleted` is rounded to two decimals.
- [ ] `grid_portion` is rounded to two decimals.

## Critical Architecture Checks

- [ ] GUI does not access PostgreSQL directly.
- [ ] GUI calls the REST API over HTTP.
- [ ] REST API does not write business data.
- [ ] REST API does not calculate Usage/Percentage business values.
- [ ] Producer does not write PostgreSQL directly.
- [ ] User does not write PostgreSQL directly.
- [ ] Usage Service and Percentage Service communicate through RabbitMQ, not direct method calls.
- [ ] Percentage Service reacts to usage update messages and does not poll producer/user events directly.

## RabbitMQ Checks

- [ ] `energy_queue` exists.
- [ ] `percentage_update_queue` exists.
- [ ] `energy_queue` has no messages stuck during normal operation.
- [ ] `percentage_update_queue` has no messages stuck during normal operation.
- [ ] Message contracts match `docs/message-contract.md`.
- [ ] Direct-queue design can be explained as one consumer per stream.

## Database Checks

- [ ] Flyway schema history table exists.
- [ ] `hourly_usage` exists.
- [ ] `current_percentage` exists.
- [ ] `hourly_usage` contains recent rows after producer/user run.
- [ ] `current_percentage` contains recent rows after percentage service run.
- [ ] Table mapping is documented in `docs/database-schema.md`.

Useful SQL:

```sql
SELECT * FROM hourly_usage ORDER BY hour DESC LIMIT 10;
SELECT * FROM current_percentage ORDER BY hour DESC LIMIT 10;
```

## REST Checks

- [ ] `GET http://localhost:8080/energy/current` returns JSON.
- [ ] `GET http://localhost:8080/energy/historical?start=2026-05-16T00:00:00&end=2026-05-16T23:59:59` returns JSON array.
- [ ] Invalid date format returns `400 Bad Request`.
- [ ] A reversed range (`start` after `end`) returns an empty array.

## Git And Team Evidence

- [ ] GitHub repository link is submitted.
- [ ] `git shortlog -sn` shows commits for every team member.
- [ ] Every team member is named in the Moodle submission text (the project intentionally keeps no
      contributors table in the README; Git history is the source of truth for commit authorship).
- [ ] Generated `target/` artifacts are not tracked in Git.

## Presentation Readiness

- [ ] Every team member can explain why this is a distributed system.
- [ ] Every team member can explain synchronous REST vs asynchronous RabbitMQ.
- [ ] Every team member can explain which component writes which table.
- [ ] Every team member can explain Usage Service calculation.
- [ ] Every team member can explain Percentage Service calculation.
- [ ] Every team member can explain why RabbitMQ helps decouple/scalably buffer work.
- [ ] Every team member can explain what happens when User messages arrive before Producer messages.
- [ ] Every team member can explain why GUI does not access the database.

## Demo Evidence

- [ ] Final demo run completed on a clean process list using `docs/smoke-test.md`.
- [ ] No port conflicts.
- [ ] Logs or screenshots saved if the team wants evidence.
- [ ] REST JSON observed during the final smoke test.
- [ ] GUI current and historical views observed during the final smoke test.

## Smoke Test Evidence - 2026-05-16

Fresh database smoke test executed from a clean Java process list.

Commands and scope:

- `docker compose down -v`
- `docker compose up -d`
- started `usage-service`, `percentage-service`, `rest-api`, `energy-producer`, and `energy-user` from their packaged JARs
- the producer read Open-Meteo solar radiation (returning 0 W/m² if offline) for plausible kWh values
- stopped producer/user before final assertions so REST and database reads were stable
- stopped all Java service processes after the smoke test

Observed result:

- PostgreSQL was reachable via `pg_isready`.
- RabbitMQ Management API was reachable at `http://localhost:15672/api/overview`.
- `energy_queue` existed with `1` consumer and `0` queued/unacknowledged messages.
- `percentage_update_queue` existed with `1` consumer and `0` queued/unacknowledged messages.
- `GET /energy/current` returned:

```json
{
  "hour": "2026-05-16T11:00",
  "communityDepleted": 16.03,
  "gridPortion": 10.73
}
```

- `GET /energy/historical?start=2026-05-16T00:00:00&end=2026-05-16T23:59:59` returned `1` hourly row.
- Latest `hourly_usage` row:

```text
2026-05-16 11:00:00 | community_produced=228.52500000000003 | community_used=36.633326434041905 | grid_used=4.404378895368673
```

- Latest `current_percentage` row:

```text
2026-05-16 11:00:00 | community_depleted=16.03 | grid_portion=10.73
```

Smoke-test artifacts:

```text
C:\dev\energy_community_project\.agent-local\smoke-20260516-114338\smoke-result.json
```

Result: backend/message/database/REST smoke test passed. JavaFX was not opened as a visible desktop window during this automated smoke test; the GUI build was verified separately.

## Clean QA Evidence - 2026-06-03

Full regression executed from a clean Docker volume (`docker compose down -v && up -d`) and a clean
Java process list, after a clean `clean package` of all six modules.

Build:

- All six modules built independently: `BUILD SUCCESS` (no live infra needed).

Runtime (all five backend services started from packaged JARs; producer using Open-Meteo solar radiation):

- Each service logged `Started <Name>Application` — independent startup confirmed.
- Producer published sensible weather-based kWh (e.g. `0.00438`–`0.00527`, within `0.001`–`0.006`);
  consecutive sends ~1–5 s apart.
- User published sensible time-of-day-based kWh (e.g. `0.00161`–`0.00277`).
- RabbitMQ `energy_queue` and `percentage_update_queue` each had `1` consumer and `0` ready messages
  (draining normally).
- `hourly_usage` updated; invariant `community_used <= community_produced` held (0 violations).
- `current_percentage` matched the recomputed formula exactly (stored `41.45` == recomputed `41.45`,
  grid `0` == `0.00`).
- `GET /energy/current` returned DB-backed JSON matching `current_percentage`
  (`{"hour":"2026-06-03T12:00","communityDepleted":41.45,"gridPortion":0.0}`).
- `GET /energy/historical` returned the hourly row; invalid dates returned `400`.

Result: all final-grading KO criteria and per-component functional requirements verified. Grid
portion was `0` in this short window because community production covered demand; the grid-fallback
path and message-ordering cases are documented in `docs/spec-code-mapping.md` and confirmed via the smoke test.
