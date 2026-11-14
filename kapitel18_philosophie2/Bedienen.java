// Kapitel 18: Die Geräte bedienen – wie es die Main UI, eine App oder eine Regel täte
import io.github.michdo93.openhab.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final Items items = new Items(new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN")));
final DateTimeFormatter UHR = DateTimeFormatter.ofPattern("HH:mm:ss");

void log(String text) {
    IO.println(UHR.format(LocalTime.now()) + " " + text);
}

record Schritt(String item, String command, String ergebnis) {}

void main() throws InterruptedException {
    for (var s : List.of(
            new Schritt("Werkstatt_Luefter", "ON", "Werkstatt_Luefter"),
            new Schritt("Rechner_Aktion", "ERREICHBARKEIT", "Rechner_Antwort"),
            new Schritt("Rechner_Aktion", "DATENTRAEGER", "Rechner_Antwort"),
            new Schritt("Rechner_Aktion", "NEUSTART", "Rechner_Antwort"),  // verboten
            new Schritt("Werkstatt_Luefter", "OFF", "Werkstatt_Luefter"))) {
        log("Command " + s.item() + " <- " + s.command());
        items.sendCommand(s.item(), s.command());
        Thread.sleep(1500);
        log("  " + s.ergebnis() + " = " + items.getItemState(s.ergebnis()));
    }
}
