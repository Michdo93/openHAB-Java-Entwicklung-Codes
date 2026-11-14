// Kapitel 11: Gruppen zur Laufzeit ändern und den Zustandsverlauf lesen
import io.github.michdo93.openhab.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final DateTimeFormatter UHR = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault());

// Alle Ketten von Gruppen über einem Item, z. B. gLichter -> gHaus
List<String> ketten(Object item) {
    List<String> out = new ArrayList<>();
    for (Map<String, Object> p : Json.list(Objects.requireNonNullElse(
            Json.get(item, "parents"), List.of()))) {
        List<String> weiter = ketten(p);
        if (weiter.isEmpty()) {
            out.add((String) p.get("name"));
        }
        weiter.forEach(k -> out.add(p.get("name") + " -> " + k));
    }
    return out;
}

String uhrzeit(Object millis) {
    return UHR.format(Instant.ofEpochMilli(((Number) millis).longValue()));
}

void main() throws InterruptedException {
    // Ein per REST angelegtes Licht in die Gruppe aller Lichter aufnehmen
    items.addOrUpdateItem("Flur_Licht", Map.of("type", "Switch", "name", "Flur_Licht",
            "label", "Flurlicht", "tags", List.of("Switch", "Light")));
    IO.println("aufnehmen: " + items.addGroupMember("gLichter", "Flur_Licht"));
    var mitglieder = Json.list(Json.get(items.getItem("gLichter"), "members"));
    IO.println("Mitglieder: " + mitglieder.stream().map(m -> m.get("name")).toList());

    // Ein Command an die Gruppe erreicht alle Mitglieder
    items.sendCommand("gLichter", "OFF");
    items.sendCommand("Flur_Licht", "ON");
    Thread.sleep(300);
    IO.println("gLichter: " + items.getItemState("gLichter")
            + " (mindestens eines ist an)");

    // openHAB 5 merkt sich den vorherigen Zustand und die Zeitpunkte
    Map<String, Object> licht = Json.map(items.getItem("Flur_Licht"));
    IO.println("Flur_Licht: " + licht.get("state")
            + " (vorher " + licht.get("lastState") + "), aktualisiert "
            + uhrzeit(licht.get("lastStateUpdate"))
            + ", geändert " + uhrzeit(licht.get("lastStateChange")));

    // In welchen Gruppen steckt das Item – bis ganz nach oben?
    Object mitEltern = items.getItem("Flur_Licht", Map.of("parents", true));
    IO.println("Eltern: " + String.join(" | ", ketten(mitEltern)));

    IO.println("entfernen: " + items.removeGroupMember("gLichter", "Flur_Licht"));
    items.deleteItem("Flur_Licht");
}
