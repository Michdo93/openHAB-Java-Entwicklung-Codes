// Kapitel 12: Thing-Actions – das Astro-Binding rechnet die Sonnenhöhe aus
import io.github.michdo93.openhab.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Actions actions = new Actions(client);
final String SONNE = "astro:sun:zuhause";
final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM., HH:mm");
final Map<String, String> uid = new HashMap<>();

String hoehe(ZonedDateTime zeit) {
    Object antwort = actions.executeAction(SONNE, uid.get("astro.getElevation"),
            Map.of("date", zeit.toInstant().toString()));
    String text = Json.str(antwort, "result");             // z. B. "-56.97 °"
    double grad = Double.parseDouble(text.split(" ")[0]);
    return "%6.1f".formatted(grad);
}

void main() {
    for (var a : Json.list(actions.getActions(SONNE, "de"))) {
        // Die UID enthält einen Hash hinter "#" – wir merken sie uns über den Namen
        String name = ((String) a.get("actionUid")).split("#")[0];
        uid.put(name, (String) a.get("actionUid"));
        String eingaben = Json.list(a.get("inputs")).stream()
                .map(i -> (String) i.get("name")).collect(Collectors.joining(", "));
        IO.println("  %-26s (%s) – %s".formatted(name, eingaben, a.get("description")));
    }

    IO.println("\nSonnenhöhe über den Tag (jetzt und die nächsten Stunden):");
    ZonedDateTime jetzt = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
    for (int stunden = 0; stunden < 24; stunden += 3) {
        ZonedDateTime zeit = jetzt.plusHours(stunden);
        IO.println("  " + FORMAT.format(zeit) + "  " + hoehe(zeit) + "°");
    }
    for (int monat : new int[] {3, 6, 9, 12}) {
        ZonedDateTime tag = jetzt.withMonth(monat).withDayOfMonth(21).withHour(12);
        IO.println("  " + FORMAT.format(tag) + "  " + hoehe(tag) + "°");
    }
}
