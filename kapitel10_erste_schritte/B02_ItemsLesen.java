// Kapitel 10: Items lesen
import io.github.michdo93.openhab.*;
import java.util.concurrent.CompletableFuture;

void main() {
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    var client = new OpenHABClient(url, null, null, System.getenv("OPENHAB_TOKEN"));
    var items = new Items(client);

    // 1. Alle Items, auf drei Felder beschränkt (spart Daten)
    var alle = Json.list(items.getItems(Map.of("fields", "name,type,state")));
    IO.println(alle.size() + " Items gefunden:");
    alle.stream()
            .sorted(Comparator.comparing(i -> (String) i.get("name")))
            .forEach(i -> IO.println("  %-28s %-22s %s".formatted(
                    i.get("name"), i.get("type"), i.get("state"))));

    // 2. Ein einzelnes Item mit allen Details
    Map<String, Object> licht = Json.map(items.getItem("Wohnzimmer_Licht"));
    IO.println("\nLabel: " + licht.get("label")
            + " | Gruppen: " + licht.get("groupNames"));
    IO.println("Tags: " + licht.get("tags"));

    // 3. Nur der Zustand – als reiner Text
    IO.println("Temperatur: " + items.getItemState("Wohnzimmer_Temperatur"));

    // 4. Filter: alle Items eines Typs, alle mit einem Tag – gleichzeitig angefragt
    var asyncItems = new AsyncItems(client);
    CompletableFuture<Object> dimmer =
            asyncItems.getItems(Map.of("type", "Dimmer", "fields", "name"));
    CompletableFuture<Object> lichter =
            asyncItems.getItems(Map.of("tags", "Light", "fields", "name"));
    IO.println("Dimmer: " + namen(dimmer.join()));
    IO.println("Mit Tag 'Light': " + namen(lichter.join()));
}

String namen(Object antwort) {
    return Json.list(antwort).stream().map(i -> (String) i.get("name")).sorted()
            .collect(java.util.stream.Collectors.joining(", "));
}
