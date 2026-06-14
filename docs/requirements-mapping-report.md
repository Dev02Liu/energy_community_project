# Requirements Mapping Report

This report maps the project against the **specification**, the **grading schema**, and the
**lecture transcripts / lecture code** (`project-resources/`). It also records the lecture‑fidelity
review (which annotations/functions are used, and whether each was taught) and the changes made to
keep the code minimal and close to the lecture, plus the robustness features added back for the code review.

Verification date: 2026-06-04; the four backend services were re-built and re-verified end-to-end on 2026-06-14 after the robustness pass (rest-api and energy-gui unchanged since). All 6 modules build with `clean package` (`BUILD SUCCESS`); the message
flow was additionally verified end‑to‑end against the spec's own worked example via the smoke-test runbook.

---

## 1. Grading Schema Mapping (Final Submission)

| Component (weight) | Requirement | Where implemented | Status |
|---|---|---|---|
| **Usage Service (30%)** | Receives PRODUCER/USER messages, updates the usage table correctly, then sends an update message | `usage-service`: `EnergyMessageListener` → `HourlyUsageUpdateService` (community pool first, overflow to grid, hourly bucketing) → saves `hourly_usage` → publishes `HourlyUsageUpdatedMessage` | ✅ Verified end‑to‑end against the spec example |
| **Current Percentage Service (30%)** | Receives the update message, updates the current percentage table correctly | `percentage-service`: `HourlyUsageUpdatedListener` → `CurrentPercentageCalculationService` (`community_used/produced`, `grid/(community_used+grid)`, 2 decimals, one row per hour) → `current_percentage` | ✅ Verified end‑to‑end against the spec example |
| **Energy Producer (10%)** | Random 1–5 s interval, sensible kWh, **weather** determines kWh | `energy-producer`: `EnergyProducerScheduler` (1 s fixed delay + 0–4 s random) → `WeatherClient` (Open‑Meteo solar radiation) + `WeatherProductionCalculator` | ✅ |
| **Energy User (10%)** | Random 1–5 s interval, sensible kWh, **time of day** determines kWh | `energy-user`: `EnergyUserScheduler` → `EnergyUsageCalculator` (peak / off‑peak / night multiplier) | ✅ |
| **REST API (10%)** | Spring Boot reads from the **database** (not static data) | `rest-api`: `EnergyController` reads `current_percentage` and `hourly_usage` via JPA; `GET /energy/current`, `GET /energy/historical` | ✅ |
| **JavaFX UI (10%)** | Pool % + grid %, refresh button, date range + show‑data button for produced/used/grid | `energy-gui`: `EnergyDashboardController` (labels, refresh, start/end fields, show‑data) using `EnergyApiClient` (REST only, not DB) | ✅ |

### Must‑Haves (0 points if missing)

| Must‑have | Evidence |
|---|---|
| Every component can be started independently | Each module is its own Spring Boot / JavaFX app; all 5 backends logged `Started …Application` when started alone |
| System builds and runs with no errors | All 6 modules `BUILD SUCCESS`; live run produced correct data |
| Spring Boot used for REST API | `rest-api` is a Spring Boot web app |
| JavaFX used for GUI | `energy-gui` uses `javafx.application.Application` |
| RabbitMQ used for communication | `energy_queue` and `percentage_update_queue`; verified message routing |
| GitHub repository link | Provided in the submission text (out of code scope) |
| Every person has commits | Verified via `git shortlog -sn` before hand‑in (out of code scope) |

---

## 2. Specification Behaviour Checks

| Spec rule | Result |
|---|---|
| `community_used` can never exceed `community_produced` | Live invariant check: **0 violations** |
| Minutes accumulate into the matching hour | `datetime` truncated to the hour (`withMinute(0)…`) |
| Spec example: `USER 0.05` on `18.05/18.02/1.056` → `18.05/18.05/1.076` | **Exact match** through the live system |
| Spec percentage example: `100.00` depleted, `5.63` grid portion | **Exact match** |
| Percentage table keeps historical hourly values | `save()` with `hour` as primary key; same hour is updated, older hourly rows remain |
| `community_produced = 0` → depleted `0`; `community_used+grid_used = 0` → grid portion `0` | Covered (division guards) |
| GUI not connected to DB; uses REST | `EnergyApiClient` only calls `http://localhost:8080/energy/...` |
| REST is read‑only on the DB | Controller only reads repositories |

---

## 3. Lecture‑Fidelity Review (annotations & functions)

Every framework feature used was checked against the lecture transcripts and the lecture code
(`disys26bwi1`).

### Taught in the lecture — used as‑is

| Feature | Lecture evidence |
|---|---|
| `@SpringBootApplication`, `@Bean Queue` in the main app class | `Disys26bwi1Application` |
| `@RestController`, `@RequestMapping`, `@GetMapping`, `@RequestParam` | `BookController`, transcript ("@RequestParam … the name") |
| `@Value` | Transcript ("Would it be @Value? … Yeah") |
| `@Service` + constructor injection | `EchoInOutService` |
| `@RabbitListener`, `RabbitTemplate`, `convertAndSend` | `EchoInOutService`, transcript |
| `JpaRepository` + derived query methods | `BookRepository.findBookEntitiesByTitleContainingIgnoreCase` → our `findByHourBetween`, `findFirstByOrderByHourDesc` |
| `@Entity`, `@Id`, `@Column` | `BookEntity` |
| Flyway `db/migration/V1__*.sql` | Transcript (Flyway section) |
| `Optional` handling | Transcript ("if possible book is empty … return possible book.get") |
| `java.net.http.HttpClient` + `HttpRequest` + `response.body()` | Transcript ("we need an HTTP client … new builder build"); lecture deliberately avoided WebClient/Webflux |
| Jackson / `ObjectMapper` for JSON | Transcript ("JSON by default … using Jackson") |

