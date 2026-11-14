// Kapitel 13: Regeln lesen – Übersicht, Module und Skript
import io.github.michdo93.openhab.*;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final Rules rules = new Rules(client);

void main() {
    for (var r : Json.list(rules.getRules(null, null, true))) {
        IO.println("%-12s %-14s %-6s Tags %s".formatted(r.get("uid"), r.get("name"),
                Json.get(r, "status", "status"), r.getOrDefault("tags", List.of())));
    }

    // Der Aufbau einer Regel: Trigger, Bedingungen, Aktionen – jeweils Module
    String uid = "restbuch-1";
    IO.println("\nRegel " + uid + ":");
    Map<String, Object> teile = new LinkedHashMap<>();
    teile.put("Trigger", rules.getTriggers(uid));
    teile.put("Bedingung", rules.getConditions(uid));
    teile.put("Aktion", rules.getActions(uid));
    teile.forEach((art, module) -> {
        for (var m : Json.list(module)) {
            String cfg = Json.stringify(m.get("configuration"));
            IO.println("  %-10s %-7s %-26s %s…".formatted(art, m.get("id"),
                    m.get("type"),
                    cfg.substring(0, Math.min(36, cfg.length()))));
        }
    });
    Object skript = rules.getModuleConfigParam(uid, "actions", "script", "script");
    String kurz = skript.toString().strip().replace("\n", " | ");
    IO.println("\nSkript der Aktion: "
            + kurz.substring(0, Math.min(66, kurz.length())));
}
