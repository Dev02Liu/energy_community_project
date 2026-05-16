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
| Weather data | Open-Meteo client with fallback mode |
| HTTP client | Java HTTP client inside weather adapter |
| Tests | JUnit 5, AssertJ, Mockito, Spring Boot test support |

## Main Components

| Class / Package | Responsibility |
|---|---|
| `EnergyProducerApplication` | Spring Boot entry point, scheduling enabled, AMQP JSON converter bean. |
| `messaging/EnergyMessage` | Service-local DTO published to RabbitMQ. Fields: `type`, `association`, `kwh`, `datetime`. |
| `scheduling/EnergyProducerScheduler` | Periodically triggers production publishing when `app.scheduling.enabled=true`. |
| `scheduling/SimulationDelayProvider` | Abstraction for simulation delay. |
| `scheduling/RandomSimulationDelayProvider` | Adds randomized delay before each published event. |
| `service/EnergyProducerService` | Creates producer messages and publishes them to `energy_queue`. |
| `weather/WeatherClient` | Weather data abstraction. |
| `weather/OpenMeteoWeatherClient` | Reads current weather from Open-Meteo. |
| `weather/FallbackWeatherClient` | Local fallback weather source. |
| `weather/ResilientWeatherClient` | Uses Open-Meteo and falls back when remote weather fails. |
| `weather/WeatherProductionCalculator` | Converts weather snapshot into plausible kWh production. |
| `weather/WeatherSnapshot` | Immutable weather input model for production calculation. |

## Configuration

File: `energy-producer/src/main/resources/application.properties`

| Property | Current Value / Meaning |
|---|---|
| `server.port` | `8081` |
| `app.queue.name` | `energy_queue` |
| `app.scheduling.enabled` | Enables/disables scheduled message publishing. |
| `app.scheduling.fixed-delay-ms` | Scheduler fixed delay before the randomized simulation wait. |
| `app.production.min-kwh` / `max-kwh` | Production calculator bounds. |
| `app.weather.mode` | `open-meteo` by default; can be overridden to `fallback`. |
| `spring.autoconfigure.exclude` | Excludes JDBC/JPA autoconfiguration because this module must not use the database. |

## Runtime Flow

```mermaid
flowchart LR
    Scheduler["EnergyProducerScheduler<br/>@Scheduled"]
    Service["EnergyProducerService"]
    Weather["WeatherClient<br/>Open-Meteo or fallback"]
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
  "kwh": 18.7,
  "datetime": "2026-05-15T14:33:00"
}
```

The contract is documented in `docs/message-contract.md` and protected by `EnergyMessageContractTest`.

## Start Command

```powershell
cd energy-producer
.\mvnw.cmd spring-boot:run
```

Fallback weather mode:

```powershell
cd energy-producer
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.weather.mode=fallback"
```

## Verification

```powershell
cd energy-producer
.\mvnw.cmd test
```

Important checks:

- Message has `type=PRODUCER`.
- Message has `association=COMMUNITY`.
- kWh is weather-dependent and bounded.
- JSON fields match the documented RabbitMQ contract.
- No database dependency is configured.

