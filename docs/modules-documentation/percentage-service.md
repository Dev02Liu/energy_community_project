# Percentage Service Module

## Purpose

`percentage-service` is an independently startable Spring Boot application and the second grading-critical core service.

It consumes usage update messages from RabbitMQ, reads the matching hourly usage row from PostgreSQL, calculates percentage values, and writes them to `current_percentage`.

## Tech Stack

| Area | Implementation |
|---|---|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Messaging | Spring AMQP, `@RabbitListener`, JSON converter |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL at runtime, H2 in tests |
| Migration | Flyway |
| Transactions | Spring `@Transactional` |
| Tests | JUnit 5, Mockito, AssertJ, Spring Data JPA tests |

## Main Components

| Class / Package | Responsibility |
|---|---|
| `PercentageServiceApplication` | Spring Boot entry point. |
| `config/RabbitMqConfig` | Declares update queue and AMQP JSON converter. |
| `listener/HourlyUsageUpdatedListener` | RabbitMQ boundary. Receives `HourlyUsageUpdatedMessage`. |
| `messaging/HourlyUsageUpdatedMessage` | Service-local DTO consumed from Usage Service JSON. |
| `entity/HourlyUsageEntity` | Read model for table `hourly_usage`. |
| `entity/CurrentPercentageEntity` | Write model for table `current_percentage`. |
| `repository/HourlyUsageRepository` | Reads hourly usage rows. |
| `repository/CurrentPercentageRepository` | Reads/writes current percentage rows. |
| `service/CurrentPercentageCalculationService` | Calculates and persists rounded percentage values. |
| `db/migration/V1__create_energy_tables.sql` | Flyway migration for required tables. |

## Configuration

File: `percentage-service/src/main/resources/application.properties`

| Property | Current Value / Meaning |
|---|---|
| HTTP port | none; this module is a RabbitMQ worker |
| `app.update-queue.name` | `percentage_update_queue` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/energy_community` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |

## Runtime Flow

```mermaid
flowchart LR
    Usage["Usage Service"]
    Queue["RabbitMQ<br/>percentage_update_queue"]
    Listener["HourlyUsageUpdatedListener"]
    Service["CurrentPercentageCalculationService"]
    UsageTable["PostgreSQL<br/>hourly_usage"]
    PercentageTable["PostgreSQL<br/>current_percentage"]

    Usage --> Queue
    Queue --> Listener
    Listener --> Service
    Service --> UsageTable
    Service --> PercentageTable
```

## Calculation Rules

```text
communityDepleted = communityUsed / communityProduced * 100
```

If `communityProduced = 0`, `communityDepleted = 0`.

```text
gridPortion = gridUsed / (communityUsed + gridUsed) * 100
```

If `communityUsed + gridUsed = 0`, `gridPortion = 0`.

Both values are rounded with `HALF_UP` to two decimals before persistence.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Q as RabbitMQ percentage_update_queue
    participant L as HourlyUsageUpdatedListener
    participant S as CurrentPercentageCalculationService
    participant HU as hourly_usage
    participant CP as current_percentage

    Q->>L: HourlyUsageUpdatedMessage(hour)
    L->>S: updateCurrentPercentage(hour)
    alt delayed event for an older hour
        S-->>L: ignore event
    else event for the current hour
    S->>HU: read hourly usage row
    S->>S: calculate rounded percentages
    S->>CP: delete stale rows for other hours
    S->>CP: upsert percentage row
    end
```

## Start Command

```powershell
cd percentage-service
.\mvnw.cmd spring-boot:run
```

## Verification

```powershell
cd percentage-service
.\mvnw.cmd test
```

Important checks:

- Consumes `percentage_update_queue`.
- Does not consume Producer/User messages.
- Reads `hourly_usage`.
- Writes `current_percentage`.
- Avoids division by zero.
- Rounds persisted values to two decimals.
- Ignores delayed update events for older hours.
- Removes stale percentage rows when recalculating the current hour.
- Contract test deserializes documented update JSON.
