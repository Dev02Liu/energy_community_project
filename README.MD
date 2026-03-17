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

* energy-producer: Simuliert Solar/Windkraft basierend auf einer Wetter-API.
* energy-user: Simuliert Stromverbrauch (Peak-Zeiten morgens & abends).
* usage-service: Berechnet, ob der Community-Pool reicht oder das Stromnetz (Grid) einspringen muss.
* percentage-service: Berechnet die aktuelle Auslastung in Prozent für das GUI.
* rest-api: Stellt die Daten via HTTP-Endpoints für die GUI bereit.
* energy-gui: JavaFX-Dashboard zur Visualisierung der Live-Daten.
* shared: Klassen die von anderen Komponenten verwendet werden.

## 5. **Workflow: So startest du das System**

Um Datenflüsse zu sehen, starte die Komponenten am besten in dieser Reihenfolge:
Docker (Infrastruktur bereitstellen).
usage-service & percentage-service (Warten auf Daten).
energy-producer & energy-user (Starten den Datenfluss).
rest-api (Macht die Daten abrufbar).
energy-gui (Zeigt alles an).

## Pro-Tipp
Wenn du die Klasse EnergyMessage oder sonstige Klassen in der Shared-Library änderst, musst du im Maven-Tab der Library erneut auf install klicken, damit die anderen 6 Komponenten die Änderung übernehmen!
