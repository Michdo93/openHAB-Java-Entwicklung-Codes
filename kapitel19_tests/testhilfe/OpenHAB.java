package testhilfe;

import io.github.michdo93.openhab.OpenHABClient;

/** Der gemeinsame Client aller Integrationstests – Adresse und Token aus der Umgebung.
 *  Ohne OPENHAB_TOKEN überspringt JUnit die Tests (@EnabledIfEnvironmentVariable). */
public final class OpenHAB {

    public static final OpenHABClient CLIENT = new OpenHABClient(
            System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
            null, null, System.getenv("OPENHAB_TOKEN"));

    private OpenHAB() {
    }
}
