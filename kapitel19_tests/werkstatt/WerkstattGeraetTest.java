package werkstatt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.michdo93.openhab.Items;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testhilfe.Attrappe;

// Kapitel 19: Das Werkstattgerät aus Kapitel 18 testen – mit Sensor auf Bestellung
class WerkstattGeraetTest {

    final Map<String, String> gemeldet = new LinkedHashMap<>();
    final Items items = new Items(Attrappe.client()) {
        @Override
        public Object postUpdate(String name, String state) {
            gemeldet.put(name, state);                // nur den letzten Wert merken
            return Map.of("message", "OK");
        }
    };

    // Ein Sensor, der liefert, was der Test vorgibt
    WerkstattGeraet geraet(Sensor.Messung... messungen) {
        var werte = new ArrayDeque<>(List.of(messungen));
        return new WerkstattGeraet(Attrappe.client(), items, 10, an -> werte.poll());
    }

    @Test
    @DisplayName("das Gerät meldet Messwerte und ein Lebenszeichen")
    void meldet() {
        geraet(new Sensor.Messung(21.5, 55)).messenUndMelden();
        gemeldet.remove("Werkstatt_LetzteMeldung");                  // Zeit egal
        assertEquals(Map.of("Werkstatt_Temperatur", "21.5 °C",
                "Werkstatt_Feuchte", "55 %", "Werkstatt_Online", "ON"), gemeldet);
    }

    @Test
    @DisplayName("zu warm: das Gerät schaltet den Lüfter selbst ein und bestätigt")
    void zuWarm() {
        var g = geraet(new Sensor.Messung(24.6, 60));
        g.messenUndMelden();
        assertTrue(g.luefterAn());
        assertEquals("ON", gemeldet.get("Werkstatt_Luefter"));
    }
}
