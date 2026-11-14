// Kapitel 15: Systemdienste, Protokollierung, Zugänge und Symbole
import io.github.michdo93.openhab.*;
import java.util.stream.Collectors;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Services services = new Services(client);
final Logging logging = new Logging(client);
final Auth auth = new Auth(client);
final String LOGGER = "org.openhab.binding.astro";

Object level() {
    return Json.get(logging.getLogger(LOGGER), "loggers", 0, "level");
}

void main() {
    // 1. Systemdienste und ihre Einstellungen (dieselben wie in der Main UI)
    for (var s : Json.list(services.getServices("de"))) {
        if ("system".equals(s.get("category"))) {
            IO.println("  %-26s %s".formatted(s.get("id"), s.get("label")));
        }
    }
    IO.println("Regionale Einstellungen: "
            + services.getServiceConfig("org.openhab.i18n"));

    // 2. Log-Level eines Bindings zur Fehlersuche anheben – und zurücksetzen
    IO.println("\nvorher: " + level());
    logging.modifyOrAddLogger(LOGGER, "DEBUG");
    IO.println("jetzt:  " + level());
    logging.removeLogger(LOGGER);                          // zurück auf den Standard
    IO.println("danach: " + level());

    // 3. Wer hat Zugang? API-Tokens und Sitzungen des angemeldeten Benutzers
    for (var t : Json.list(auth.getAPITokens())) {
        IO.println("\nToken \"%s\" (Bereich %s) seit %s".formatted(t.get("name"),
                t.get("scope"), t.get("createdTime").toString().substring(0, 10)));
    }
    IO.println("offene Sitzungen: " + Json.list(auth.getSessions()).size());

    // 4. Symbolsätze und Maßeinheiten
    IO.println("Symbolsätze: "
            + Json.list(new Iconsets(client).getIconsets("de")).stream()
            .map(i -> (String) i.get("label")).collect(Collectors.joining(", ")));
    var groessen = Json.list(Json.get(new Systeminfo(client).getUoMInfo(),
            "uomInfo", "dimensions"));
    IO.println(groessen.size() + " physikalische Größen, z. B. "
            + groessen.stream().limit(3)
            .map(d -> d.get("dimension") + " [" + d.get("systemUnit") + "]")
            .collect(Collectors.joining(", ")));
}