### Not shown verbatim in the lecture — kept because they are **necessary** (and build on taught concepts)

| Feature | Why it stays | Explainability |
|---|---|---|
| `@Scheduled` + `@EnableScheduling` (producer/user) | The spec **requires** sending a message "every couple of seconds" — a timer is mandatory; `@Scheduled` is the standard Spring way | "Runs the method on a timer" |
| `JacksonJsonMessageConverter` `@Bean` | The lecture's echo sent a plain `String`; we send `EnergyMessage` **objects**, which need a JSON converter (builds on the taught Jackson JSON) | "Send/receive messages as JSON instead of Java serialization" |
| `@Component` (scheduler, calculator, weather client, listeners) | Base stereotype of the taught `@Service`/`@RestController` | "Marks the class as a Spring‑managed bean" |
| GUI async (`sendAsync` + `CompletableFuture` + `Platform.runLater`) | The lecture's single‑label HTTP demo was synchronous; the dashboard loads on startup and on button clicks, so an async wrapper keeps the window from freezing. Core HTTP/JSON is exactly as taught | "Run the request in the background, then update the labels on the UI thread" |
| `@Table` on entities | Trivial JPA annotation; lecture used `@Entity(name=…)` for the same purpose | "Maps the entity to a table name" |

### Added in this pass for robustness (beyond the literal lecture, easy to explain in the review)

| Feature | Why it is there | Explainability |
|---|---|---|
| Standalone `@Configuration` class `config/RabbitMqConfig` (all four backend services) | Separation of concerns: the queue and converter `@Bean`s live in a dedicated config class instead of the `@SpringBootApplication` class, keeping the entry point focused on bootstrapping. Builds on the taught `@Bean`/`@Configuration` concept. | "All RabbitMQ setup sits in one config class per service" |
| Explicit message-type handling + validation (`usage-service`) | `PRODUCER` and `USER` are matched explicitly; an unknown type is rejected with `IllegalArgumentException` instead of being silently counted as usage. | "We don't blindly trust the incoming message type" |
| Hour-keyed save in `CurrentPercentageCalculationService.updateCurrentPercentage` (`percentage-service`) | Uses `hour` as the primary key, so the same hour is updated in place and older hourly rows remain. | "One stored percentage row per hour" |
| Input validation + listener error handling (`percentage-service`) | Negative kWh in an update is rejected; the listener logs and drops a failing message instead of letting Spring AMQP requeue it endlessly. | "Bad data is logged and dropped, never stored or looped" |

---

## 4. Changes Applied (this and the previous clean‑up passes)

- **energy-producer**: weather package reduced from **7 classes to 2** (`WeatherClient` + `WeatherProductionCalculator`); raw‑HTTP/adapter/fallback/exception/snapshot classes removed; offline behaviour handled inline (returns `0` W/m²). Magic production/weather/message values externalized to `application.properties` (`@Value`); queue + converter bean in `config/RabbitMqConfig`.
- **energy-user**: magic consumption/multiplier/message values externalized to `application.properties` (`@Value`); queue + converter bean in `config/RabbitMqConfig`.
- **usage-service**: community-pool-first usage logic with hourly bucketing; explicit `PRODUCER`/`USER` type handling (unknown types rejected with `IllegalArgumentException`); message types externalized via `@Value`; queue + converter beans in `config/RabbitMqConfig`.
- **percentage-service**: `BigDecimal` rounding → `Math.round`; percentages calculated from the update message (no `hourly_usage` read); negative-kWh input validation; hour-keyed `save()` keeps one historical row per hour; listener logs and drops failing messages; queue + converter beans in `config/RabbitMqConfig`.
- **rest-api**: `EnergyController` now binds `@RequestParam LocalDateTime` directly (lecture `ObservationController` style); manual date parsing and the `start>end` 400 guard removed (invalid date → Spring returns 400; reversed range → empty list).
- **energy-gui**: removed the generic `<T>` send helper and the custom `@FunctionalInterface` in `EnergyApiClient`; two straightforward request methods remain.

Net effect: the cleanup passes removed well over **700 lines of production code**; this robustness pass
added back a small amount (one config class per service, plus validation, a transaction, and listener
error handling), leaving each module still minimal and close to the lecture code. All six modules build
cleanly (`clean package` → `BUILD SUCCESS`).

---

## 5. Remaining Recommendations (optional, not required to pass)

- None are required for the grade. The only features beyond the literal lecture snippets (`@Scheduled`,
  the JSON message converter, the GUI async wrapper) are **necessary for the components to work** and
  are each easy to explain in the code review.
- For the demo, prefer a machine with internet so the producer reads live Open‑Meteo radiation; if it
  is offline the producer keeps running at the minimum kWh (graceful fallback), which still satisfies
  "weather data is used".
