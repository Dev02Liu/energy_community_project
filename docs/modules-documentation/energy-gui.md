# JavaFX GUI Module

## Purpose

`energy-gui` is an independently startable JavaFX desktop application. It is the user-facing client for current percentage data and historical usage data.

It communicates only with the REST API over HTTP. It has no PostgreSQL, JPA, or RabbitMQ dependency.

## Tech Stack

| Area | Implementation |
|---|---|
| Runtime | Java 25 |
| UI | JavaFX 25.0.2 |
| Layout | FXML (`energy-view.fxml`) loaded via `FXMLLoader` |
| HTTP | Java `HttpClient` |
| JSON | FasterXML Jackson `ObjectMapper` |
| Build | Maven, JavaFX Maven plugin |

## Main Components

| Class / Package | Responsibility |
|---|---|
| `MainApp` | JavaFX launcher entry point configured in Maven. |
| `app/EnergyGuiApplication` | Loads `energy-view.fxml` via `FXMLLoader`, injects the client through the controller factory, and shows the scene. |
| `view/energy-view.fxml` | Declares the JavaFX layout (labels, buttons, `DatePicker`/`ComboBox`). Linked to the controller via `fx:controller`. |
| `controller/EnergyDashboardController` | Handles `@FXML` button actions, fills the hour dropdowns, combines date/hour, and updates labels from the REST responses. |
| `client/EnergyApiClient` | HTTP client for REST API calls. |
| `dto/CurrentPercentageDTO` | DTO for `/energy/current`. |
| `dto/HistoricalSummaryDTO` | DTO for `/energy/historical`: aggregated totals returned by the REST API. |

## Configuration

The GUI calls the REST API at `http://localhost:8080`, which is the fixed local review/demo setup.

## Startup Without REST

The GUI is independently startable even when the REST API is unavailable. The window opens
first, then the initial `/energy/current` call runs asynchronously. If the REST API is down,
the request fails and the labels show `Error fetching data` instead of crashing. The same
applies to the refresh and show-data actions, so the GUI degrades gracefully.

## UI Behavior

Current data:

- Refresh button calls `GET /energy/current`.
- GUI displays community pool depletion and grid portion as formatted labels.

Historical data:

- User selects the start/end **date** from a `DatePicker` (calendar) and the **hour** (`00:00`–`23:00`) from a `ComboBox` dropdown. The hour list is fixed in code (0–23), not loaded from the database. The GUI combines date + hour into a `LocalDateTime` and sends it as ISO to the REST API.
- Show-data button calls `GET /energy/historical?start=...&end=...`.
- The REST API returns the aggregated totals for the range, which the GUI displays as labels:
  - community produced,
  - community used,
  - grid used.

Current implementation note: the totals are aggregated server-side by the REST API and shown as labels, not in a `TableView`.

## Runtime Flow

```mermaid
flowchart LR
    User["User"]
    Controller["EnergyDashboardController"]
    Client["EnergyApiClient<br/>Java HttpClient"]
    API["REST API<br/>localhost:8080"]
    CurrentDTO["CurrentPercentageDTO"]
    HistoryDTO["HistoricalSummaryDTO"]
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
    C->>A: fetchHistoricalData(start, end)
    A->>R: GET /energy/historical
    R-->>A: HistoricalSummaryDTO
    A-->>C: CompletableFuture result
    C->>UI: update historical labels from summary
```

## Start Command

The GUI starts independently. The REST API should be running for live data; if it is not, the
GUI still opens and shows `Error fetching data` (see *Startup Without REST*).

```powershell
cd energy-gui
..\energy-producer\mvnw.cmd -f pom.xml javafx:run
```

## Verification

```powershell
cd ..
.\energy-producer\mvnw.cmd -f .\energy-gui\pom.xml clean package
```

Manual GUI check:

1. Start Docker, Usage Service, Percentage Service, REST API, Producer, and User.
2. Start the GUI.
3. Click refresh and verify current percentage labels.
4. Enter a date range that includes generated data.
5. Click show data and verify historical aggregate labels.
6. Confirm no database credentials or direct database connection are used by the GUI.
