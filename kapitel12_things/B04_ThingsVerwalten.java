// Kapitel 12: Ein Thing anlegen, umkonfigurieren, abschalten und löschen
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Things things = new Things(client);
final String UID = "astro:moon:buch";

String status() {
    Object s = things.getThingStatus(UID);
    String detail = Json.str(s, "statusDetail");
    return Json.str(s, "status") + ("NONE".equals(detail) ? "" : " / " + detail);
}

void main() throws InterruptedException {
    var neu = Json.map(things.createThing(Map.of(
            "UID", UID, "thingTypeUID", "astro:moon", "label", "Mond (per REST)",
            "configuration",
                    Map.of("geolocation", "48.0950,8.6630", "interval", 300))));
    IO.println("angelegt: " + neu.get("UID") + " mit "
            + Json.list(neu.get("channels")).size() + " Channels");
    Thread.sleep(2000);
    IO.println("Status:   " + status());

    // Konfiguration ändern – openHAB initialisiert das Thing neu
    things.updateThingConfiguration(UID,
            Map.of("geolocation", "52.5200,13.4050", "interval", 60));
    IO.println("Ort:      "
            + Json.get(things.getThing(UID), "configuration", "geolocation")
            + " (Berlin)");

    // Label ändern: updateThing ersetzt die ganze Definition
    var thing = Json.map(things.getThing(UID));
    thing.put("label", "Mond über Berlin");
    IO.println("Label:    " + Json.get(things.updateThing(UID, thing), "label"));

    // Abschalten und wieder einschalten
    things.disableThing(UID);
    Thread.sleep(1000);
    IO.println("aus:      " + status());
    things.enableThing(UID);
    Thread.sleep(2000);
    IO.println("an:       " + status());

    IO.println("löschen:  "
            + Json.get(things.deleteThing(UID), "statusInfo", "status"));
    IO.println("Things:   " + Json.list(things.getThings(true)).stream()
            .map(t -> t.get("UID")).toList());
}
