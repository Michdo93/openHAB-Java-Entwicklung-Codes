package integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Links;
import io.github.michdo93.openhab.testsuite.ChannelTester;
import io.github.michdo93.openhab.testsuite.PersistenceTester;
import io.github.michdo93.openhab.testsuite.RuleTester;
import io.github.michdo93.openhab.testsuite.SitemapTester;
import io.github.michdo93.openhab.testsuite.ThingTester;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import testhilfe.OpenHAB;

// Kapitel 19: Integrationstests für Things, Regeln, Links, Persistenz und Sitemaps
@DisplayName("Things, Regeln und mehr")
@EnabledIfEnvironmentVariable(named = "OPENHAB_TOKEN", matches = ".+")
class SystemIT {

    static final String SONNE = "astro:sun:zuhause";
    static final String KANAL = SONNE + ":position#elevation";

    @Test
    @DisplayName("das Astro-Thing ist online und lässt sich abschalten")
    void thing() throws InterruptedException {
        var tester = new ThingTester(OpenHAB.CLIENT);
        assertTrue(tester.isThingOnline(SONNE));
        assertTrue(tester.disableThing(SONNE));
        assertTrue(tester.isThingUninitialized(SONNE));
        assertTrue(tester.enableThing(SONNE));
        Thread.sleep(2000);
        assertTrue(tester.isThingOnline(SONNE));
    }

    @Test
    @DisplayName("die Regel »Gute Nacht« setzt den Hausstatus")
    void regel() {
        var tester = new RuleTester(OpenHAB.CLIENT);
        assertTrue(tester.isRuleIdle("restbuch-1"));
        assertTrue(tester.testRuleExecution("restbuch-1", "Haus_Status", "Nachtruhe"));
    }

    @Nested
    @DisplayName("ein Item mit Link")
    class MitLink {
        final Items items = new Items(OpenHAB.CLIENT);
        final Links links = new Links(OpenHAB.CLIENT);

        @BeforeEach
        void anlegen() {                                // anlegen und verknüpfen …
            items.addOrUpdateItem("Test_Hoehe", Map.of("type", "Number:Angle",
                    "name", "Test_Hoehe"));
            links.linkItemToChannel("Test_Hoehe", KANAL, Map.of());
        }

        @AfterEach
        void aufraeumen() {                             // … und danach wieder aufräumen
            links.deleteAllLinks("Test_Hoehe");
            items.deleteItem("Test_Hoehe");
        }

        @Test
        @DisplayName("der Link ist vorhanden, verwaiste Links gibt es nicht")
        void link() {
            var tester = new ChannelTester(OpenHAB.CLIENT);
            assertTrue(tester.isItemLinkedToChannel("Test_Hoehe", KANAL));
            assertFalse(tester.isItemLinkedToAnyChannel("Wohnzimmer_Licht"));
            assertFalse(tester.hasOrphanedLinks());
        }
    }

    @Test
    @DisplayName("der Dimmer wird in inmemory gespeichert")
    void persistenz() throws InterruptedException {
        new Items(OpenHAB.CLIENT).sendCommand("Wohnzimmer_Dimmer", "33");
        Thread.sleep(1000);
        var tester = new PersistenceTester(OpenHAB.CLIENT);
        Instant jetzt = Instant.now(), vorher = jetzt.minusSeconds(60);
        assertTrue(tester.isItemPersisted("inmemory", "Wohnzimmer_Dimmer"));
        assertTrue(tester.hasDataInRange("inmemory", "Wohnzimmer_Dimmer",
                vorher.toString(), jetzt.toString()));
        assertTrue(tester.checkLastPersistedState("inmemory", "Wohnzimmer_Dimmer",
                "33"));
    }

    @Test
    @DisplayName("die Sitemap zeigt den Lüfter")
    void sitemap() {
        var tester = new SitemapTester(OpenHAB.CLIENT);
        assertTrue(tester.doesSitemapExist("restbuch"));
        assertTrue(tester.doesSitemapContainItem("restbuch", "Werkstatt_Luefter"));
        assertFalse(tester.doesSitemapContainItem("restbuch", "Gibt_es_nicht"));
    }
}
