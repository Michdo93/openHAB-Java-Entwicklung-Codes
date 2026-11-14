// Kapitel 14: Gespeicherte Werte gezielt löschen
import io.github.michdo93.openhab.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Persistence persistence = new Persistence(client);
final String DIENST = "inmemory";
final String ITEM = "Werkstatt_Temperatur";
final Instant JETZT = Instant.now();

Object anzahl() {
    return Json.get(persistence.getItemPersistenceData(ITEM, DIENST,
            JETZT.minus(3, ChronoUnit.DAYS).toString(), JETZT.toString()),
                    "datapoints");
}

void main() {
    IO.println("vorher:  " + anzahl());
    // Nur die Nacht von 0 bis 6 Uhr von gestern löschen
    var gestern = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault());
    String von = gestern.toInstant().toString();
    String bis = gestern.plusHours(6).toInstant().toString();
    IO.println(persistence.deleteItemData(ITEM, von, bis, DIENST));
    IO.println("nachher: " + anzahl());

    // rrd4j lehnt das Löschen ab – genau wie das Schreiben
    String text = Json.error(persistence.deleteItemData(ITEM, von, JETZT.toString(),
            "rrd4j"));
    IO.println("rrd4j: " + text.substring(text.lastIndexOf('(') + 1).replace(")", ""));
}
