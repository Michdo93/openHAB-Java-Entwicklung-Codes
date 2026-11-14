package integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.michdo93.openhab.testsuite.ItemTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import testhilfe.OpenHAB;

// Kapitel 19: Integrationstests mit dem ItemTester – gegen ein echtes openHAB
@DisplayName("Items der Übungsumgebung")
@EnabledIfEnvironmentVariable(named = "OPENHAB_TOKEN", matches = ".+")
class ItemsIT {

    final ItemTester tester = new ItemTester(OpenHAB.CLIENT);

    @ParameterizedTest(name = "{0} existiert als {1}")
    @CsvSource({"Wohnzimmer_Licht, Switch", "Wohnzimmer_Dimmer, Dimmer",
            "Wohnzimmer_Farblicht, Color", "Wohnzimmer_Temperatur, Number:Temperature",
            "Kueche_Rollladen, Rollershutter", "Flur_Haustuer, Contact",
            "Haus_Status, String", "gLichter, Group"})
    void existiert(String name, String typ) {
        assertTrue(tester.doesItemExist(name), name + " fehlt");
        assertTrue(tester.checkItemIsType(name, typ), name + " ist kein " + typ);
    }

    @Test
    @DisplayName("Lichtgruppe")
    void lichtgruppe() {
        assertTrue(tester.isGroupItem("gLichter"));
        assertTrue(tester.doesGroupContainMember("gLichter", "Wohnzimmer_Licht"));
        assertTrue(tester.getGroupMembers("gLichter").stream()
                .anyMatch(m -> "Wohnzimmer_Dimmer".equals(m.get("name"))));
    }

    @Test
    @DisplayName("Schalter, Dimmer, Farbe und Text reagieren")
    void reagieren() {
        assertTrue(tester.testSwitch("Wohnzimmer_Licht", "ON", "ON", 5));
        assertTrue(tester.testDimmer("Wohnzimmer_Dimmer", "40", "40", 5));
        assertTrue(tester.testColor("Wohnzimmer_Farblicht", "120,100,50", "120,100,50",
                5));
        assertTrue(tester.testString("Haus_Status", "Test", "Test", 5));
    }

    @Test
    @DisplayName("ein Kontakt lässt sich nur per Update setzen")
    void kontakt() {
        assertTrue(tester.testContact("Flur_Haustuer", "OPEN", "OPEN", 5));
    }
}
