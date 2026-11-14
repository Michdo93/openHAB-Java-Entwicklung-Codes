// Kapitel 11: Einen ganzen Raum mit einem einzigen Aufruf anlegen
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);

// Hilfsmethode: die Felder, die jedes Item hat, plus beliebige weitere
Map<String, Object> item(String type, String name, String label, List<String> gruppen,
                         List<String> tags, Map<String, Object> mehr) {
    Map<String, Object> i = new LinkedHashMap<>();
    i.put("type", type);
    i.put("name", name);
    i.put("label", label);
    i.put("groupNames", gruppen);
    i.put("tags", tags);
    i.putAll(mehr);
    return i;
}

void main() {
    List<Map<String, Object>> gartenhaus = List.of(
            item("Group", "gGartenhaus", "Gartenhaus", List.of("gHaus"),
                    List.of("Garden"), Map.of()),
            item("Switch", "Gartenhaus_Licht", "Licht",
                    List.of("gGartenhaus", "gGartenhaus_Lichter"),
                    List.of("Switch", "Light"), Map.of("category", "light")),
            item("Number:Temperature", "Gartenhaus_Temperatur", "Temperatur",
                    List.of("gGartenhaus"), List.of("Measurement", "Temperature"),
                    Map.of("category", "temperature")),
            item("Contact", "Gartenhaus_Tuer", "Tür", List.of("gGartenhaus"),
                    List.of("OpenState", "Door"), Map.of("category", "door")),
            item("Group", "gGartenhaus_Lichter", "Lichter im Gartenhaus",
                    List.of("gGartenhaus"), List.of(),
                    Map.of("groupType", "Switch", "function",
                           Map.of("name", "OR", "params", List.of("ON", "OFF")))));

    for (Map<String, Object> r : Json.list(items.addOrUpdateItems(gartenhaus))) {
        IO.println("anlegen: %-24s %s".formatted(r.get("name"), r.get("status")));
    }
    items.postUpdate("Gartenhaus_Temperatur", "12.5 °C");
    items.postUpdate("Gartenhaus_Tuer", "CLOSED");
    items.sendCommand("Gartenhaus_Licht", "ON");

    Object raum = items.getItem("gGartenhaus", Map.of("recursive", true));
    Json.list(Json.get(raum, "members")).stream()
            .sorted(Comparator.comparing(m -> (String) m.get("name")))
            .forEach(m -> IO.println("  %-24s %-20s %s".formatted(
                    m.get("name"), m.get("type"), m.get("state"))));
    // Die Gruppenfunktion OR(ON,OFF) fasst die Mitglieder zusammen
    IO.println("Lichter im Gartenhaus: " + items.getItemState("gGartenhaus_Lichter"));

    // Aufräumen: zuerst die Mitglieder, dann die Gruppen
    for (Map<String, Object> i : gartenhaus.reversed()) {
        items.deleteItem((String) i.get("name"));
    }
    IO.println("aufgeräumt: " + Json.isError(items.getItem("gGartenhaus")));
}
