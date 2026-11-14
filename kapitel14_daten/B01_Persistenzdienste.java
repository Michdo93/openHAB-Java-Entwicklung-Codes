// Kapitel 14: Persistenzdienste und die Items, die sie speichern
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Persistence persistence = new Persistence(client);

void main() {
    for (var dienst : Json.list(persistence.getServices("de"))) {
        IO.println("%-9s %-10s %s".formatted(dienst.get("id"), dienst.get("type"),
                dienst.get("label")));
    }
    // Was liegt im Arbeitsspeicher-Dienst – und seit wann?
    for (var e : Json.list(persistence.getItemsFromService("inmemory")).subList(0, 6)) {
        IO.println("  %-24s %4s Werte, ältester %s".formatted(e.get("name"),
                e.getOrDefault("count", "?"), e.getOrDefault("earliest", "–")));
    }
    // rrd4j kann keine Itemliste liefern – die Bibliothek erklärt den Grund
    IO.println("rrd4j: " + persistence.getItemsFromService("rrd4j"));
}
