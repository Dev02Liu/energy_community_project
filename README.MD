# ⚡ Projekt-Setup: Energy Community

Willkommen im Team! Dieses Projekt ist ein verteiltes System (SOA), das die Energieproduktion und den Verbrauch einer Gemeinschaft simuliert. Damit alles reibungslos läuft, folgt bitte dieser Anleitung.

---

## 1. Voraussetzungen (Prerequisites)
Bevor ihr den Code ausführt, müsst ihr folgende Tools installiert haben:

* **Java JDK 25 (LTS):** Wir nutzen die aktuellste Version. Prüft eure Version im Terminal mit `java -version`.
* **IntelliJ IDEA:** (Community oder Ultimate Edition).
* **Docker Desktop:** Essenziell für die Datenbank und die Message-Queue.
* **Maven:** In IntelliJ integriert, wird für die Abhängigkeiten genutzt.

---

## 2. Erstmaliges Setup (Step-by-Step)

### A. Repository & IntelliJ
1. Klon das Repository auf deinen Rechner.
2. Öffne den **Hauptordner** (`energy-community-project`) in IntelliJ.
3. **Maven-Module verknüpfen:** * Öffne den **Maven-Tab** auf der rechten Seite.
    * Klicke auf das **+** Symbol.
    * Wähle nacheinander die `pom.xml` Dateien aus allen 6 Unterordnern und der Library aus.
4. **Ordner-Farben:** Falls die `src/main/java` Ordner nicht blau sind: Rechtsklick auf den Ordner -> *Mark Directory as* -> *Sources Root*.

### B. Die Shared Library (WICHTIG!)
Da alle Services das gleiche Nachrichtenformat nutzen, müssen wir die gemeinsame Bibliothek zuerst lokal registrieren:
1. Suche im Maven-Tab das Modul `shared`.
2. Gehe zu **Lifecycle** -> Doppelklick auf **install**.
3. Erst wenn hier "BUILD SUCCESS" steht, finden die anderen Projekte die Klasse `EnergyMessage`.

---

## 3. Infrastruktur starten (Docker)
Wir müssen nicht mühsam Datenbanken installieren. Docker erledigt das für uns.

1. Öffne ein Terminal im Hauptverzeichnis des Projekts.
2. Starte die Container mit:
   ```bash
   docker-compose up -d
3. Check: * Datenbank: Läuft auf Port 5432
  RabbitMQ (Das "Postamt"): Web-Interface unter http://localhost:15672 (Login: guest / guest).

## 4. **Die 6 Komponenten**

* **energy-producer:** Simuliert Solar/Windkraft basierend auf Wetter-Daten (Zufallsintervall 1-5 Sek.).
* **energy-user:** Simuliert Stromverbrauch basierend auf der Tageszeit, d.h. Peaks morgens & abends (Zufallsintervall 1-5 Sek.).
* **usage-service:** Berechnet, ob der Community-Pool reicht oder das Stromnetz (Grid) einspringen muss.
* **percentage-service:** Berechnet die aktuelle Auslastung in Prozent für das GUI.
* **rest-api:** Liest die echten Daten via Spring Data JPA aus der PostgreSQL-Datenbank und stellt sie als HTTP-Endpoints für die GUI bereit.
* **energy-gui:** JavaFX-Dashboard zur Visualisierung der Live-Daten.
* **(shared:** Klassen die von anderen Komponenten verwendet werden.)

## 5. **Workflow: So startest du das System**

Um das komplette System laufen zu lassen und Datenflüsse zu sehen, gehe am besten in dieser Reihenfolge vor:

### 1. Docker-Infrastruktur starten
Öffne ein Terminal im Hauptverzeichnis und starte die Services (Datenbank & RabbitMQ):
```bash
docker-compose up -d
```

### 2. Spring Boot Microservices starten
Starte nacheinander die folgenden Services in IntelliJ (über den Play-Button der jeweiligen `Application`-Klasse) oder im Terminal mit `mvn spring-boot:run` in den Unterordnern:

> 🛠️ **Alternative per Skript (Windows):**  
> Führe im Hauptverzeichnis einfach das Skript `.\start-services.ps1` aus. Dieses öffnet automatisch für jeden der 5 Backend-Microservices ein neues PowerShell-Fenster und startet ihn mit `mvnw.cmd spring-boot:run`.

1. **Dienste empfangen:** `usage-service` & `percentage-service` (Warten auf eingehende Daten)
2. **Daten generieren:** `energy-producer` & `energy-user` (Fangen an, Messages an RabbitMQ zu senden)
3. **Daten bereitstellen:** `rest-api` (Stellt die generierten Daten via REST für das Frontend bereit)

### 3. Benutzeroberfläche (GUI) starten
Zum Schluss öffnest du das Frontend zur Visualisierung. Starte dazu im Projekt **`energy-gui`** die Klasse `MainApp.java` (über IntelliJ) oder navigiere im Terminal in den `energy-gui` Ordner und führe aus:
```bash
mvn compile javafx:run
```

## Pro-Tipp
Wenn du die Klasse EnergyMessage oder sonstige Klassen in der Shared-Library änderst, musst du im Verzeichnis `/shared` den Befehl `mvn clean install` (bzw. im Maven-Tab der Library auf `install`) ausführen, damit die anderen 6 Komponenten deine Änderungen übernehmen!

## Bauen der Module testen
mit diesem cmd
```bash
$modules = @("shared", "energy-gui", "energy-producer", "energy-user", "percentage-service", "rest-api", "usage-service"); foreach ($mod in $modules) { Write-Host "Building $mod..."; cd $mod; mvn clean install -DskipTests; if ($LASTEXITCODE -ne 0) { Write-Host "Fehler beim Bauen von $mod!" -ForegroundColor Red; exit }; cd .. }
```

in base_directory ausführen und schauen, ob es fehlerlos baut. Das Skript baut alle Module nacheinander und stoppt, wenn ein Fehler auftritt. So kannst du schnell überprüfen, ob alle Module korrekt gebaut werden können.