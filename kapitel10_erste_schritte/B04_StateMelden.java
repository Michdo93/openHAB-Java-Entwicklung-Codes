// Kapitel 10: States melden
import io.github.michdo93.openhab.*;
import java.time.Instant;

void main() {
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    var client = new OpenHABClient(url, null, null, System.getenv("OPENHAB_TOKEN"));
    var items = new Items(client);

    // Ein Messwert mit Einheit, eine Feuchte und ein Zeitstempel
    IO.println(items.postUpdate("Werkstatt_Temperatur", "18.4 °C"));
    IO.println(items.postUpdate("Werkstatt_Feuchte", "56 %"));
    IO.println(items.postUpdate("Werkstatt_LetzteMeldung", Instant.now().toString()));

    // Ein Contact kennt keine Commands – nur States
    IO.println(items.postUpdate("Flur_Haustuer", "OPEN"));

    // Einheiten werden umgerechnet: Fahrenheit rein, Celsius raus
    IO.println(items.postUpdate("Wohnzimmer_Temperatur", "71.6 °F"));

    for (String name : List.of("Werkstatt_Temperatur", "Werkstatt_Feuchte",
                               "Flur_Haustuer", "Wohnzimmer_Temperatur")) {
        IO.println("%-22s = %s".formatted(name, items.getItemState(name)));
    }

    // Für die weiteren Beispiele wieder 21.5 °C setzen
    items.postUpdate("Wohnzimmer_Temperatur", "21.5 °C");
}
