// Kapitel 10: Optionale Parameter – Überladungen, alle Parameter, Options-Map
import io.github.michdo93.openhab.*;

void main() {
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    var client = new OpenHABClient(url, null, null, System.getenv("OPENHAB_TOKEN"));
    var items = new Items(client);

    // 1. kurze Überladung: nur die ersten Parameter, der Rest hat Vorgabewerte
    IO.println("Switch-Items:  " + anzahl(items.getItems("Switch")));
    // 2. alle Parameter in der Reihenfolge der Python-Bibliothek, null = Vorgabe
    //    getItems(type, tags, metadata, recursive, fields, staticDataOnly,
    //             language, parents)
    IO.println("Licht, Felder: "
            + items.getItems(null, "Light", null, null, "name", null, null, null));
    // 3. Options-Map: nur die Parameter, die man braucht – mit Namen
    IO.println("Licht, Map:    "
            + items.getItems(Map.of("tags", "Light", "fields", "name")));

    // Tippfehler im Namen einer Option fallen sofort auf
    try {
        items.getItems(Map.of("tag", "Light"));
    } catch (IllegalArgumentException e) {
        IO.println("Tippfehler:    " + e.getMessage().split(", allowed")[0]);
    }

    // Json hilft beim Auswerten: Pfade, Typen, Fehler
    Object item = items.getItem("Wohnzimmer_Temperatur");
    IO.println("Muster:        " + Json.get(item, "stateDescription", "pattern"));
    IO.println("erstes Tag:    " + Json.get(item, "tags", 0));
    IO.println("fehlt:         " + Json.get(item, "metadata", "unit"));
}

int anzahl(Object antwort) {
    return Json.list(antwort).size();
}
