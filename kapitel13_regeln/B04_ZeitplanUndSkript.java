// Kapitel 13: Zeitplan-Trigger, ein Rules-DSL-Skript und die Vorschau des Zeitplans
import io.github.michdo93.openhab.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final Rules rules = new Rules(client);
final String UID = "buch_lueften";
// openHAB liefert z. B. "Nov 16, 2026, 7:30:00 AM"
final DateTimeFormatter EIN = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a",
        Locale.US);
final DateTimeFormatter AUS = DateTimeFormatter.ofPattern("EE dd.MM., HH:mm");

// Rules-DSL-Skript als Aktion: läuft werktags um 7:30 und 17:30
final String SKRIPT = """
        val feuchte = Werkstatt_Feuchte.state
        if (feuchte instanceof QuantityType
                && (feuchte as QuantityType<?>).toUnit("%").doubleValue > 60) {
            Werkstatt_Luefter.sendCommand(ON)
            Haus_Status.postUpdate("Werkstatt wird gelüftet")
        }
        """;

Map<String, Object> cron(String id, String ausdruck) {
    return Map.of("id", id, "type", "timer.GenericCronTrigger",
            "configuration", Map.of("cronExpression", ausdruck));
}

void main() throws InterruptedException {
    rules.deleteRule(UID);
    IO.println(rules.createRule(Map.of(
            "uid", UID, "name", "Werkstatt lüften", "tags", List.of("Buch", "Schedule"),
            "triggers", List.of(cron("morgens", "0 30 7 ? * MON-FRI"),
                                cron("abends", "0 30 17 ? * MON-FRI")),
            "actions", List.of(Map.of("id", "skript", "type", "script.ScriptAction",
                    "configuration", Map.of("type", "application/vnd.openhab.dsl.rule",
                                            "script", SKRIPT))))));

    // Wann läuft was? openHAB simuliert alle Regeln mit dem Tag "Schedule"
    ZoneId zone = ZoneId.systemDefault();
    LocalDate heute = LocalDate.now();
    String von = heute.plusDays(1).atStartOfDay(zone).toInstant().toString();
    String bis = heute.plusDays(8).atStartOfDay(zone).toInstant().toString();
    for (var eintrag : Json.list(rules.simulateSchedule(von, bis))) {
        if (!UID.equals(Json.get(eintrag, "rule", "uid"))) {
            continue;
        }
        String text = ((String) eintrag.get("date")).replaceAll("\\s+", " ");
        LocalDateTime zeit = LocalDateTime.parse(text, EIN);
        IO.println("  " + AUS.format(zeit) + "  " + Json.get(eintrag, "rule", "name"));
    }

    // Nicht bis morgen warten: Werkstatt feucht machen und die Regel sofort ausführen
    items.postUpdate("Werkstatt_Feuchte", "72 %");
    rules.runNow(UID);
    Thread.sleep(2000);  // DSL-Skripte werden beim ersten Lauf übersetzt
    IO.println("Status: " + items.getItemState("Haus_Status"));
    IO.println("Lüfter: " + items.getItemState("Werkstatt_Luefter")
            + " (autoupdate=false: erst das Gerät bestätigt den Command)");
    IO.println("löschen: " + rules.deleteRule(UID));
}
