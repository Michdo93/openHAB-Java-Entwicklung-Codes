// Kapitel 12: Geräte suchen lassen und aus der Inbox übernehmen
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Things things = new Things(client);

void main() throws InterruptedException {
    // Das Astro-Binding findet "Geräte" für den eingestellten Standort.
    // Der Standort ist eine Systemeinstellung – auch sie lässt sich per REST setzen.
    var services = new Services(client);
    var config = Json.map(services.getServiceConfig("org.openhab.i18n"));
    config.put("location", "48.0950,8.6630");  // Breite, Länge (Aldingen)
    services.updateServiceConfig("org.openhab.i18n", config);
    IO.println("Standort: " + Json.get(services.getServiceConfig("org.openhab.i18n"),
            "location"));

    var discovery = new Discovery(client);
    var inbox = new Inbox(client);
    IO.println("Bindings mit Gerätesuche: " + discovery.getDiscoveryBindings());
    IO.println("Suche läuft, Dauer in s: " + discovery.startBindingScan("astro"));
    Thread.sleep(3000);
    for (var e : Json.list(inbox.getDiscoveredThings())) {
        IO.println("  %-8s %-18s %s  %s".formatted(e.get("flag"), e.get("thingUID"),
                e.get("label"), e.get("properties")));
    }

    // Den Mond übernehmen: aus dem Inbox-Eintrag wird ein Thing
    IO.println("\nübernehmen: "
            + inbox.approveDiscoveryResult("astro:moon:local", "Mond über Aldingen"));
    Thread.sleep(2000);
    var mond = Json.map(things.getThing("astro:moon:local"));
    IO.println(mond.get("label") + " " + Json.get(mond, "statusInfo", "status") + " "
            + Json.list(mond.get("channels")).size() + " Channels");

    // Wieder entfernen – der Eintrag erscheint bei der nächsten Suche erneut
    Object geloescht = things.deleteThing("astro:moon:local");
    IO.println("löschen: " + Json.get(geloescht, "statusInfo", "status"));
}
