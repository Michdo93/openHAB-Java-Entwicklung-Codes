// Kapitel 14: Werte mit eigenem Zeitstempel in einen Persistenzdienst schreiben
// 48 Stunden synthetische Temperaturen (alle 15 Minuten) für den Dienst "inmemory" –
// einschließlich eines Ausreißers, den Kapitel 20 wiederfindet.
import io.github.michdo93.openhab.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Persistence persistence = new Persistence(client);
final AsyncPersistence async = new AsyncPersistence(client);
final String DIENST = "inmemory";

void main() {
    var rnd = new Random(42);  // fester Startwert: jeder Lauf erzeugt dieselben Daten
    Instant jetzt = Instant.now().truncatedTo(ChronoUnit.MINUTES);
    Map<String, Double> basis = new LinkedHashMap<>();
    basis.put("Wohnzimmer_Temperatur", 21.0);
    basis.put("Werkstatt_Temperatur", 17.0);

    basis.forEach((item, grund) -> {
        // alte Testdaten der letzten drei Tage entfernen
        persistence.deleteItemData(item, jetzt.minus(3, ChronoUnit.DAYS).toString(),
                jetzt.toString(), DIENST);
        // alle Werte vorbereiten und gleichzeitig schreiben
        List<CompletableFuture<Object>> anfragen = new ArrayList<>();
        for (int k = 48 * 4; k > 0; k--) {
            Instant zeit = jetzt.minus(15L * k, ChronoUnit.MINUTES);
            var lokal = zeit.atZone(ZoneId.systemDefault());
            double stunde = lokal.getHour() + lokal.getMinute() / 60.0;
            double wert = grund + 1.5 * Math.sin((stunde - 9) / 24 * 2 * Math.PI)
                    + 0.15 * rnd.nextGaussian();
            if (item.equals("Werkstatt_Temperatur") && k == 30) {
                wert += 6.0;                                     // ein Ausreißer
            }
            String zustand = String.format(Locale.ROOT, "%.2f °C", wert);
            anfragen.add(async.storeItemData(item, zeit.toString(), zustand, DIENST));
        }
        var fehler = anfragen.stream().map(CompletableFuture::join)
                .filter(Json::isError).toList();
        if (!fehler.isEmpty()) {
            throw new IllegalStateException(item + ": " + Json.error(fehler.get(0)));
        }
        IO.println(item + ": " + anfragen.size() + " Werte in »" + DIENST
                + "« geschrieben");
    });

    // Gegenprobe: rrd4j nimmt keine Werte von außen an
    Object antwort = persistence.storeItemData("Wohnzimmer_Temperatur",
            jetzt.toString(),
            "20 °C", "rrd4j");
    String text = Json.error(antwort);
    IO.println("Gegenprobe rrd4j: "
            + text.substring(text.lastIndexOf('(') + 1).replace(")", ""));
}
