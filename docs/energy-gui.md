# JavaFX GUI Module

## Purpose

`energy-gui` is an independently startable JavaFX desktop application. It is the user-facing client for current percentage data and historical usage data.

It communicates only with the REST API over HTTP. It has no PostgreSQL, JPA, or RabbitMQ dependency.

## Tech Stack

| Area | Implementation |
|---|---|
| Runtime | Java 25 |
| UI | JavaFX 25.0.2 |
| HTTP | Java `HttpClient` |
| JSON | FasterXML Jackson `ObjectMapper` |
| Build | Maven, JavaFX Maven plugin |
| Tests | JUnit 5, AssertJ |

## Main Components

| Class / Package | Responsibility |
|---|---|
| `MainApp` | JavaFX launcher entry point configured in Maven. |
| `app/EnergyGuiApplication` | Creates the JavaFX scene and wires the controller/client. |
| `controller/EnergyDashboardController` | Builds UI controls, handles button actions, validates input, updates labels. |
| `client/EnergyApiClient` | HTTP client for REST API calls. |
| `client/EnergyApiException` | Controlled API error type. |
| `dto/CurrentPercentageDTO` | DTO for `/energy/current`. |
| `dto/HistoricalUsageDTO` | DTO for `/energy/historical`. |
| `service/EnergyValueFormatter` | Formats percentages and kWh values for labels. |

## UI Behavior

Current data:

- Refresh button calls `GET /energy/current`.
- GUI displays community pool depletion and grid portion as formatted labels.

Historical data:

- User enters start/end date in `dd.MM.yyyy HH:mm` or ISO local datetime.
- Show-data button calls `GET /energy/historical?start=...&end=...`.
- GUI sums returned hourly rows and displays aggregate labels:
  - community produced,
  - community used,
  - grid used.

Current implementation note: historical rows are not displayed in a `TableView`; they are aggregated into labels.

## Runtime Flow

```mermaid
flowchart LR
    User["User"]
    Controller["EnergyDashboardController"]
    Client["EnergyApiClient<br/>Java HttpClient"]
    API["REST API<br/>localhost:8080"]
    CurrentDTO["CurrentPercentageDTO"]
    HistoryDTO["HistoricalUsageDTO[]"]
    Labels["JavaFX Labels"]

    User --> Controller
    Controller --> Client
    Client -->|"GET /energy/current"| API
    Client -->|"GET /energy/historical"| API
    API --> CurrentDTO
    API --> HistoryDTO
    CurrentDTO --> Controller
    HistoryDTO --> Controller
    Controller --> Labels
```

## Sequence Diagram

```mermaid
sequenceDiagram
    participant U as User
    participant C as EnergyDashboardController
    participant A as EnergyApiClient
    participant R as REST API
    participant UI as JavaFX Labels

    U->>C: click refresh
    C->>A: fetchCurrentPercentage()
    A->>R: GET /energy/current
    R-->>A: CurrentPercentageDTO
    A-->>C: CompletableFuture result
    C->>UI: Platform.runLater update current labels

    U->>C: enter range and click show data
    C->>C: validate date range
    C->>A: fetchHistoricalUsage(start, end)
    A->>R: GET /energy/historical
    R-->>A: HistoricalUsageDTO[]
    A-->>C: CompletableFuture result
    C->>UI: sum rows and update historical labels
```

## Start Command

The REST API must be running first.

```powershell
cd energy-gui
..\energy-producer\mvnw.cmd -f pom.xml javafx:run
```

## Verification

```powershell
cd ..
.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml test
```

Manual GUI check:

1. Start Docker, Usage Service, Percentage Service, REST API, Producer, and User.
2. Start the GUI.
3. Click refresh and verify current percentage labels.
4. Enter a date range that includes generated data.
5. Click show data and verify historical aggregate labels.
6. Confirm no database credentials or direct database connection are used by the GUI.

