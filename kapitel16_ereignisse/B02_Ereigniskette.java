// Kapitel 16: Welche Ereignisse löst ein einziger Command aus?
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final String ZIEL = "Wohnzimmer_Licht";

void main() {
    Thread.startVirtualThread(() -> {
        try {
            Thread.sleep(1000);
            String neu = "ON".equals(items.getItemState(ZIEL)) ? "OFF" : "ON";
            IO.println(">>> sende Command " + neu + "\n");
            items.sendCommand(ZIEL, neu);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    });

    // Alle Ereignisse dieses Items: Topic openhab/items/Wohnzimmer_Licht/*
    try (SSEConnection strom = new Events(client)
            .getEvents("openhab/items/" + ZIEL + "/*")) {
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
            strom.close();                            // nach drei Sekunden schließen
        });
        for (var e : strom.events()) {                // payload ist schon entpackt
            String[] topic = ((String) e.get("topic")).split("/");
            IO.println("%-24s %-13s %s".formatted(e.get("type"),
                    topic[topic.length - 1],
                    Json.get(e, "payload", "value")));
        }
    }
}
