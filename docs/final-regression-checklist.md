# Final Regression Checklist for Hand-In

This checklist ties **every item in the official grading schema** (`ProjektGrading.pdf`, Moodle
"Project Grading Schema", BWI-BB-4-SS2026-DISYS-EN) to a concrete command or manual check. Run it
end to end before the final hand-in and before the presentation. Fix or document any failing box
before submission.

- It is **self-contained**: the grading text is quoted inline, so no item depends on the gitignored
  `project-resources/` folder or any other local-only file.
- One section per grading category. Each box is pass/fail: `[ ]` = not verified, `[x]` = passed,
  `[!]` = failed/needs follow-up (add a note).
- Commands are written for **PowerShell on Windows** and are copyable as-is.
- For the full manual end-to-end runbook see [how-to-run.md](how-to-run.md) and
  [smoke-test.md](smoke-test.md). For startup/isolation evidence see
  [independent-startup-verification.md](independent-startup-verification.md).

---

## 0. Prerequisites (run once)

Start infrastructure and all components from a clean process list. Use separate terminals per
service (start order: usage → percentage → rest → producer → user → gui).

```powershell
# From repo root
docker compose up -d
docker compose ps           # expect energy-db and energy-mq "Up"
```

```powershell
# One terminal per service
cd usage-service       ; .\mvnw.cmd spring-boot:run
cd percentage-service  ; .\mvnw.cmd spring-boot:run
cd rest-api            ; .\mvnw.cmd spring-boot:run
cd energy-producer     ; .\mvnw.cmd spring-boot:run
cd energy-user         ; .\mvnw.cmd spring-boot:run
cd energy-gui          ; ..\energy-producer\mvnw.cmd -f pom.xml javafx:run
```

- [ ] Infrastructure is up (`docker compose ps` shows `energy-db` and `energy-mq` as `Up`).
- [ ] All six components are running, each started in its own terminal.

---

## 1. Final Submission Must-Haves (0-point gates)

> Grading: "If these elements are not in the project, the final project will be graded with **0
> points**." Every component independently startable; system builds and runs with no errors; Spring
> Boot for REST API; JavaFX for GUI; RabbitMQ for service communication; GitHub repository link in
> submission.

```powershell
# Build every module independently (proves "builds with no errors" + independent compilation)
foreach ($m in "energy-producer","energy-user","usage-service","percentage-service","rest-api") {
  Push-Location $m; .\mvnw.cmd clean package; Pop-Location
}
.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml clean package
```

```powershell
# Each module is a standalone Maven project with its own pom and entry point
Get-ChildItem energy-producer,energy-user,usage-service,percentage-service,rest-api,energy-gui -Filter pom.xml | Select-Object Directory
# No forbidden cross-module/shared imports (expect: no output)
rg "com\.energy_community_project\.shared" .
```

- [ ] **Independent startup**: each of the 6 components starts alone with only its required infra
      (producer/user need RabbitMQ; usage/percentage need RabbitMQ + PostgreSQL; rest needs
      PostgreSQL; GUI needs only itself). Each backend log shows `Started <Name>Application`.
- [ ] **Builds and runs with no errors**: all 6 `clean package` runs end in `BUILD SUCCESS`.
- [ ] **Spring Boot for REST API**: `rest-api/pom.xml` uses `spring-boot-starter-webmvc`.
- [ ] **JavaFX for GUI**: `energy-gui/pom.xml` uses `javafx-controls` and the
      `javafx-maven-plugin`.
- [ ] **RabbitMQ for service communication**: producer/user → `energy_queue` → usage; usage →
      `percentage_update_queue` → percentage (confirm in step 8 below and the Management UI).
- [ ] **GitHub repository link** is included in the Moodle submission text.

---

## 2. Energy Producer — 10%

> Grading: "Sends production message in random 1–5 second intervals to the message queue.
> Production message must include a random but sensible kWh value. Weather data must be used to
> determine the kWh value."

```powershell
# Watch the producer terminal; it prints one line per published message:
#   Sent: PRODUCER - 0.00xxx kWh
```

