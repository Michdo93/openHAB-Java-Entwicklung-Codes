package ohapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Kapitel 19: Unit-Tests für das Model aus Kapitel 17 – ganz ohne openHAB
class ItemModelTest {

    @Test
    @DisplayName("das Model meldet nur echte Änderungen")
    void nurEchteAenderungen() {
        var model = new ItemModel(null);
        List<String> protokoll = new ArrayList<>();
        model.anmelden((item, alt) -> protokoll.add(item.name() + " " + alt + " -> "
                + item.state()));

        model.setzeZustand("Wohnzimmer_Licht", "ON", "Switch", "Deckenlicht");
        model.setzeZustand("Wohnzimmer_Licht", "ON");                // keine Änderung
        model.setzeZustand("Wohnzimmer_Licht", "OFF");

        assertEquals(List.of("Wohnzimmer_Licht NULL -> ON",
                "Wohnzimmer_Licht ON -> OFF"), protokoll);
    }

    @Test
    @DisplayName("das Model nimmt nur die gewünschten Items auf")
    void nurGewuenschteItems() {
        var model = new ItemModel(List.of("Wohnzimmer_Licht"));
        model.laden(List.of(Map.of("name", "Wohnzimmer_Licht", "state", "ON"),
                Map.of("name", "Kueche_Rollladen", "state", "0")));
        assertEquals(List.of("Wohnzimmer_Licht"),
                model.alle().stream().map(Item::name).toList());
        assertFalse(model.kennt("Kueche_Rollladen"));
    }

    @Test
    @DisplayName("alle liefert eine Kopie, die das Model nicht verändert")
    void kopie() {
        var model = new ItemModel(null);
        model.setzeZustand("Haus_Status", "Alle zu Hause", "String", "Status");
        model.alle().clear();
        assertEquals("Alle zu Hause", model.get("Haus_Status").state());
    }
}
