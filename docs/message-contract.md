# RabbitMQ Message Contract

Services communicate through JSON payloads on RabbitMQ. DTO classes are service-local and intentionally not shared through a common Maven dependency. This keeps all six applications independently buildable and limits coupling to the documented JSON contract.

## Topology

The current implementation uses durable direct queues:

```text
Energy Producer -> energy_queue -> Usage Service
Energy User     -> energy_queue -> Usage Service
Usage Service   -> percentage_update_queue -> Percentage Service
```

| Queue | Publisher | Consumer | Payload |
|---|---|---|---|
| `energy_queue` | `energy-producer`, `energy-user` | `usage-service` | Energy Message |
| `percentage_update_queue` | `usage-service` | `percentage-service` | Hourly Usage Updated Message |

## Why Direct Queues Are Used

RabbitMQ is used to decouple the runtime services asynchronously:

- Producer/User do not call Usage Service directly.
- Usage Service does not call Percentage Service directly.
- Messages can be buffered by RabbitMQ while a consumer is temporarily slower.
- Each service can be started independently as long as the required infrastructure is running.

The direct-queue design is sufficient here because each stream currently has exactly one consumer:

- all production/usage events belong to Usage Service,
- all usage-update events belong to Percentage Service.

The lecture material also covers exchanges and routing keys. If the system later needs multiple consumers for the same event stream, introduce an exchange and separate queues, for example:

```text
Exchange: energy.events.topic
Routing keys:
- energy.producer.created
- energy.user.created
- energy.usage.updated
```

That extension would keep the same message payloads but route through exchange bindings instead of publishing directly to queue names.

## Energy Message

Queue: `energy_queue`

Purpose: minute-level production or consumption event that `usage-service` aggregates into an hourly row.

Published by:

- `energy-producer`
- `energy-user`

Consumed by:

- `usage-service`

JSON shape:

```json
{
  "type": "PRODUCER",
  "association": "COMMUNITY",
  "kwh": 12.5,
  "datetime": "2026-05-15T09:30:00"
}
```

Fields:

| Field | Type | Required | Valid Values / Meaning |
|---|---|---|---|
| `type` | string | yes | `PRODUCER` or `USER` |
| `association` | string | yes | Current implementation accepts `COMMUNITY` |
| `kwh` | number | yes | Non-negative produced or requested energy amount |
| `datetime` | ISO-8601 local datetime string | yes | Event timestamp used for hourly aggregation |

Validation in `usage-service`:

- rejects null messages,
- rejects unknown types,
- rejects invalid associations,
- rejects null datetimes,
- rejects negative, NaN, or infinite kWh values,
- invalid messages do not write the database and do not publish update events.

## Producer Message Example

```json
{
  "type": "PRODUCER",
  "association": "COMMUNITY",
  "kwh": 18.7,
  "datetime": "2026-05-15T14:33:00"
}
```

Effect:

```text
hour = 2026-05-15T14:00:00
hourly_usage.community_produced += 18.7
```

## User Message Example

```json
{
  "type": "USER",
  "association": "COMMUNITY",
  "kwh": 5.0,
  "datetime": "2026-05-15T14:34:00"
}
```

Effect:

```text
hour = 2026-05-15T14:00:00
availableCommunityEnergy = communityProduced - communityUsed
communityPart = min(kwh, max(availableCommunityEnergy, 0))
gridPart = kwh - communityPart

communityUsed += communityPart
gridUsed += gridPart
```

There is no separate Grid message. Grid energy is treated as available fallback energy when community production is not sufficient.

## Hourly Usage Updated Message

Queue: `percentage_update_queue`

Purpose: derived event indicating that the hourly usage data changed and current percentages should be recalculated.

Published by:

- `usage-service`

Consumed by:

- `percentage-service`

JSON shape:

```json
{
  "hour": "2026-05-15T09:00:00"
}
```

Fields:

| Field | Type | Required | Description |
|---|---|---|---|
| `hour` | ISO-8601 local datetime string | yes | Start of the hour whose usage data was updated |

Processing order:

```text
Usage Service writes hourly_usage
Usage Service publishes Hourly Usage Updated Message
Percentage Service consumes update
Percentage Service reads hourly_usage and writes current_percentage
```

## Guardrails

- Do not add direct service method calls between producer/user, usage, and percentage services.
- Do not let producer/user write to PostgreSQL.
- Do not let Percentage Service consume producer/user messages directly.
- Update this document when a message field is added, removed, renamed, or changes meaning.
- Keep payloads backward-compatible where practical because services are independently startable.
