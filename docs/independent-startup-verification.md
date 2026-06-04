# Independent Startup and Port/Config Isolation Verification

This report verifies Issue 14: every component starts independently with only its required
infrastructure, each Spring Boot service has its own port/config, and no service depends on
another service module at build or startup time.

Verified on Java 25 (Temurin 25.0.3) / Spring Boot 4.0.3 with PostgreSQL 17 and RabbitMQ 4
running via `docker compose up -d`.

## Summary

| Definition of Done item | Result |
|---|---|
| Each Spring Boot service has its own port/config | PASS |
| Producer/user can start with RabbitMQ running | PASS |
| Usage/percentage can start with RabbitMQ and PostgreSQL running | PASS |
| REST can start with PostgreSQL running | PASS |
| GUI can start with REST unavailable and show errors gracefully | PASS (graceful + documented) |

| Acceptance criterion | Result |
|---|---|
| Starting one service does not require compiling or starting another service module | PASS |
| No service imports implementation classes from another service module | PASS |
| Spring Boot configuration values are documented and overrideable | PASS |

## 1. Module isolation (build time)

- There is no aggregator/root `pom.xml` and no `<modules>` declaration in any module. Each of the
  six components (`energy-producer`, `energy-user`, `usage-service`, `percentage-service`,
  `rest-api`, `energy-gui`) is a standalone Maven project with its own `pom.xml`.
- Each Spring module ships its own Maven Wrapper (`mvnw.cmd`). `energy-gui` is built with the
  producer's wrapper against its own `pom.xml` (`..\energy-producer\mvnw.cmd -f pom.xml`); this is
  build-tooling reuse only, not a code or runtime dependency, and is documented in
  [how-to-run.md](how-to-run.md).
- No forbidden cross-module dependencies. The check from the issue returns nothing:

  ```text
  rg "com.energy_community_project.shared"      -> no matches (no shared module exists)
  ```

  Every module imports only its own package namespace (`energy_producer`, `energy_user`,
  `usage_service`, `percentage_service`, `rest_api`, `gui`); no module imports another module's
  classes.

## 2. Independent compilation and tests

Each module was built independently with `clean package` (tests included). Tests use H2 and
disabled RabbitMQ listeners (`spring.rabbitmq.listener.simple.auto-startup=false`), so they need
no live infrastructure.

| Module | `clean package` |
|---|---|
| energy-producer | BUILD SUCCESS |
| energy-user | BUILD SUCCESS |
| usage-service | BUILD SUCCESS |
| percentage-service | BUILD SUCCESS |
| rest-api | BUILD SUCCESS |
| energy-gui | BUILD SUCCESS |

## 3. Port / config isolation

- Only `rest-api` configures and binds an HTTP port: `server.port=8080`
  (`Tomcat started on port 8080`).
- `energy-producer`, `energy-user`, `usage-service`, `percentage-service` use
  `spring-boot-starter-amqp` with no web starter and start **no** servlet container, so they can
  all run simultaneously without a port conflict.
- `energy-producer` and `energy-user` explicitly exclude `DataSourceAutoConfiguration` and
  `HibernateJpaAutoConfiguration`, so they do not require PostgreSQL at startup.
- Per-module configuration is documented in [docs/modules-documentation/](modules-documentation/).
  Spring Boot override mechanisms are documented in the [README](../README.MD). The GUI uses the
  local review/demo REST URL `http://localhost:8080`.

## 4. Independent runtime startup (only infra running)

Each Spring Boot service was started **alone** from its built jar, with only PostgreSQL and
RabbitMQ running (no other application service started). All reached their `Started ...Application`
log line:

| Service | Required infra | Result |
|---|---|---|
| energy-producer | RabbitMQ | Started EnergyProducerApplication |
| energy-user | RabbitMQ | Started EnergyUserApplication |
| usage-service | RabbitMQ + PostgreSQL | Started UsageServiceApplication |
| percentage-service | RabbitMQ + PostgreSQL | Started PercentageServiceApplication |
| rest-api | PostgreSQL | Started RestApiApplication |

The DB-backed services run Flyway and pass Hibernate `validate` against PostgreSQL on startup
(`Successfully validated 1 migration`, `Schema "public" is up to date`).

REST was additionally checked functionally with only PostgreSQL running:

```text
GET http://localhost:8080/energy/current
-> HTTP 200
   {"hour":"2026-06-03T12:00","communityDepleted":0.0,"gridPortion":0.0}
```

## 5. GUI startup without REST

`EnergyGuiApplication.start()` builds and shows the window first, then issues the initial
`/energy/current` call asynchronously (`CompletableFuture` with an `exceptionally` handler and a
3-second connect timeout). If the REST API is unavailable, the GUI still opens and the labels show
`Error fetching data` instead of crashing; refresh and show-data behave the same way. This is both
implemented and documented (see [modules-documentation/energy-gui.md](modules-documentation/energy-gui.md)).

The GUI uses the local review/demo REST URL `http://localhost:8080`.

## How to reproduce

```powershell
# Infra
docker compose up -d

# Forbidden-dependency check (expect no output)
rg "com.energy_community_project.shared|import com.energy_community_project.(energy_producer|energy_user|usage_service|percentage_service|rest_api|gui)" `
   | Select-String -NotMatch "/(\w+)/src/main/java/.*\1"

# Build each module independently
foreach ($m in "energy-producer","energy-user","usage-service","percentage-service","rest-api") {
  Push-Location $m; .\mvnw.cmd clean package; Pop-Location
}
.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml clean package

# Start each service alone (separate terminals) and look for "Started ...Application"
cd usage-service; .\mvnw.cmd spring-boot:run
```
