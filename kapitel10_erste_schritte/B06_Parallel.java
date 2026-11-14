// Kapitel 10: Viele Anfragen gleichzeitig – mit virtuellen Threads und AsyncItems
import io.github.michdo93.openhab.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

void main() throws Exception {
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    var client = new OpenHABClient(url, null, null, System.getenv("OPENHAB_TOKEN"));
    var items = new Items(client);
    List<String> namen = List.of("Wohnzimmer_Licht", "Wohnzimmer_Dimmer",
            "Wohnzimmer_Temperatur", "Kueche_Rollladen", "Flur_Haustuer",
            "Werkstatt_Temperatur", "Haus_Status");
    items.getItemState("Haus_Status");               // Verbindung aufwärmen

    // nacheinander: jede Anfrage wartet auf die vorige
    long start = System.nanoTime();
    for (String name : namen) {
        items.getItemState(name);
    }
    long nacheinander = (System.nanoTime() - start) / 1_000_000;

    // gleichzeitig: ein virtueller Thread je Anfrage
    start = System.nanoTime();
    List<Future<Object>> zustaende = new ArrayList<>();
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
        for (String name : namen) {
            zustaende.add(pool.submit(() -> items.getItemState(name)));
        }
    }
    long gleichzeitig = (System.nanoTime() - start) / 1_000_000;

    // dasselbe mit der Klasse AsyncItems
    var asyncItems = new AsyncItems(client);
    List<CompletableFuture<Object>> f =
            namen.stream().map(asyncItems::getItemState).toList();
    CompletableFuture.allOf(f.toArray(CompletableFuture[]::new)).join();

    for (int i = 0; i < namen.size(); i++) {
        IO.println("%-24s %-16s %s".formatted(
                namen.get(i), zustaende.get(i).get(), f.get(i).join()));
    }
    IO.println("nacheinander: " + nacheinander + " ms");
    IO.println("gleichzeitig: " + gleichzeitig + " ms");
}