- [ ] Producer publishes `PRODUCER` messages to `energy_queue` (lines `Sent: PRODUCER - ... kWh`).
- [ ] Interval between consecutive sends is **random ~1–5 s** (1 s fixed delay after completion +
      random 0–4 s sleep before each publish; eyeball the timestamps of consecutive lines).
- [ ] kWh value is random but sensible (within configured `app.production.min-kwh` …
      `app.production.max-kwh`, default `0.001`–`0.006`).
- [ ] Weather determines the value: with internet, producer fetches the current Open-Meteo solar
      radiation; offline `WeatherClient` logs `Weather API not reachable` and produces the minimum kWh.

---

## 3. Energy User — 10%

> Grading: "Sends usage message in random 1–5 second intervals to the message queue. Usage message
> must include a random but sensible kWh value. Time of day must be used to determine the kWh value."

- [ ] User publishes `USER` messages to `energy_queue` (lines `Sent: USER - ... kWh`).
- [ ] Interval between consecutive sends is **random ~1–5 s** (same scheduler design as producer).
- [ ] kWh value is random but sensible (`0.001` base + random variation `< 0.002`, times the
      time-of-day multiplier).
- [ ] Time of day is applied: higher demand during peak hours (07:00–09:59 / 18:00–21:59, ×3.0),
      lower at night (23:00–05:59, ×0.5), ×1.0 otherwise.

---

## 4. Usage Service — 30%

> Grading: "Receives the production/usage messages and updates the usage table correctly.
> Afterwards sends an update message to the queue."

```powershell
# PostgreSQL: latest aggregated rows
docker exec energy-db psql -U user -d energy_community -c "SELECT * FROM hourly_usage ORDER BY hour DESC LIMIT 10;"
# Invariant must hold for every row (expect 0 rows returned)
docker exec energy-db psql -U user -d energy_community -c "SELECT * FROM hourly_usage WHERE community_used > community_produced;"
```

- [ ] Consumes both `PRODUCER` and `USER` messages from `energy_queue`.
- [ ] Buckets each message to the start of its hour (e.g. a `14:34` message updates the `14:00` row).
- [ ] `hourly_usage` gets fresh rows/updates after producer/user run.
- [ ] Allocation is correct: community energy covers demand first, remainder goes to grid
      (`community_used` + `grid_used` reconcile with messages for the hour).
- [ ] Invariant `community_used <= community_produced` holds (query above returns **no rows**).
- [ ] After saving the hourly usage row, an update message is published to `percentage_update_queue`
      (verify via Management UI / step 8).

---

## 5. Current Percentage Service — 30%

> Grading: "Receives the update message and updates the current percentage table correctly."

```powershell
docker exec energy-db psql -U user -d energy_community -c "SELECT * FROM current_percentage ORDER BY hour DESC LIMIT 10;"
```

- [ ] Consumes update messages from `percentage_update_queue`.
- [ ] Calculates from the update message and writes `current_percentage` for the matching hour.
- [ ] `community_depleted = community_used / community_produced * 100`, rounded to 2 decimals.
- [ ] `grid_portion = grid_used / (community_used + grid_used) * 100`, rounded to 2 decimals.
- [ ] Division by zero is handled (returns `0`, no crash / no NaN rows).

---

## 6. REST API — 10%

> Grading: "The Spring Boot App reads the data from the database instead of using static sample
> data."

```powershell
curl http://localhost:8080/energy/current
curl "http://localhost:8080/energy/historical?start=2026-05-16T00:00:00&end=2026-05-16T23:59:59"
# Error handling
curl "http://localhost:8080/energy/historical?start=not-a-date&end=also-bad"   # expect 400
```

- [ ] `GET /energy/current` returns JSON backed by `current_percentage` (values change as the
      backend runs — not static).
- [ ] `GET /energy/historical?start=...&end=...` returns a JSON array backed by `hourly_usage`.
- [ ] Cross-check: REST `current` values equal the latest `current_percentage` DB row.
- [ ] Invalid date format returns `400 Bad Request`; a reversed range (`start` after `end`) returns an empty array.

