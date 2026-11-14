// Kapitel 15: Eine Sitemap als UI-Komponente erzeugen
import io.github.michdo93.openhab.*;
import java.util.stream.Collectors;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final UI ui = new UI(client);
final Sitemaps sitemaps = new Sitemaps(client);
final String UID = "buch_tablet";

Map<String, Object> widget(String component, String item, Map<String, Object> config) {
    Map<String, Object> c = new LinkedHashMap<>(config);
    c.put("item", item);
    return Map.of("component", component, "config", c);
}

Map<String, Object> rahmen(String label, Object... widgets) {
    return Map.of("component", "Frame", "config", Map.of("label", label),
            "slots", Map.of("widgets", List.of(widgets)));
}

void main() {
    Map<String, Object> tablet = Map.of(
            "uid", UID, "component", "Sitemap", "config",
                    Map.of("label", "Tablet im Flur"),
            "slots", Map.of("widgets", List.of(
                    rahmen("Licht",
                            widget("Switch", "Wohnzimmer_Licht",
                                    Map.of("label", "Decke")),
                            widget("Slider", "Wohnzimmer_Dimmer",
                                    Map.of("label", "Leselampe")),
                            widget("Switch", "gLichter", Map.of("label", "Alle Lichter",
                                    "mappings", List.of("OFF=Alle aus")))),
                    rahmen("Haus",
                            widget("Text", "Flur_Haustuer", Map.of("label", "Haustür")),
                            widget("Selection", "Haus_Status", Map.of("label", "Modus",
                                    "mappings", List.of("Alle zu Hause=Zu Hause",
                                            "Kino=Kino", "Gute Nacht=Nacht")))))));

    ui.deleteUIComponent("system:sitemap", UID);             // falls schon vorhanden
    IO.println("anlegen: "
            + Json.get(ui.addUIComponent("system:sitemap", tablet), "uid"));
    IO.println("Sitemaps jetzt: " + Json.list(sitemaps.getSitemaps()).stream()
            .map(s -> (String) s.get("name")).collect(Collectors.joining(", ")));
    for (var frame : Json.list(Json.get(sitemaps.getSitemap(UID), "homepage",
            "widgets"))) {
        IO.println("  " + frame.get("label") + ": "
                + Json.list(frame.get("widgets")).stream()
                .map(w -> w.get("label") + " (" + w.get("type") + ")")
                .collect(Collectors.joining(", ")));
    }
    IO.println("UI-Komponenten: "
            + Json.list(ui.getUIComponents("system:sitemap")).stream()
            .map(k -> (String) k.get("uid")).collect(Collectors.joining(", ")));
    // Die Sitemap bleibt bestehen – B03_SitemapEreignisse nutzt sie.
}
