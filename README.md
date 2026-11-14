# openHAB & Java-Entwicklung – Begleit-Repository

Beispiele zum Buch „openHAB & Java-Entwicklung“ (Reihe „openHAB REST API“). Alle Programme
verwenden [java-openhab-rest-client](https://github.com/Michdo93/java-openhab-rest-client)
und, für die Tests, [java-openhab-test-suite](https://github.com/Michdo93/java-openhab-test-suite)
in Version 2.0.0 direkt über JitPack.

## Voraussetzungen

- JDK 25 (z. B. `sudo apt install openjdk-25-jdk`) und Maven 3.9
- openHAB 5 mit der Übungskonfiguration aus `openhab-conf/` (Kapitel 1 des Buchs)
- ein API-Token (Main UI → Profil → API-Tokens)

## Installation

```bash
git clone https://github.com/Michdo93/openHAB-Java-Entwicklung-Codes.git
cd openHAB-Java-Entwicklung-Codes
mvn dependency:copy-dependencies     # Bibliotheken nach lib/
cp .env.example .env                 # Adresse und Token eintragen
./starte.sh kapitel10_erste_schritte/B01_Verbinden.java
```

Die Beispiele sind Einzeldateien (Java 25, „compact source files“) und werden direkt aus dem
Quelltext gestartet; `starte.sh` lädt `.env` und setzt den Klassenpfad `lib/*`.
Die Datei `.env` steht in `.gitignore` – Tokens gehören nicht ins Repository.

## Maven

| Befehl | Wirkung |
|---|---|
| `mvn dependency:copy-dependencies` | Client, Test-Suite und JavaFX nach `lib/` kopieren |
| `mvn test` | Unit- und Integrationstests aus Kapitel 19 (Integrationstests nur mit `OPENHAB_TOKEN`) |
| `mvn package` | `target/ohwerkzeuge.jar` mit `ohcli`, Backend und MCP-Server (Kapitel 20) |

## Inhalt

| Ordner | Inhalt |
|---|---|
| `kapitel08_java/` | Werte und Typen, Kontrollfluss, Methoden, Klassen und Records, Fehler und JSON, Collections und Streams, Generics und Muster |
| `kapitel09_vertiefung/` | Threads und Executor, CompletableFuture, Beobachter-Muster, Dateien, HttpClient, Prozesse und Signale |
| `kapitel10_erste_schritte/` | Verbinden, Items lesen, Commands, States, Fehler, parallele Anfragen, optionale Parameter |
| `kapitel11_items/` | Items anlegen, viele Items, Metadaten, semantisches Modell, Gruppen |
| `kapitel12_things/` | Add-ons, Thing-Typen, Discovery und Inbox, Things, Links, Actions |
| `kapitel13_regeln/` | Regeln lesen, anlegen, steuern, Zeitpläne und Skripte, Modultypen |
| `kapitel14_daten/` | Persistenz, Testdaten, Verlauf als SVG, Daten löschen, Transformationen, CSV-Export |
| `kapitel15_oberflaechen/` | Sitemaps, UI-Komponenten, Sitemap-Ereignisse, Sprache, System |
| `kapitel16_ereignisse/` | SSE roh, Ereignisketten, Ereignisarten, State-Tracker, robuster Listener, Verteiler |
| `kapitel17_philosophie1/ohapp/` | Anwendung nach MVC: Model, Controller, Konsole und JavaFX-Oberfläche |
| `kapitel18_philosophie2/` | Werkstattgerät (Paket `werkstatt`), der Rechner als Gerät (mit systemd-Einheit), Bedienung |
| `kapitel19_tests/` | Unit- und Integrationstests mit JUnit 5 und der Test-Suite, Vorab-Test |
| `kapitel20_projekte/` | `ohcli`, Backend mit Bedienseite, Anomalieerkennung, MCP-Server |
| `openhab-conf/` | Übungskonfiguration (Items, Things, Regeln, Sitemap, Persistenz) |
| `.github/workflows/` | GitHub Actions: Tests bei jedem Push |

Programme aus mehreren Dateien starten ebenfalls direkt aus dem Quelltext:

```bash
./starte.sh kapitel17_philosophie1/ohapp/Main.java --gui
./starte.sh kapitel18_philosophie2/werkstatt/WerkstattGeraet.java --intervall=10
```

Die Projekte aus Kapitel 20 laufen aus dem JAR:

```bash
mvn package
java -jar target/ohwerkzeuge.jar list --tag Light       # ohcli
java -cp target/ohwerkzeuge.jar backend.Backend          # Backend auf http://127.0.0.1:3000/
java -cp target/ohwerkzeuge.jar mcp.OpenHABMcp           # MCP-Server (stdio)
```

## Zum Buch

„openHAB & Java-Entwicklung – Desktop-Anwendungen, Dienste und Backends mit der openHAB REST API,
Server-Sent Events und dem java-openhab-rest-client – von JavaFX bis zum eigenen Backend“
von Michael Christian Dörflinger, Reihe „openHAB REST API“.

Lizenz: siehe `LICENSE`.
