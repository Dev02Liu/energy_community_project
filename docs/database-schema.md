# Database Schema

The project uses one shared PostgreSQL database called `energy_community`. Schema creation is reproducible through Flyway migrations in the DB-backed modules:

- `usage-service/src/main/resources/db/migration/V1__create_energy_tables.sql`
- `percentage-service/src/main/resources/db/migration/V1__create_energy_tables.sql`
- `rest-api/src/main/resources/db/migration/V1__create_energy_tables.sql`

Hibernate is configured with schema validation:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This means Flyway is the source of truth for table creation and Hibernate only verifies that the JPA entities match the existing schema.

## Table Mapping

| Conceptual Specification Table | Implemented Table | Reason / Meaning |
|---|---|---|
| `energy_usage_hourly` | `hourly_usage` | Stores the same hourly aggregated usage values. The shorter implementation name is used consistently by JPA, Flyway, REST, and Usage Service. |
| `current_percentage` | `current_percentage` | Stores the latest calculated percentage values used by REST and GUI. |

## `hourly_usage`

Purpose:

- stores aggregated hourly energy usage values,
- groups all producer/user messages by the beginning of their hour,
- is written by Usage Service,
- is read by Percentage Service,
- is read by REST API for historical data.

Columns:

| Column | Type | Meaning |
|---|---|---|
| `hour` | `TIMESTAMP`, primary key | Start of the hour, for example `2025-01-10T14:00:00`. |
| `community_produced` | `DOUBLE PRECISION`, not null | Total energy produced by the community during this hour. |
| `community_used` | `DOUBLE PRECISION`, not null | Part of user consumption covered by community energy. |
| `grid_used` | `DOUBLE PRECISION`, not null | Part of user consumption covered by external grid energy. |

Usage Service update rules:

```text
message.datetime = 2025-01-10T14:34:00
hour = 2025-01-10T14:00:00
```

Producer message:

```text
community_produced += message.kwh
```

User message:

```text
availableCommunityEnergy = community_produced - community_used
communityPart = min(message.kwh, max(availableCommunityEnergy, 0))
gridPart = message.kwh - communityPart

community_used += communityPart
grid_used += gridPart
```

Invariant:

```text
community_used <= community_produced
```

## `current_percentage`

Purpose:

- stores the latest calculated percentage values,
- is written by Current Percentage Service after a usage update message,
- is read by REST API for `/energy/current`,
- is cleared and rewritten when a new percentage value is calculated.

Columns:

| Column | Type | Meaning |
|---|---|---|
| `hour` | `TIMESTAMP`, primary key | Hour for which the percentage was calculated. |
| `community_depleted` | `DOUBLE PRECISION`, not null | Percentage of produced community energy that was used. |
| `grid_portion` | `DOUBLE PRECISION`, not null | Percentage of total consumption that came from the grid. |

Percentage formulas:

```text
community_depleted = community_used / community_produced * 100
```

If `community_produced = 0`, `community_depleted = 0`.

```text
grid_portion = grid_used / (community_used + grid_used) * 100
```

If `community_used + grid_used = 0`, `grid_portion = 0`.

Persisted percentage values are rounded to two decimal places.

## Timestamp Columns

The project mapping may mention technical audit columns such as `updated_at` or `calculated_at`. The current implementation intentionally omits those columns because the grading-critical data is:

- the hourly bucket,
- the aggregated kWh values,
- the calculated percentage values,
- the component responsibilities for writing and reading those values.

If technical audit timestamps become required, add a new Flyway migration instead of editing `V1__create_energy_tables.sql`.

## Component Responsibilities

| Component | Reads | Writes |
|---|---|---|
| Energy Producer | none | none |
| Energy User | none | none |
| Usage Service | `hourly_usage` | `hourly_usage` |
| Percentage Service | none (calculates from the update message) | `current_percentage` |
| REST API | `hourly_usage`, `current_percentage` | none |
| JavaFX GUI | none directly | none |

The REST API uses Spring Data repositories for reading, but it exposes no write endpoints and performs no core business calculations.
