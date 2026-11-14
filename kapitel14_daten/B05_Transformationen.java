// Kapitel 14: Ein Add-on installieren und eine Transformation anlegen
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Addons addons = new Addons(client);
final Items items = new Items(client);
final Transformations trafos = new Transformations(client);
final String UID = "config:map:tuer";

boolean mapBereit() {
    return Json.array(trafos.getTransformationServices()).contains("MAP");
}

void main() throws InterruptedException {
    // 1. Den MAP-Transformationsdienst installieren (falls noch nicht geschehen)
    if (!Boolean.TRUE.equals(Json.get(addons.getAddon("transformation-map"),
            "installed"))) {
        IO.println("installieren: " + addons.installAddon("transformation-map"));
        while (!mapBereit()) {
            Thread.sleep(1000);
        }
    }
    IO.println("Transformationsdienste: " + trafos.getTransformationServices());

    // 2. Eine eigene MAP-Transformation anlegen
    IO.println("anlegen: " + trafos.updateTransformation(UID, Map.of(
            "uid", UID, "label", "Türzustand", "type", "map",
            "configuration", Map.of("function",
                    "OPEN=offen\nCLOSED=geschlossen\nNULL=unbekannt\n"))));
    for (var t : Json.list(trafos.getTransformations())) {
        IO.println("  %-16s %-4s %s".formatted(t.get("uid"), t.get("type"),
                Boolean.TRUE.equals(t.get("editable")) ? "änderbar" : "aus Datei"));
    }

    // 3. Im Anzeigeformat eines Items verwenden
    items.addMetadata("Flur_Haustuer", "stateDescription", Map.of("value", " ",
            "config", Map.of("pattern", "MAP(" + UID + "):%s")));
    for (String zustand : List.of("OPEN", "CLOSED")) {
        items.postUpdate("Flur_Haustuer", zustand);
        Thread.sleep(300);  // postUpdate wird asynchron verarbeitet
        var tuer = Json.map(items.getItem("Flur_Haustuer"));
        IO.println("Zustand %-6s -> Anzeige %s".formatted(tuer.get("state"),
                tuer.get("transformedState")));
    }

    // Aufräumen – das Add-on bleibt installiert
    items.removeMetadata("Flur_Haustuer", "stateDescription");
    IO.println("löschen: " + trafos.deleteTransformation(UID));
}
