// Kapitel 12: Welche Add-ons gibt es, welche sind installiert?
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Addons addons = new Addons(client);

void main() {
    var alle = Json.list(addons.getAddons(null, "de"));
    var installiert = alle.stream().filter(a -> Boolean.TRUE.equals(a.get("installed")))
            .sorted(Comparator.comparing(a -> (String) a.get("uid"))).toList();
    IO.println(alle.size() + " Add-ons verfügbar, " + installiert.size()
            + " installiert:");
    for (var a : installiert) {
        IO.println("  %-24s %-8s %s".formatted(a.get("uid"), a.get("version"),
                a.get("label")));
    }

    // Wie viele gibt es von jeder Art?
    Map<String, Integer> arten = new TreeMap<>();
    for (var a : alle) {
        arten.merge((String) a.get("type"), 1, Integer::sum);
    }
    IO.println("\nVerfügbar nach Art: " + arten);

    var astro = Json.map(addons.getAddon("binding-astro", null, "de"));
    String text = (String) astro.get("description");
    IO.println("\n" + astro.get("label") + ": "
            + text.substring(0, Math.min(70, text.length()))
            + "…");
    IO.println("Logger-Pakete: " + astro.get("loggerPackages"));
    var quellen = Json.list(addons.getAddonServices()).stream().map(s -> s.get("id"))
            .toList();
    IO.println("Installationsquellen: " + quellen);
}
