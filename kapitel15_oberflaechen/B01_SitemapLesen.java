// Kapitel 15: Sitemaps lesen – die Bedienseiten, die Apps und Basic UI zeigen
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Sitemaps sitemaps = new Sitemaps(client);

// Rekursiv: jedes Widget mit Typ, Beschriftung und Zustand, Unter-Widgets eingerückt
void zeige(Object widgets, int tiefe) {
    for (var w : Json.list(widgets)) {
        Object item = w.get("item");
        String wert = item == null ? "" : "= " + Json.get(item, "state");
        IO.println("%s%-12s %-28s %s".formatted("  ".repeat(tiefe), w.get("type"),
                w.get("label"), wert).stripTrailing());
        if (w.get("widgets") != null) {
            zeige(w.get("widgets"), tiefe + 1);
        }
    }
}

void main() {
    for (var s : Json.list(sitemaps.getSitemaps())) {
        IO.println("Sitemap " + s.get("name") + ": " + s.get("label"));
    }
    Object seite = Json.get(sitemaps.getSitemap("restbuch", null, null, false, "de"),
            "homepage");
    IO.println("\nStartseite »" + Json.get(seite, "title") + "«:");
    zeige(Json.get(seite, "widgets"), 1);
}
