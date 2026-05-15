# Projekt-Setup: Energy Community

Willkommen im Team. Dieses Projekt ist ein verteiltes System (SOA), das die Energieproduktion und den Verbrauch einer Gemeinschaft simuliert.

## 1. Voraussetzungen

Bevor ihr den Code ausfuehrt, muessen folgende Tools installiert sein:

- Java JDK 25
- IntelliJ IDEA
- Docker Desktop
- Maven

## 2. Erstmaliges Setup

### A. Repository und IntelliJ

1. Repository klonen.
2. Den Hauptordner `energy-community-project` in IntelliJ oeffnen.
3. Im Maven-Tab nacheinander die `pom.xml` Dateien der Service-/App-Unterordner hinzufuegen:
   - `energy-gui`
   - `energy-producer`
   - `energy-user`
   - `percentage-service`
   - `rest-api`
   - `usage-service`
4. Falls `src/main/java` nicht als Source Root markiert ist: Rechtsklick auf den Ordner, dann `Mark Directory as` -> `Sources Root`.

### B. Service-Vertraege

Die Services sind bewusst unabhaengige Maven-Projekte. Es gibt keine gemeinsame `shared` Compile-Time-Abhaengigkeit mehr.

REST- und RabbitMQ-Payloads werden als JSON-Vertraege dokumentiert:

- `docs/message-contract.md`

Wenn ein Message-Feld geaendert wird, muss zuerst der Vertrag dokumentiert und danach die service-lokalen DTOs in den betroffenen Komponenten angepasst werden.

## 3. Infrastruktur starten

Im Hauptverzeichnis:

```bash
docker-compose up -d
```

Ports:

- PostgreSQL: `5432`
- RabbitMQ Java clients: `5672`
- RabbitMQ Management UI: `http://localhost:15672` mit `guest` / `guest`

## 4. Komponenten

- `energy-producer`: Simuliert Solar/Windkraft basierend auf Wetter-Daten bzw. einem Wetter-Faktor.
- `energy-user`: Simuliert Stromverbrauch basierend auf Tageszeit mit Peaks morgens und abends.
- `usage-service`: Berechnet Community-Nutzung und Grid-Anteil aus Producer/User Messages.
- `percentage-service`: Berechnet die aktuelle Auslastung in Prozent fuer das GUI.
- `rest-api`: Liest Daten aus PostgreSQL und stellt HTTP-Endpunkte fuer das GUI bereit.
- `energy-gui`: JavaFX-Dashboard zur Visualisierung der Live-Daten.

## 5. System starten

### 1. Docker-Infrastruktur starten

```bash
docker-compose up -d
```

### 2. Spring Boot Microservices starten

Startreihenfolge:

1. `usage-service` und `percentage-service`
2. `energy-producer` und `energy-user`
3. `rest-api`

Alternative per Skript unter Windows:

```powershell
.\start-services.ps1
```

### 3. GUI starten

```bash
cd energy-gui
mvn compile javafx:run
```

## 6. Module bauen

Im Hauptverzeichnis:

```powershell
$modules = @("energy-gui", "energy-producer", "energy-user", "percentage-service", "rest-api", "usage-service"); foreach ($mod in $modules) { Write-Host "Building $mod..."; cd $mod; mvn clean install -DskipTests; if ($LASTEXITCODE -ne 0) { Write-Host "Fehler beim Bauen von $mod!" -ForegroundColor Red; exit }; cd .. }
```

Das Skript baut alle Module nacheinander und stoppt beim ersten Fehler.
