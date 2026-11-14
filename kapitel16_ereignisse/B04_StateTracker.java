// Kapitel 16: Der State-Tracker – nur ausgewählte Items, fertig formatiert
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Events events = new Events(client);
final Items items = new Items(client);
final List<String> SEITE_1 = List.of("Wohnzimmer_Dimmer", "Wohnzimmer_Temperatur");
final List<String> SEITE_2 = List.of("Werkstatt_Temperatur", "Werkstatt_Feuchte");

// Die aktuelle Anzeige je Item – ein Programm würde hier sein Display neu zeichnen
final Map<String, Object> anzeigen = new LinkedHashMap<>();

void bedienen(String verbindung, SSEConnection strom) throws InterruptedException {
    Thread.sleep(1000);
    items.sendCommand("Wohnzimmer_Dimmer", "55");
    items.postUpdate("Werkstatt_Temperatur", "19.3 °C");  // (noch) nicht abonniert
    Thread.sleep(1000);
    IO.println("--- Ansicht gewechselt: neue Item-Liste");
    events.updateSSEConnectionItems(verbindung, SEITE_2);
    Thread.sleep(1000);
    items.postUpdate("Werkstatt_Temperatur", "19.8 °C");
    Thread.sleep(1000);
    strom.close();
}

void main() {
    SSEConnection strom = events.initiateStateTracker();
    String verbindung = null;
    for (String zeile : strom.lines()) {
        if (verbindung == null && zeile.startsWith("data:")) {
            // 1. Nachricht: die Verbindungskennung
            String id = zeile.substring(5).trim();
            verbindung = id;
            IO.println("Verbindung: " + id);
            events.updateSSEConnectionItems(id, SEITE_1);
            Thread.startVirtualThread(() -> {
                try {
                    bedienen(id, strom);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            continue;
        }
        Map<String, Object> daten = SSE.parseSSEData(zeile);
        if (daten == null || "ALIVE".equals(daten.get("type"))) {
            continue;
        }
        daten.forEach((name, z) -> {
            Object anzeige = Json.get(z, "displayState");  // formatiert wie in der UI
            if (anzeige == null) {
                anzeige = Json.get(z, "state");
            }
            IO.println("  %-22s state=%-12s anzeige=%s".formatted(name,
                    Json.get(z, "state"), anzeige));
            anzeigen.put(name, anzeige);
        });
    }
    IO.println("zuletzt angezeigt: " + anzeigen.values());
}
