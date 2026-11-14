// Kapitel 13: Eine Regel steuern – Parameter ändern, ausführen, abschalten, ersetzen
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final Rules rules = new Rules(client);
final String UID = "buch_kinoabend";                      // aus B02_RegelAnlegen

String status() {
    Object s = Json.get(rules.getRule(UID), "status");
    return Json.get(s, "status") + " / " + Json.get(s, "statusDetail");
}

void main() throws InterruptedException {
    if (Json.isError(rules.getRule(UID))) {
        throw new IllegalStateException("Zuerst B02_RegelAnlegen.java ausführen.");
    }

    // 1. Einen einzelnen Parameter eines Moduls ändern: Dimmer auf 10 statt 20 %
    rules.setModuleConfigParam(UID, "actions", "4", "command", "10");
    IO.println("Dimmwert jetzt: "
            + rules.getModuleConfigParam(UID, "actions", "4", "command"));

    // 2. Sofort ausführen – runNow überspringt Trigger UND Bedingungen
    items.sendCommand("Wohnzimmer_Dimmer", "80");
    items.postUpdate("Flur_Haustuer", "OPEN");
    rules.runNow(UID);
    Thread.sleep(1000);
    IO.println("runNow trotz offener Tür: " + items.getItemState("Wohnzimmer_Dimmer"));

    // 3. Abschalten: der Trigger wird ignoriert
    rules.disable(UID);
    IO.println("abgeschaltet: " + status());
    items.postUpdate("Flur_Haustuer", "CLOSED");
    items.sendCommand("Wohnzimmer_Dimmer", "80");
    items.sendCommand("Haus_Status", "Kino");
    Thread.sleep(1000);
    IO.println("Trigger bei abgeschalteter Regel: "
            + items.getItemState("Wohnzimmer_Dimmer"));
    rules.enable(UID);
    IO.println("eingeschaltet: " + status());

    // 4. Die ganze Regel ersetzen: eine zusätzliche Aktion
    Map<String, Object> regel = Json.map(rules.getRule(UID));
    List<Object> aktionen = new ArrayList<>(Json.array(regel.get("actions")));
    aktionen.add(Map.of("id", "6", "type", "core.ItemStateUpdateAction",
            "configuration", Map.of("itemName", "Haus_Status", "state", "Film läuft")));
    regel.put("actions", aktionen);
    rules.updateRule(UID, regel);
    items.sendCommand("Haus_Status", "Kino");
    Thread.sleep(1000);
    IO.println("nach updateRule: " + items.getItemState("Haus_Status"));
    IO.println("löschen: " + rules.deleteRule(UID));
}
