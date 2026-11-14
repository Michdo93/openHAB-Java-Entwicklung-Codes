package ohapp;

import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Json;
import io.github.michdo93.openhab.Rules;

/** Controller: übersetzt Benutzeraktionen in REST-Aufrufe (State-Command-Pattern). */
public class Controller {

    private final Items items;
    private final Rules rules;
    private final ItemModel model;

    public Controller(Items items, Rules rules, ItemModel model) {
        this.items = items;
        this.rules = rules;
        this.model = model;
    }

    /** Schalter umlegen – der neue Zustand kommt über den Ereignisstrom. */
    public Object umschalten(String name) {
        return senden(name, "ON".equals(model.get(name).state()) ? "OFF" : "ON");
    }

    public Object senden(String name, String command) {
        Object antwort = items.sendCommand(name, command, "ohapp");
        if (Json.isError(antwort)) {
            System.err.println("Command " + command + " an " + name + " abgelehnt: "
                    + Json.error(antwort));
        }
        // Bewusst KEIN model.setzeZustand(…):
        // openHAB ist die Quelle der Wahrheit (State-Command-Pattern).
        return antwort;
    }

    /** Eine vorhandene openHAB-Regel auslösen, statt ihre Logik nachzubauen. */
    public Object szene(String regelUid) {
        Object antwort = rules.runNow(regelUid);
        if (Json.isError(antwort)) {
            System.err.println("Regel " + regelUid + ": " + Json.error(antwort));
        }
        return antwort;
    }
}
