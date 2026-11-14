// Kapitel 16: Ein Strom für das ganze Programm – Ereignisse an Beobachter verteilen
import io.github.michdo93.openhab.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);

// Ein Verteiler liest EINEN Strom und meldet jedes Item an seine Beobachter
static class ItemVerteiler {
    private final Map<String, List<BiConsumer<String, Object>>> beobachter =
            new ConcurrentHashMap<>();
    private final OpenHABClient client;
    private volatile SSEConnection strom;

    ItemVerteiler(OpenHABClient client) {
        this.client = client;
    }

    // name = Itemname oder "*" für alle, die alles wissen wollen
    void on(String name, BiConsumer<String, Object> wer) {
        beobachter.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(wer);
    }

    private void melde(String schluessel, String name, Object payload) {
        beobachter.getOrDefault(schluessel, List.of())
                .forEach(b -> b.accept(name, payload));
    }

    void start() {
        strom = new ItemEvents(client).ItemStateChangedEvent();
        for (var e : strom.events()) {
            String name = ((String) e.get("topic")).split("/")[2];
            melde(name, name, e.get("payload"));
            melde("*", name, e.get("payload"));
        }
    }

    void stop() {
        if (strom != null) {
            strom.close();
        }
    }
}

void main() throws InterruptedException {
    var verteiler = new ItemVerteiler(client);

    // Beliebig viele Programmteile melden sich für "ihr" Item an
    BiConsumer<String, String> beobachte = (name, beschriftung) ->
            verteiler.on(name,
                    (n, p) -> IO.println("%-16s %s -> %s".formatted(beschriftung,
                    Json.get(p, "oldValue"), Json.get(p, "value"))));
    beobachte.accept("Wohnzimmer_Licht", "Deckenlicht");
    beobachte.accept("Wohnzimmer_Dimmer", "Leselampe");
    beobachte.accept("Wohnzimmer_Dimmer", "Leselampe (2)");   // derselbe Strom
    var anzahl = new AtomicInteger();
    verteiler.on("*", (n, p) -> anzahl.incrementAndGet());

    Thread leser = Thread.startVirtualThread(verteiler::start);
    Thread.sleep(1000);
    items.sendCommand("Wohnzimmer_Licht", "OFF");
    items.sendCommand("Wohnzimmer_Dimmer", "45");
    Thread.sleep(3000);
    verteiler.stop();
    leser.join();
    IO.println(anzahl.get() + " Ereignisse über einen einzigen Strom verteilt");
}
