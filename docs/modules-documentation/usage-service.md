# Usage Service Module

## Purpose

`usage-service` is an independently startable Spring Boot application and one of the two grading-critical core services.

It consumes producer/user messages from RabbitMQ, aggregates them into hourly usage rows, writes PostgreSQL, and publishes an update message for the Percentage Service.

## Tech Stack

| Area | Implementation |
|---|---|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Messaging | Spring AMQP, `@RabbitListener`, `RabbitTemplate`, JSON converter |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL at runtime, H2 in tests |
| Migration | Flyway |
| Transactions | Spring `@Transactional` |
| Tests | JUnit 5, Mockito, AssertJ, Spring Data JPA tests |

## Main Components

| Class / Package | Responsibility |
|---|---|
| `UsageServiceApplication` | Spring Boot entry point. |
| `config/RabbitMqConfig` | Declares durable queues and AMQP JSON converter. |
| `listener/EnergyMessageListener` | RabbitMQ boundary. Receives `EnergyMessage` from `energy_queue` and delegates to service logic. |
| `messaging/EnergyMessage` | Service-local DTO consumed from Producer/User JSON. |
| `messaging/HourlyUsageUpdatedMessage` | Service-local DTO published after usage changes. |
| `entity/HourlyUsageEntity` | JPA entity for table `hourly_usage`. |
| `repository/HourlyUsageRepository` | Data access for hourly usage rows. |
| `service/HourlyUsageUpdateService` | Validates messages, applies business calculation, writes DB, publishes update event. |
| `db/migration/V1__create_energy_tables.sql` | Flyway migration for required tables. |

## Configuration

File: `usage-service/src/main/resources/application.properties`

| Property | Current Value / Meaning |
|---|---|
| HTTP port | none; this module is a RabbitMQ worker |
| `app.queue.name` | `energy_queue` |
| `app.update-queue.name` | `percentage_update_queue` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/energy_community` |
| `spring.jpa.hibernate.ddl-auto` | `validate` |

## Runtime Flow

```mermaid
flowchart LR
    Producer["Energy Producer"]
    User["Energy User"]
    EnergyQueue["RabbitMQ<br/>energy_queue"]
    Listener["EnergyMessageListener"]
    Service["HourlyUsageUpdateService"]
    Repo["HourlyUsageRepository"]
    Table["PostgreSQL<br/>hourly_usage"]
    UpdateMsg["HourlyUsageUpdatedMessage"]
    UpdateQueue["RabbitMQ<br/>percentage_update_queue"]

    Producer --> EnergyQueue
    User --> EnergyQueue
    EnergyQueue --> Listener
    Listener --> Service
    Service --> Repo
    Repo --> Table
    Service --> UpdateMsg
    UpdateMsg --> UpdateQueue
```

## Business Rules

Every message is bucketed to the start of the hour:

```text
2025-01-10T14:34 -> 2025-01-10T14:00
```

For `PRODUCER`:

```text
communityProduced += message.kwh
```

For `USER`:

```text
availableCommunityEnergy = max(communityProduced - communityUsed, 0)
communityPart = min(message.kwh, availableCommunityEnergy)
gridPart = message.kwh - communityPart

communityUsed += communityPart
gridUsed += gridPart
```

Invariant:

```text
communityUsed <= communityProduced
```

A `PRODUCER` message increases `communityProduced`; every other message is treated as usage. Since the
only senders are the Energy Producer and Energy User (which always send valid `COMMUNITY` messages),
the service keeps the lecture style and does not add extra message validation.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Q as RabbitMQ energy_queue
    participant L as EnergyMessageListener
    participant S as HourlyUsageUpdateService
    participant DB as PostgreSQL hourly_usage
    participant UQ as RabbitMQ percentage_update_queue

    Q->>L: EnergyMessage JSON
    L->>S: handleEnergyMessage(message)
    S->>S: validate and truncate datetime to hour
    S->>DB: find or create hourly row
    S->>DB: save updated usage values
    DB-->>S: commit transaction
    S->>UQ: publish HourlyUsageUpdatedMessage(hour)
```

## Start Command

```powershell
cd usage-service
.\mvnw.cmd spring-boot:run
```

## Verification

```powershell
cd usage-service
.\mvnw.cmd test
```

Important checks:

- Consumes `energy_queue`.
- Writes `hourly_usage`.
- Publishes `percentage_update_queue` after the database commit.
- Preserves `communityUsed <= communityProduced`.
- Handles user-before-producer order according to message order.
- Contract tests deserialize documented Producer/User JSON.
