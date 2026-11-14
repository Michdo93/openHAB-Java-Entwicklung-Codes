// Kapitel 12: Items mit Channels verknüpfen – mit und ohne Profil
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final Links links = new Links(client);
final String SONNE = "astro:sun:zuhause";

void main() throws InterruptedException {
    // Zwei neue Items für Sonnenhöhe und Tagesphase
    items.addOrUpdateItem("Sonne_Hoehe", Map.of("type", "Number:Angle",
            "name", "Sonne_Hoehe", "label", "Sonnenhöhe", "groupNames",
                    List.of("gHaus")));
    items.addOrUpdateItem("Sonne_Phase", Map.of("type", "String",
            "name", "Sonne_Phase", "label", "Tagesphase", "groupNames",
                    List.of("gHaus")));

    // Welche Profile passen zwischen diesen Channel und ein Number:Angle-Item?
    Object profile = new ProfileTypes(client)
            .getProfileTypes("astro:elevation", "Number:Angle");
    IO.println("passende Profile: "
            + Json.list(profile).stream().map(p -> p.get("uid")).toList());

    // Link ohne Profil und Link mit Profil "offset" (+90° -> 0° bis 180°)
    IO.println("Link 1: "
            + links.linkItemToChannel("Sonne_Phase", SONNE + ":phase#name", Map.of()));
    IO.println("Link 2: " + links.linkItemToChannel("Sonne_Hoehe",
            SONNE + ":position#elevation",
            Map.of("profile", "system:offset", "offset", "90 °")));
    Thread.sleep(2000);  // das Binding liefert den Wert beim Verknüpfen
    IO.println("Sonne_Phase = " + items.getItemState("Sonne_Phase"));
    IO.println("Sonne_Hoehe = " + items.getItemState("Sonne_Hoehe") + " (Höhe + 90°)");
    for (var link : Json.list(links.getLinks(null, "Sonne_Hoehe"))) {
        IO.println("gespeichert: " + link.get("itemName") + " -> "
                + link.get("channelUID")
                + " " + link.get("configuration"));
    }

    // Aufräumen: Links lösen, Items löschen, verwaiste Links prüfen
    IO.println("lösen: "
            + links.unlinkItemFromChannel("Sonne_Phase", SONNE + ":phase#name"));
    IO.println("alle Links des Items löschen: " + links.deleteAllLinks("Sonne_Hoehe"));
    for (String name : List.of("Sonne_Hoehe", "Sonne_Phase")) {
        items.deleteItem(name);
    }
    IO.println("verwaiste Links: " + links.getOrphanLinks());
}
