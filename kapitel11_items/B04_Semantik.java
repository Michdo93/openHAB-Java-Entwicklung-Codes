// Kapitel 11: Das semantische Modell – Orte, Ausstattung und Messpunkte
import io.github.michdo93.openhab.*;
import java.util.stream.Collectors;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final Tags tags = new Tags(client);

void main() {
    // 1. Der Tag-Katalog: Hunderte vordefinierte semantische Tags, hier auf Deutsch
    var katalog = Json.list(tags.getTags("de"));
    var orte = katalog.stream()
            .filter(t -> ((String) t.get("uid")).startsWith("Location_")).toList();
    IO.println(katalog.size() + " Tags, davon " + orte.size() + " Orte, z. B.: "
            + orte.stream().limit(6).map(t -> (String) t.get("label"))
                    .collect(Collectors.joining(", ")));

    // 2. openHAB leitet aus den Tags Metadaten im Namensraum "semantics" ab
    Object alle = items.getItems(Map.of("metadata", "semantics",
            "fields", "name,label,metadata"));
    for (Map<String, Object> item : Json.list(alle)) {
        Object sem = Json.get(item, "metadata", "semantics");
        if (sem == null) {
            continue;
        }
        String bezug = Objects.toString(Json.get(sem, "config", "relatesTo"), "");
        String ort = Objects.toString(Json.get(sem, "config", "hasLocation"), "");
        IO.println("  %-24s %-26s %-22s %s".formatted(
                item.get("name"), Json.get(sem, "value"), bezug, ort));
    }

    // 3. Wo befindet sich ein Messpunkt? openHAB folgt dem Modell nach oben.
    Object ort = items.getSemanticItem("Wohnzimmer_Temperatur", "Location");
    IO.println("\nWohnzimmer_Temperatur liegt in: " + Json.get(ort, "name")
            + " (" + Json.get(ort, "label") + ")");

    // 4. Ein eigener Ort: das Tag wird per REST angelegt und sofort nutzbar
    Object tag = tags.createTag(Map.of(
            "uid", "Location_Outdoor_Garden_Gartenhaus", "name", "Gartenhaus",
            "label", "Gartenhaus", "description", "Hütte im Garten",
            "synonyms", List.of("Schuppen")));
    IO.println("\nTag angelegt: " + Json.get(tag, "uid")
            + " (editierbar: " + Json.get(tag, "editable") + ")");
    items.addOrUpdateItem("gGartenhaus", Map.of("type", "Group", "name", "gGartenhaus",
            "label", "Gartenhaus", "tags", List.of("Gartenhaus"),
            "groupNames", List.of("gHaus")));
    items.addOrUpdateItem("Gartenhaus_Temperatur", Map.of("type", "Number:Temperature",
            "name", "Gartenhaus_Temperatur", "label", "Temperatur",
            "tags", List.of("Measurement", "Temperature"),
            "groupNames", List.of("gGartenhaus")));
    Object neu = items.getItem("Gartenhaus_Temperatur",
            Map.of("metadata", "semantics"));
    Object sem = Json.get(neu, "metadata", "semantics");
    IO.println("Semantik: " + Json.get(sem, "value") + " " + Json.get(sem, "config"));

    for (String name : List.of("Gartenhaus_Temperatur", "gGartenhaus")) {
        items.deleteItem(name);
    }
    IO.println("Tag löschen: " + tags.deleteTag("Location_Outdoor_Garden_Gartenhaus"));
}
