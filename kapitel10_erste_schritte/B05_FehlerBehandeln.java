// Kapitel 10: Fehler behandeln
import io.github.michdo93.openhab.*;

void main() {
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    var client = new OpenHABClient(url, null, null, System.getenv("OPENHAB_TOKEN"));
    var items = new Items(client);

    // Alle Methoden melden Ablehnungen als Ergebnis {error=…} – sie werfen nicht
    String[][] versuche = {
        {"Wohnzimer_Licht", "ON"},       // Tippfehler im Namen
        {"Wohnzimmer_Licht", "HALB"},    // ungültiger Command
        {"Flur_Haustuer", "OPEN"},       // Contact: kein Command
        {"Wohnzimmer_Licht", "OFF"},
    };
    for (String[] v : versuche) {
        Object antwort = items.sendCommand(v[0], v[1]);
        IO.println(Json.isError(antwort)
                ? "FEHLER  " + v[0] + " <- " + v[1] + ": " + Json.error(antwort)
                : "OK      " + v[0] + " <- " + v[1]);
    }

    // Ein Item aus einer .items-Datei lässt sich steuern, aber nicht löschen
    IO.println("\nlöschen: " + items.deleteItem("Wohnzimmer_Licht"));

    // Falsches Token: openHAB lehnt ab, die Bibliothek reicht die Begründung weiter
    var fremd = new OpenHABClient(url, null, null, "oh.falsch.123"); // warnt (stderr)
    Object abgelehnt = new Items(fremd).getItemState("Wohnzimmer_Licht");
    IO.println("falsches Token: " + Json.error(abgelehnt));

    // Netzwerkfehler: ein Server, den es nicht gibt, zwei Sekunden Wartezeit
    var weg = new OpenHABClient("http://127.0.0.1:9", null, null, null, 2);
    IO.println("anderer Server: " + new Items(weg).getItemState("Wohnzimmer_Licht"));

    // Wer lieber mit Ausnahmen arbeitet: Json.map wirft bei einem Fehlerergebnis
    try {
        Json.map(items.getItem("Gibtsnicht"));
    } catch (IllegalStateException e) {
        IO.println("Ausnahme: " + e.getMessage());
    }
}
