// Kapitel 16: Den Ereignisstrom direkt lesen – Zeile für Zeile
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));

// Eine Aufgabe nach einer Wartezeit in einem virtuellen Thread ausführen
void spaeter(long ms, Runnable aufgabe) {
    Thread.startVirtualThread(() -> {
        try {
            Thread.sleep(ms);
            aufgabe.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    });
}

void main() {
    // Zum Ausprobieren: nach einer Sekunde den Dimmer verstellen
    String wert = String.valueOf(20 + new Random().nextInt(60));
    spaeter(1000, () -> new Items(client).sendCommand("Wohnzimmer_Dimmer", wert));

    // Alle Zustandsänderungen aller Items (Topic openhab/items/*/statechanged)
    SSEConnection strom = new ItemEvents(client).ItemStateChangedEvent();
    spaeter(15000, strom::close);                        // 15 Sekunden lang lesen
    IO.println("Warte auf Ereignisse …");
    for (String zeile : SSE.iterSSELines(strom)) {  // jede Zeile, sobald sie da ist
        IO.println("roh: " + zeile.substring(0, Math.min(70, zeile.length())));
        if (!zeile.startsWith("data:")) {
            continue;                                    // event:-, id:- und Leerzeilen
        }
        // 1. JSON: das Ereignis
        var ereignis = Json.map(Json.parse(zeile.substring(5)));
        if ("ALIVE".equals(ereignis.get("type"))) {
            continue;                                    // Lebenszeichen, kein Ereignis
        }
        Object payload = Json.parse((String) ereignis.get("payload"));  // 2. JSON
        String name = ((String) ereignis.get("topic")).split("/")[2];
        IO.println("  -> " + name + ": " + Json.get(payload, "oldValue") + " -> "
                + Json.get(payload, "value"));
    }
    IO.println("beendet");                       // close() beendet die Schleife
}
