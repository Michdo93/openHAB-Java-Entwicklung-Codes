// Kapitel 8: Ausnahmen, eigene Ausnahmeklassen, Optional und JSON
// Braucht die Bibliothek (Json): ./starte.sh kapitel08_java/B05_FehlerUndJson.java
import io.github.michdo93.openhab.Json;

// Eine eigene, ungeprüfte Ausnahme
class MesswertFehler extends RuntimeException {
    MesswertFehler(String text) {
        super(text);
    }
}

double prozent(String text) {
    double wert;
    try {
        wert = Double.parseDouble(text);
    } catch (NumberFormatException e) {             // abfangen und übersetzen
        throw new MesswertFehler("keine Zahl: " + text);
    }
    if (wert < 0 || wert > 100) {
        throw new MesswertFehler("außerhalb 0–100: " + wert);
    }
    return wert;
}

// Optional: ein Ergebnis, das fehlen darf – statt null
Optional<String> raumVon(String item) {
    int i = item.indexOf('_');
    return i > 0 ? Optional.of(item.substring(0, i)) : Optional.empty();
}

void main() throws Exception {
    for (String text : List.of("55", "120", "viel")) {
        try {
            IO.println("OK: " + prozent(text));
        } catch (MesswertFehler e) {
            IO.println("Fehler: " + e.getMessage());
        } finally {
            IO.println("  (geprüft: " + text + ")");
        }
    }

    IO.println(raumVon("Wohnzimmer_Licht").orElse("?") + " / "
            + raumVon("Licht").orElse("?"));

    // Geprüfte Ausnahmen (z. B. IOException) muss man fangen oder weitergeben –
    // main gibt sie hier mit "throws Exception" weiter.
    // try-with-resources schließt die Ressource auch im Fehlerfall:
    try (var leser = new BufferedReader(new StringReader("Zeile 1\nZeile 2"))) {
        IO.println("gelesen: " + leser.readLine());
    }

    // JSON ohne Zusatzbibliothek: java-openhab-rest-client bringt Json mit
    String text = """
            {"name": "Werkstatt_Temperatur", "state": "18.5 °C",
             "tags": ["Measurement", "Temperature"]}""";
    Map<String, Object> item = Json.map(Json.parse(text));
    List<Object> tags = Json.array(item.get("tags"));
    IO.println(item.get("name") + " hat " + tags.size() + " Tags");
    item.put("state", "19 °C");
    IO.println(Json.stringify(item));
    IO.println(Json.stringify(Map.of("ok", true), 2));
    try {
        Json.parse("{kaputt");
    } catch (IllegalArgumentException e) {
        IO.println("JSON-Fehler: " + e.getMessage());
    }
}
