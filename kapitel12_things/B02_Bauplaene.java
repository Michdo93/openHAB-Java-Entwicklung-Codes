// Kapitel 12: Baupläne – Thing-Typen, Channel-Gruppen, Konfiguration, Channel-Typen
import io.github.michdo93.openhab.*;
import java.util.stream.Collectors;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));

void main() {
    var typen = new ThingTypes(client);
    for (var t : Json.list(typen.getThingTypes("astro", "de"))) {
        IO.println("%-12s %s".formatted(t.get("UID"), t.get("label")));
    }
    var mond = Json.map(typen.getThingType("astro:moon", "de"));
    IO.println("\n" + mond.get("label") + " – Channel-Gruppen:");
    for (var gruppe : Json.list(mond.get("channelGroups"))) {
        String kanaele = Json.list(gruppe.get("channels")).stream()
                .map(k -> (String) k.get("id")).collect(Collectors.joining(", "));
        IO.println("  %-10s %s".formatted(gruppe.get("id"), kanaele));
    }

    // Welche Konfiguration braucht ein Mond-Thing?
    Object beschreibung = new ConfigDescriptions(client)
            .getConfigDescription("thing-type:astro:moonconfig", "de");
    IO.println("\nKonfiguration:");
    for (var p : Json.list(Json.get(beschreibung, "parameters"))) {
        String pflicht = Boolean.TRUE.equals(p.get("required")) ? "Pflicht"
                : "Standard " + Objects.requireNonNullElse(p.get("default"), "–");
        IO.println("  %-12s %-8s %-16s %s".formatted(
                p.get("name"), p.get("type"), pflicht, p.get("label")));
    }

    // Ein Channel-Typ verrät, zu welchem Item-Typ er passt
    var kanal = Json.map(new ChannelTypes(client)
            .getChannelType("astro:phaseName", "de"));
    IO.println("\nChannel-Typ " + kanal.get("UID") + ": " + kanal.get("label")
            + ", Item-Typ " + kanal.get("itemType"));
    String optionen = Json.list(Json.get(kanal, "stateDescription", "options")).stream()
            .limit(8).map(o -> (String) o.get("label"))
                    .collect(Collectors.joining(", "));
    IO.println("Optionen: " + optionen);
}
