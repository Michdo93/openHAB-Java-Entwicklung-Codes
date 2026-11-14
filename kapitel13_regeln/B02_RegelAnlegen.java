// Kapitel 13: Eine Regel anlegen – Trigger, Bedingung, Aktionen
import io.github.michdo93.openhab.*;
import java.util.stream.Collectors;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final Rules rules = new Rules(client);
final String UID = "buch_kinoabend";
final List<String> NAMEN = List.of("Wohnzimmer_Licht", "Wohnzimmer_Dimmer",
        "Kueche_Rollladen");

// Ein Aktionsmodul: "sende Command an Item"
Map<String, Object> befehl(String id, String itemName, String command) {
    return Map.of("id", id, "type", "core.ItemCommandAction",
            "configuration", Map.of("itemName", itemName, "command", command));
}

String zustand() {
    return NAMEN.stream().map(n -> n + "=" + items.getItemState(n))
            .collect(Collectors.joining(", "));
}

void main() throws InterruptedException {
    Map<String, Object> regel = Map.of(
            "uid", UID,
            "name", "Kinoabend",
            "description",
                    "Licht dimmen und Rollladen schließen, wenn die Haustür zu ist.",
            "tags", List.of("Buch", "Szene"),
            "triggers", List.of(Map.of("id", "1", "type", "core.ItemCommandTrigger",
                    "configuration",
                            Map.of("itemName", "Haus_Status", "command", "Kino"))),
            "conditions", List.of(Map.of("id", "2", "type", "core.ItemStateCondition",
                    "configuration", Map.of("itemName", "Flur_Haustuer",
                                            "operator", "=", "state", "CLOSED"))),
            "actions", List.of(befehl("3", "Wohnzimmer_Licht", "OFF"),
                               befehl("4", "Wohnzimmer_Dimmer", "20"),
                               befehl("5", "Kueche_Rollladen", "DOWN")));

    rules.deleteRule(UID);                     // falls vom letzten Lauf noch vorhanden
    IO.println("anlegen: " + rules.createRule(regel));
    IO.println("Status:  " + Json.get(rules.getRule(UID), "status", "status"));

    // Ausgangslage herstellen
    items.sendCommand("Wohnzimmer_Licht", "ON");
    items.sendCommand("Wohnzimmer_Dimmer", "80");
    items.sendCommand("Kueche_Rollladen", "UP");

    // 1. Tür offen: die Bedingung verhindert die Aktionen
    items.postUpdate("Flur_Haustuer", "OPEN");
    items.sendCommand("Haus_Status", "Kino");
    Thread.sleep(1000);
    IO.println("Tür offen:  " + zustand());

    // 2. Tür zu: die Regel greift
    items.postUpdate("Flur_Haustuer", "CLOSED");
    items.sendCommand("Haus_Status", "Kino");
    Thread.sleep(1000);
    IO.println("Tür zu:     " + zustand());
}
