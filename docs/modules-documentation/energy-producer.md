# Energy Producer Module

## Purpose

`energy-producer` is an independently startable Spring Boot application. It simulates community energy production and publishes `PRODUCER` messages to RabbitMQ.

It does not read or write PostgreSQL and does not call the Usage Service directly.

## Tech Stack

| Area | Implementation |
|---|---|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Messaging | Spring AMQP, `RabbitTemplate`, JSON converter |
| Scheduling | Spring `@Scheduled` |
| Weather data | Open-Meteo current solar radiation |
| HTTP client | Java built-in `HttpClient` inside `WeatherClient` |

## Main Components

| Class / Package | Responsibility |
|---|---|
| `EnergyProducerApplication` | Spring Boot entry point, scheduling enabled, AMQP JSON converter bean. |
| `messaging/EnergyMessage` | Service-local DTO published to RabbitMQ. Fields: `type`, `association`, `kwh`, `datetime`. |
| `scheduling/EnergyProducerScheduler` | Periodically waits a short random delay and triggers production publishing. |
| `service/EnergyProducerService` | Creates producer messages and publishes them to `energy_queue`. |
| `weather/WeatherClient` | Reads the current solar radiation (W/m²) from Open-Meteo. Returns `0` if the API cannot be reached, so the producer keeps running offline. |
| `weather/WeatherProductionCalculator` | Converts solar radiation into a plausible kWh value: more sun means more energy, plus a small bounded random variation. |

## Configuration

File: `energy-producer/src/main/resources/application.properties`

| Property | Current Value / Meaning |
|---|---|
| HTTP port | none; this module is a RabbitMQ publisher |
| `app.queue.name` | `energy_queue` |
| `app.scheduling.fixed-delay-ms` | `1000`; combined with a randomized `0-3999 ms` wait, events are published every `1-5` seconds. |
| `app.production.min-kwh` / `max-kwh` | `0.001` / `0.006`; minute-scale production calculator bounds. |
| `app.weather.latitude` / `longitude` | `48.2082` / `16.3738` (Vienna); location used for the Open-Meteo request. |
| `spring.autoconfigure.exclude` | Excludes JDBC/JPA autoconfiguration because this module must not use the database. |

## Runtime Flow

```mermaid
flowchart LR
    Scheduler["EnergyProducerScheduler<br/>@Scheduled"]
    Service["EnergyProducerService"]
    Weather["WeatherClient<br/>Open-Meteo solar radiation"]
    Calculator["WeatherProductionCalculator"]
    DTO["EnergyMessage<br/>type=PRODUCER"]
    Rabbit["RabbitMQ<br/>energy_queue"]

    Scheduler --> Service
    Service --> Weather
    Weather --> Calculator
    Calculator --> DTO
    DTO --> Rabbit
```

## Message Contract

Published queue: `energy_queue`

```json
{
  "type": "PRODUCER",
  "association": "COMMUNITY",
  "kwh": 0.0047,
  "datetime": "2026-05-15T14:33:00"
}
```

The contract is documented in `docs/message-contract.md`.

## Start Command

```powershell
cd energy-producer
.\mvnw.cmd spring-boot:run
```

If the Open-Meteo API cannot be reached, `WeatherClient` returns `0` W/m² and the producer keeps publishing at the minimum kWh value, so no extra configuration is needed to run offline.

## Verification

```powershell
cd energy-producer
.\mvnw.cmd clean package
```

Behavior to confirm during the smoke test (`docs/smoke-test.md`):

- Message has `type=PRODUCER`.
- Message has `association=COMMUNITY`.
- kWh is weather-dependent, randomly varied, and bounded.
- JSON fields match the documented RabbitMQ contract.
- No database dependency is configured.
