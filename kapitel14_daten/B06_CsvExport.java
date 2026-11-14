// Kapitel 14: Den Verlauf eines Items als CSV-Datei exportieren – in Zeitabschnitten
import io.github.michdo93.openhab.*;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Persistence persistence = new Persistence(client);

void main(String[] args) throws Exception {
    String item = args.length > 0 ? args[0] : "Wohnzimmer_Temperatur";
    int stunden = args.length > 1 ? Integer.parseInt(args[1]) : 48;
    String dienst = args.length > 2 ? args[2] : "inmemory";

    // Ein gepufferter Writer: Zeilen gehen blockweise auf die Platte,
    // nichts sammelt sich im Speicher
    Path datei = Path.of(item + ".csv");
    int zeilen = 0;
    try (BufferedWriter csv = Files.newBufferedWriter(datei)) {
        csv.write("zeit;wert;einheit\n");
        // Lange Zeiträume in Abschnitten von 6 Stunden abfragen: kleine Antworten
        Duration abschnitt = Duration.ofHours(6);
        Instant ende = Instant.now();
        for (Instant von = ende.minus(Duration.ofHours(stunden)); von.isBefore(ende);
                von = von.plus(abschnitt)) {
            Instant bis = von.plus(abschnitt)
                    .isBefore(ende) ? von.plus(abschnitt) : ende;
            Object antwort = persistence.getItemPersistenceData(item, dienst,
                    von.toString(), bis.minusMillis(1).toString());
            if (Json.isError(antwort)) {
                throw new IllegalStateException(Json.error(antwort));
            }
            Object einheit = Json.get(antwort, "unit");
            for (var p : Json.list(Json.get(antwort, "data"))) {
                Instant zeit = Instant.ofEpochMilli(((Number) p.get("time"))
                        .longValue());
                csv.write(zeit + ";" + ((String) p.get("state")).replace(".", ",") + ";"
                        + (einheit == null ? "" : einheit) + "\n");
                zeilen++;
            }
        }
    }
    IO.println(zeilen + " Werte (" + stunden + " h, " + dienst + ") nach " + datei
            + " geschrieben");
}
