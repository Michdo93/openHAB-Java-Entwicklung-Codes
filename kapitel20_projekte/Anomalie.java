// Kapitel 20: Auffällige Messwerte in der Persistenz erkennen und als Bericht ablegen.
// Verfahren: gleitender Median und robuste Streuung (MAD). Ein Wert gilt als
// Ausreißer, wenn er mehr als SCHWELLE robuste Standardabweichungen vom Median seiner
// Nachbarn abweicht. Das Ergebnis geht als Text an openHAB und als Datei in berichte/.
import io.github.michdo93.openhab.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final int FENSTER = 8;              // Nachbarn links und rechts
final double SCHWELLE = 6.0;        // robuste Standardabweichungen
final String DIENST = "inmemory";   // Testdaten aus Kapitel 14; im Alltag z. B. "rrd4j"
final DateTimeFormatter UHRZEIT = DateTimeFormatter.ofPattern("dd.MM., HH:mm")
        .withZone(ZoneId.systemDefault());

record Messwert(Instant zeit, double wert) {}

record Treffer(Instant zeit, double wert, double mitte, double abstand) {}

double median(List<Double> werte) {
    List<Double> s = werte.stream().sorted().toList();
    int m = s.size() / 2;
    return s.size() % 2 == 1 ? s.get(m) : (s.get(m - 1) + s.get(m)) / 2;
}

List<Messwert> ladeVerlauf(Persistence persistence, String item, int stunden) {
    Instant ende = Instant.now(), start = ende.minus(Duration.ofHours(stunden));
    Object antwort = persistence.getItemPersistenceData(item, DIENST, start.toString(),
            ende.toString());
    List<Messwert> reihe = new ArrayList<>();
    for (var p : Json.list(Json.get(antwort, "data"))) {
        long ms = ((Number) p.get("time")).longValue();
        reihe.add(new Messwert(Instant.ofEpochMilli(ms),
                Double.parseDouble((String) p.get("state"))));
    }
    return reihe;
}

List<Treffer> findeAusreisser(List<Messwert> reihe) {
    List<Treffer> treffer = new ArrayList<>();
    for (int i = 0; i < reihe.size(); i++) {
        List<Double> nachbarn = new ArrayList<>();
        int von = Math.max(0, i - FENSTER);
        int bis = Math.min(reihe.size() - 1, i + FENSTER);
        for (int j = von; j <= bis; j++) {
            if (j != i) {
                nachbarn.add(reihe.get(j).wert());
            }
        }
        if (nachbarn.size() < FENSTER) {
            continue;
        }
        double mitte = median(nachbarn);
        double mad = median(nachbarn.stream().map(x -> Math.abs(x - mitte)).toList())
                * 1.4826;
        double abstand = Math.abs(reihe.get(i).wert() - mitte) / (mad > 0 ? mad : 0.05);
        if (abstand > SCHWELLE) {
            treffer.add(new Treffer(reihe.get(i).zeit(), reihe.get(i).wert(), mitte,
                    abstand));
        }
    }
    return treffer;
}

void main() throws Exception {
    var client = new OpenHABClient(
            System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
            null, null, System.getenv("OPENHAB_TOKEN"));
    var persistence = new Persistence(client);
    var items = new Items(client);

    String jetzt = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss")
            .format(LocalDateTime.now());
    List<String> bericht = new ArrayList<>();
    bericht.addAll(List.of("# Auffällige Messwerte – " + jetzt, ""));
    for (String item : List.of("Werkstatt_Temperatur", "Wohnzimmer_Temperatur")) {
        List<Messwert> reihe = ladeVerlauf(persistence, item, 48);
        List<Treffer> treffer = findeAusreisser(reihe);
        IO.println(item + ": " + reihe.size() + " Werte, " + treffer.size()
                + " Auffälligkeit(en)");
        bericht.addAll(List.of("## " + item, "",
                reihe.size() + " Werte, " + treffer.size() + " auffällig", ""));
        for (Treffer t : treffer) {
            String zeile = String.format(Locale.ROOT,
                    "%s  %.1f °C  (erwartet ~%.1f, %.0f sigma)",
                    UHRZEIT.format(t.zeit()), t.wert(), t.mitte(), t.abstand());
            IO.println("   " + zeile);
            bericht.add("- " + zeile);
        }
        bericht.add("");
        if (!treffer.isEmpty()) {
            Treffer t = treffer.getLast();
            String uhr = DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault()).format(t.zeit());
            items.postUpdate("Haus_Status", String.format(Locale.ROOT,
                    "Auffällig: %s %.1f °C um %s", item, t.wert(), uhr));
        }
    }
    Path datei = Path.of("berichte", "anomalien-" + LocalDate.now() + ".md");
    Files.createDirectories(datei.getParent());
    Files.write(datei, bericht);
    IO.println("Bericht: " + datei);
}
