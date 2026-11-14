// Kapitel 11: Ein Item anlegen, lesen, ändern und löschen (CRUD)
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final String NAME = "Garten_Pumpe";

void main() {
    // Create – ein neues Item aus einer Map (wird als JSON gesendet)
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put("type", "Switch");
    definition.put("name", NAME);
    definition.put("label", "Gartenpumpe");
    definition.put("category", "water");
    definition.put("tags", List.of("Switch"));
    definition.put("groupNames", List.of("gHaus"));
    Map<String, Object> neu = Json.map(items.addOrUpdateItem(NAME, definition));
    IO.println("angelegt:  " + neu.get("name") + " " + neu.get("type")
            + " " + neu.get("state"));

    // Read – und gleich bedienen: ein per REST angelegtes Item ist ein normales Item
    items.sendCommand(NAME, "ON");
    IO.println("gelesen:   " + items.getItemState(NAME));

    // Update – PUT ersetzt die ganze Definition (idempotent)
    definition.put("label", "Gartenpumpe Zisterne");
    definition.put("groupNames", List.of("gHaus", "gWerkstatt"));
    items.addOrUpdateItem(NAME, definition);
    Map<String, Object> pumpe = Json.map(items.getItem(NAME));
    IO.println("geändert:  " + pumpe.get("label") + " " + pumpe.get("groupNames")
            + " Zustand: " + pumpe.get("state"));
    boolean editierbar = Boolean.TRUE.equals(pumpe.get("editable"));
    IO.println("verwaltet: " + (editierbar ? "per REST änderbar" : "aus einer Datei"));

    // Delete – und wieder aufräumen
    IO.println("löschen:   " + items.deleteItem(NAME));
    IO.println("danach:    " + items.getItem(NAME));
}
