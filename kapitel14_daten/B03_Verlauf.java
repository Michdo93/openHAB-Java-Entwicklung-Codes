// Kapitel 14: Historische Werte abfragen, auswerten und als SVG-Datei zeichnen
import io.github.michdo93.openhab.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Persistence persistence = new Persistence(client);
final String DIENST = "inmemory";                        // Testdaten aus B02_Testdaten
final String ITEM = "Wohnzimmer_Temperatur";

record Punkt(Instant zeit, double wert) {}

// Ein Datenpunkt der Antwort: Zeit in Millisekunden, Zustand als Text
Punkt punkt(Map<String, Object> p) {
    long ms = ((Number) p.get("time")).longValue();
    double wert = Double.parseDouble((String) p.get("state"));
    return new Punkt(Instant.ofEpochMilli(ms), wert);
}

void main() throws Exception {
    Instant ende = Instant.now(), start = ende.minus(Duration.ofHours(24));
    Object antwort = persistence.getItemPersistenceData(ITEM, DIENST,
            Map.of("startTime", start.toString(), "endTime", ende.toString()));
    List<Punkt> punkte = Json.list(Json.get(antwort, "data")).stream()
            .map(this::punkt).toList();
    var statistik = punkte.stream().mapToDouble(Punkt::wert).summaryStatistics();
    IO.println(Json.get(antwort, "datapoints") + " Messwerte der letzten 24 Stunden");
    IO.println(String.format("Minimum %.1f °C   Maximum %.1f °C   Mittel %.1f °C",
            statistik.getMin(), statistik.getMax(), statistik.getAverage()));
    Punkt warm = punkte.stream().max(Comparator.comparingDouble(Punkt::wert))
            .orElseThrow();
    IO.println("am wärmsten um " + DateTimeFormatter.ofPattern("HH:mm")
            .format(warm.zeit().atZone(ZoneId.systemDefault())) + " Uhr");

    // Der Verlauf als SVG-Linie – ohne Diagramm-Bibliothek
    int b = 600, h = 160;
    double lo = statistik.getMin(), hi = statistik.getMax();
    double spanne = Duration.between(start, ende).toMillis();
    String linie = punkte.stream().map(p -> String.format(Locale.ROOT, "%.1f,%.1f",
                    Duration.between(start, p.zeit()).toMillis() / spanne * b,
                    h - (p.wert() - lo) / (hi - lo) * (h - 10) - 5))
            .collect(Collectors.joining(" "));
    String svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" \
            style="background:#fff">
              <polyline fill="none" stroke="#e64a19" stroke-width="2" points="%s"/>
              <text x="4" y="14" font-size="12">%.1f °C</text>
              <text x="4" y="%d" font-size="12">%.1f °C</text></svg>
            """.formatted(b, h, linie, hi, h - 4, lo);
    // im Browser oder in Inkscape öffnen
    Files.writeString(Path.of("verlauf.svg"), svg);
    IO.println("verlauf.svg geschrieben (" + svg.length() + " Zeichen)");

    // Dasselbe als Textdiagramm im Terminal: stündliche Werte als Zeichen
    String balken = " .:-=+*#%@";
    var zeile = new StringBuilder();
    for (int i = 0; i < punkte.size(); i += 4) {
        double anteil = (punkte.get(i).wert() - lo) / (hi - lo);
        zeile.append(balken.charAt((int) Math.round(anteil * 9)));
    }
    IO.println(zeile);

    // Seitenweise abrufen: 10 Werte pro Seite – sofern der Dienst Seiten unterstützt
    Object seite = persistence.getItemPersistenceData(ITEM, DIENST, Map.of(
            "startTime", start.toString(), "endTime", ende.toString(),
            "page", 0, "pageLength", 10));
    IO.println("Seite 0: " + Json.list(Json.get(seite, "data")).stream().limit(3)
            .map(p -> (String) p.get("state")).collect(Collectors.joining(", "))
            + " …");

    // Mit boundary liefert openHAB zusätzlich den Wert vor und nach dem Zeitraum
    Object rand = persistence.getItemPersistenceData(ITEM, DIENST, Map.of(
            "startTime", ende.minus(Duration.ofMinutes(5)).toString(),
            "endTime", ende.toString(), "boundary", true));
    IO.println("letzte 5 Minuten mit Rand: " + Json.get(rand, "datapoints") + " Werte");
}
