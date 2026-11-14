// Kapitel 16: Ein Strom für vieles – Items, Things und Links gleichzeitig beobachten
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final Things things = new Things(client);
final Links links = new Links(client);
final String SONNE = "astro:sun:zuhause";
final String KANAL = SONNE + ":position#azimuth";
final String THEMEN = String.join(",",
        "openhab/items/Sonne_Azimut/added",          // Item angelegt
        "openhab/items/Sonne_Azimut/removed",        // Item gelöscht
        "openhab/items/Sonne_Azimut/statechanged",   // Zustand geändert
        "openhab/things/*/statuschanged",            // Statuswechsel aller Things
        "openhab/links/*/*");                        // neue und gelöschte Links

// Erzeugt nacheinander typische Ereignisse und schließt dann den Strom
void aktionen(SSEConnection strom) throws InterruptedException {
    Thread.sleep(1000);
    items.addOrUpdateItem("Sonne_Azimut", Map.of("type", "Number:Angle",
            "name", "Sonne_Azimut", "label", "Sonnenrichtung"));
    links.linkItemToChannel("Sonne_Azimut", KANAL, Map.of());
    Thread.sleep(1000);
    things.disableThing(SONNE);
    Thread.sleep(1000);
    things.enableThing(SONNE);
    Thread.sleep(2000);
    links.unlinkItemFromChannel("Sonne_Azimut", KANAL);
    items.deleteItem("Sonne_Azimut");
    Thread.sleep(1000);
    strom.close();                                   // fertig: Strom schließen
}

String info(String typ, Object p) {
    if (typ.equals("ThingStatusInfoChangedEvent")) {
        Object neu = Json.get(p, 0), alt = Json.get(p, 1);      // Liste [neu, alt]
        return Json.get(alt, "status") + " -> " + Json.get(neu, "status")
                + " (" + Json.get(neu, "statusDetail") + ")";
    }
    if (typ.startsWith("ItemChannelLink")) {
        String[] kanal = ((String) Json.get(p, "channelUID")).split(":");
        return Json.get(p, "itemName") + " <-> " + kanal[kanal.length - 1];
    }
    if (p instanceof Map<?, ?> m) {
        Object wert = m.get("value") != null ? m.get("value") : m.get("name");
        return wert == null ? "" : wert.toString();
    }
    return "";
}

void main() {
    SSEConnection strom = new Events(client).getEvents(THEMEN);
    Thread.startVirtualThread(() -> {
        try {
            aktionen(strom);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    });
    for (var e : strom.events()) {
        String typ = (String) e.get("type");
        IO.println("%-34s %s".formatted(typ, info(typ, e.get("payload"))));
    }
}
