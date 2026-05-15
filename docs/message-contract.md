# RabbitMQ Message Contract

Services communicate through JSON payloads on RabbitMQ. The Java classes are service-local DTOs; they intentionally are not shared through a Maven module. This keeps the services independently buildable and limits coupling to the documented message contract.

## Energy Message

Queue: `energy_queue`

Purpose: minute-level production/usage event that is aggregated by `usage-service` into hourly rows.

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

| Field | Type | Required | Description |
|---|---|---|---|
| `type` | string | yes | `PRODUCER` for generated energy or `USER` for consumed energy. |
| `association` | string | yes | Energy community identifier. Current implementation uses `COMMUNITY`. |
| `kwh` | number | yes | Produced or consumed energy amount in kilowatt-hours. |
| `datetime` | ISO-8601 local date-time string | yes | Event timestamp used for hourly aggregation. |

Valid examples:

- Producer: `type=PRODUCER`, `association=COMMUNITY`, `kwh` is produced energy.
- User: `type=USER`, `association=COMMUNITY`, `kwh` is requested/used energy.

## Hourly Usage Updated Message

Queue: `percentage_update_queue`

Purpose: derived event indicating that usage data for an hour changed and current percentages should be recalculated.

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
| `hour` | ISO-8601 local date-time string | yes | Start of the hour whose usage data was updated. |

## Guardrails

- Do not add a compile-time dependency between services just to share these DTOs.
- Update this document when a message field is added, removed, renamed, or changes meaning.
- Keep payloads backward-compatible where practical because services may be deployed independently.
- A direct queue is acceptable while exactly one service consumes the stream. If multiple services must receive the same events, introduce an exchange and per-service queues instead of competing consumers on one work queue.
