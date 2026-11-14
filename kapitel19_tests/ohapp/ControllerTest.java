package ohapp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Rules;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testhilfe.Attrappe;

// Kapitel 19: Unit-Tests für den Controller – openHAB wird durch Attrappen ersetzt
class ControllerTest {

    final List<String> aufrufe = new ArrayList<>();

    // Attrappen: Unterklassen der echten Klassen, die jeden Aufruf mitschreiben
    final Items items = new Items(Attrappe.client()) {
        @Override
        public Object sendCommand(String name, String command, String quelle) {
            aufrufe.add("sendCommand " + name + " " + command + " " + quelle);
            return Map.of("message", "OK");
        }
    };
    final Rules rules = new Rules(Attrappe.client()) {
        @Override
        public Object runNow(String uid) {
            aufrufe.add("runNow " + uid);
            return Map.of("message", "OK");
        }
    };

    @Test
    @DisplayName("umschalten sendet den Gegenwert, ändert das Model aber nicht")
    void umschalten() {
        var model = new ItemModel(null);
        model.setzeZustand("Wohnzimmer_Licht", "OFF", "Switch", "Deckenlicht");

        new Controller(items, rules, model).umschalten("Wohnzimmer_Licht");

        assertEquals(List.of("sendCommand Wohnzimmer_Licht ON ohapp"), aufrufe);
        assertEquals("OFF", model.get("Wohnzimmer_Licht").state());  // State-Command!
    }

    @Test
    @DisplayName("eine Szene löst die Regel aus")
    void szene() {
        new Controller(items, rules, new ItemModel(null)).szene("restbuch-1");
        assertEquals(List.of("runNow restbuch-1"), aufrufe);
    }
}
