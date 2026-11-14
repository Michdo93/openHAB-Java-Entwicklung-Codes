// Kapitel 10: Commands senden
import io.github.michdo93.openhab.*;

void main() throws InterruptedException {
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    var client = new OpenHABClient(url, null, null, System.getenv("OPENHAB_TOKEN"));
    var items = new Items(client);

    String[][] befehle = {
        {"Wohnzimmer_Licht", "ON"},               // Switch
        {"Wohnzimmer_Dimmer", "60"},              // Dimmer: Prozent
        {"Wohnzimmer_Farblicht", "240,100,80"},   // Color: Farbton,Sättigung,Helligkeit
        {"Kueche_Rollladen", "DOWN"},             // Rollershutter
        {"Haus_Status", "Alle zu Hause"},         // String
    };
    for (String[] b : befehle) {
        Object antwort = items.sendCommand(b[0], b[1], "buch:kapitel10");
        IO.println("%-22s <- %-14s %s".formatted(b[0], b[1], antwort));
    }

    Thread.sleep(500);                       // openHAB verarbeitet Commands asynchron
    for (String[] b : befehle) {
        IO.println("%-22s = %s".formatted(b[0], items.getItemState(b[0])));
    }
}