---

## 7. JavaFX UI — 10%

> Grading: "The GUI shows community pool usage and grid portion in percent. It has a button to
> refresh the usages. It enables the selection of a date range and includes a button to show the
> community produced, used and grid used data in kWh for the selected date range."

- [ ] GUI shows **community pool usage** and **grid portion** in percent.
- [ ] **Refresh** button reloads current values from `GET /energy/current`.
- [ ] Date range can be selected (start/end fields, ISO format such as `2026-05-16T00:00`).
- [ ] **Show data** button displays **community produced / used / grid used** in kWh for the range
      (from `GET /energy/historical`).
- [ ] GUI calls only the REST API — no PostgreSQL/JPA/RabbitMQ dependency.
- [ ] If REST is down, the GUI still opens and shows `Error fetching data` (graceful degradation).

---

## 8. RabbitMQ Message Flow (mandatory communication channel)

```powershell
# Queue overview via the Management API (guest/guest)
curl -u guest:guest http://localhost:15672/api/queues
# Or open the UI:  http://localhost:15672  (Queues tab)
```

- [ ] `energy_queue` exists and drains during normal operation (≈0 ready messages, 1 consumer).
- [ ] `percentage_update_queue` exists and drains during normal operation (≈0 ready, 1 consumer).
- [ ] End-to-end chain observed: Producer/User → `energy_queue` → Usage → DB → `percentage_update_queue`
      → Percentage → DB → REST → GUI.

---

## 9. Repository Hygiene

```powershell
# No build artifacts tracked (expect 0)
(git ls-files | Select-String "/target/").Count
# Working tree clean before tagging the submission
git status --short
# Every team member has commits
git shortlog -sn HEAD
```

- [ ] No `target/` build artifacts are tracked in Git (count is `0`).
- [ ] `.gitignore` excludes `target/`, IDE files, secrets/`.env`, and local agent folders.
- [ ] `git status` is clean (or only intended changes) at submission time.
- [ ] **Every person has commits** — `git shortlog -sn` lists each team member.
- [ ] GitHub repository link is ready to paste into the Moodle submission.
- [ ] No checklist step relies on a local-only/ignored file (e.g. `project-resources/`,
      `.agent-local/`).

---

## 10. Known Acceptable Warnings

These appear during a healthy build/run and do **not** indicate failure:

- [ ] `OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes ...` —
      JVM CDS notice; harmless.
- [ ] Producer `Weather API not reachable, using 0 W/m2` — expected when Open-Meteo is
      unreachable; `WeatherClient` returns `0` and the producer keeps working at minimum kWh.
- [ ] Git `LF will be replaced by CRLF` — Windows line-ending notice on commit; harmless.

Any warning **not** listed here should be investigated before hand-in.

---

## 11. Presentation Readiness

> Grading note: points can be omitted for badly written code, insufficient presentation, or failure
> to explain the system.

- [ ] Every member can explain why this is a distributed system (independent components + messaging).
- [ ] Every member can explain synchronous REST vs asynchronous RabbitMQ.
- [ ] Every member can explain which component writes which table (usage → `hourly_usage`,
      percentage → `current_percentage`).
- [ ] Every member can explain the Usage Service allocation (community first, grid fallback) and the
      Percentage Service formulas.
- [ ] Every member can explain why the GUI never touches the database directly.
- [ ] Clean demo rehearsed end to end from a fresh `docker compose up -d` and clean process list.

---

## Sign-Off

| Grading category | Weight | Result (pass/fail) | Verified by | Date |
|---|---:|---|---|---|
| Must-Haves (0-point gates) | gate | | | |
| Energy Producer | 10% | | | |
| Energy User | 10% | | | |
| Usage Service | 30% | | | |
| Current Percentage Service | 30% | | | |
| REST API | 10% | | | |
| JavaFX UI | 10% | | | |
| RabbitMQ flow | mandatory | | | |
| Repository hygiene | gate | | | |
| Presentation readiness | — | | | |
