// Kapitel 11: Metadaten – Zusatzinformationen, die openHABs Verhalten ändern
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final String NAME = "Klingel_Status";

void main() throws InterruptedException {
    items.addOrUpdateItem(NAME, Map.of("type", "String", "name", NAME,
            "label", "Klingel", "groupNames", List.of("gHaus")));

    // 1. stateDescription: Anzeigeformat und erlaubte Werte für Oberflächen
    String optionen = "RUHE=Ruhe,KLINGELT=Es klingelt!,GEOEFFNET=Tür geöffnet";
    IO.println("stateDescription: " + items.addMetadata(NAME, "stateDescription",
            Map.of("value", " ", "config",
                   Map.of("pattern", "%s", "readOnly", false, "options", optionen))));

    // 2. expire: Zustand fällt nach zehn Sekunden automatisch zurück
    IO.println("expire:           "
            + items.addMetadata(NAME, "expire", Map.of("value", "10s,state=RUHE")));

    // 3. eigener Namensraum: beliebige Daten für die eigene Anwendung
    IO.println("eigener:          " + items.addMetadata(NAME, "restbuch", Map.of(
            "value", "klingel", "config", Map.of("raum", "Flur", "prioritaet", 2))));
    IO.println("Namensräume:      " + items.getMetadataNamespaces(NAME));
    Object item = items.getItem(NAME,
            Map.of("metadata", "stateDescription,expire,restbuch"));
    IO.println(Json.stringify(Json.get(item, "metadata"), 2));

    // Die Anzeige nutzt die Optionen aus stateDescription
    items.postUpdate(NAME, "KLINGELT");
    Thread.sleep(300);                      // openHAB verarbeitet Updates asynchron
    Map<String, Object> klingel = Json.map(items.getItem(NAME));
    var optionenListe = Json.list(Json.get(klingel, "stateDescription", "options"));
    String anzeige = optionenListe.stream()
            .filter(o -> o.get("value").equals(klingel.get("state")))
            .map(o -> (String) o.get("label"))
            .findFirst().orElse("?");
    IO.println("Zustand: " + klingel.get("state") + " | Anzeige: " + anzeige);

    // expire setzt den Zustand nach zehn Sekunden zurück
    Thread.sleep(11_000);
    IO.println("nach 11 s: " + items.getItemState(NAME));
    IO.println("entfernen: " + items.removeMetadata(NAME, "restbuch"));
    items.deleteItem(NAME);
}
