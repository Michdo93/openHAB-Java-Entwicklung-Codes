// Kapitel 13: Welche Bausteine gibt es für Regeln?
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final ModuleTypes module = new ModuleTypes(client);

void main() {
    Map<String, List<Map<String, Object>>> nachArt = new LinkedHashMap<>();
    for (String art : List.of("Trigger", "Bedingung", "Aktion")) {
        nachArt.put(art, new ArrayList<>());
    }
    for (var m : Json.list(module.getModuleTypes(null, null, "de"))) {
        String uid = (String) m.get("uid");
        if (!"VISIBLE".equals(m.get("visibility")) || uid.contains("#")) {
            continue;                                          // ohne Thing-Actions
        }
        String art = uid.contains("Trigger") ? "Trigger"
                : uid.contains("Condition") ? "Bedingung" : "Aktion";
        nachArt.get(art).add(m);
    }
    nachArt.forEach((art, liste) -> {
        IO.println(art + " (" + liste.size() + "):");
        liste.stream().sorted(Comparator.comparing(m -> (String) m.get("uid"))).limit(7)
                .forEach(m -> IO.println("  %-30s %s".formatted(m.get("uid"),
                        m.get("label"))));
    });

    var typ = Json.map(module.getModuleType("core.ItemCommandAction", "de"));
    IO.println("\n" + typ.get("uid") + " – " + typ.get("description"));
    for (var p : Json.list(typ.get("configDescriptions"))) {
        boolean pflicht = Boolean.TRUE.equals(p.get("required"));
        IO.println("  %-10s %-6s %-8s %s".formatted(p.get("name"), p.get("type"),
                pflicht ? "Pflicht" : "optional", p.get("label")));
    }
    IO.println("\nRegelvorlagen installiert: "
            + Json.list(new Templates(client).getTemplates("de")).size());
}
