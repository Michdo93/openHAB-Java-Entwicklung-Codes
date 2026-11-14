// Kapitel 15: Text wie einen Sprachbefehl auswerten lassen – ohne Mikrofon
import io.github.michdo93.openhab.*;
import java.util.stream.Collectors;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Voice voice = new Voice(client);
final Items items = new Items(client);
final Services services = new Services(client);

// Er erkennt Items an ihrem Label und antwortet in der gewählten Sprache
String sage(String satz) {
    Object antwort = voice.interpretText(satz, "de");
    if (!Json.isError(antwort)) {
        return antwort.toString();
    }
    String text = Json.error(antwort);
    return text.substring(text.lastIndexOf('(') + 1).replace(")", "");
}

void main(String[] args) throws InterruptedException {
    for (var i : Json.list(voice.getInterpreters("de"))) {
        IO.println("Interpreter %-8s %-26s Sprachen %s".formatted(i.get("id"),
                i.get("label"), i.get("locales")));
    }
    IO.println("Audio-Ausgänge: " + Json.list(new Audio(client).getSinks()).stream()
            .map(s -> (String) s.get("id")).collect(Collectors.joining(", ")));

    // Den eingebauten Interpreter zum Standard machen – eine Systemeinstellung
    Map<String,
            Object> config = Json.map(services.getServiceConfig("org.openhab.voice"));
    config.put("defaultHLI", "system");
    services.updateServiceConfig("org.openhab.voice", config);
    IO.println("Standard-Interpreter: "
            + Json.get(services.getServiceConfig("org.openhab.voice"), "defaultHLI"));

    items.sendCommand("Wohnzimmer_Licht", "OFF");
    items.sendCommand("Wohnzimmer_Dimmer", "60");
    for (String satz : List.of("Schalte das Deckenlicht ein",
            "Schalte das Deckenlicht ein",
            "Schalte die Leselampe aus", "Mach mir einen Kaffee")) {
        IO.println("  »" + satz + "« -> " + sage(satz));
    }
    Thread.sleep(500);
    IO.println("Deckenlicht: " + items.getItemState("Wohnzimmer_Licht")
            + " | Leselampe: " + items.getItemState("Wohnzimmer_Dimmer"));

    // Einen bestimmten Interpreter wählen, unabhängig vom Standard
    IO.println(voice.interpretTextBatch("turn off the Deckenlicht", List.of("system"),
            "en"));

    // Eigene Sätze direkt aus dem Terminal:
    // ./starte.sh kapitel15_oberflaechen/B04_Sprache.java "Schalte die Leselampe ein"
    for (String satz : args) {
        IO.println("  »" + satz + "« -> " + sage(satz));
    }
}
