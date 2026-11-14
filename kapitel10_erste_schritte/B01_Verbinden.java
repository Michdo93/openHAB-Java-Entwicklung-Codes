// Kapitel 10: Verbindung zu openHAB herstellen – mit Token oder Basic Auth
import io.github.michdo93.openhab.*;

void main() {
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    String token = System.getenv("OPENHAB_TOKEN");

    OpenHABClient client = token != null
            ? new OpenHABClient(url, null, null, token)             // Variante 1: Token
            : new OpenHABClient(url, System.getenv("OPENHAB_USER"), // Variante 2:
                                System.getenv("OPENHAB_PASSWORD")); // Basic Auth
    if (!client.isLoggedIn) {                       // der Konstruktor hat schon geprüft
        IO.println("Keine Verbindung zu " + url);
        System.exit(1);
    }

    // Die Wurzel der REST API: Version und Liste aller Endpunkte
    Map<String, Object> wurzel = Json.map(client.get("/"));
    Object version = Json.get(wurzel, "runtimeInfo", "version");
    IO.println("openHAB " + version + " unter " + url);
    IO.println("  Sprache/Land:   " + wurzel.get("locale")
            + ", Maßsystem " + wurzel.get("measurementSystem"));
    IO.println("  Endpunkte:      " + Json.array(wurzel.get("links")).size());

    var info = Json.map(Json.get(new Systeminfo(client).getSystemInfo(), "systemInfo"));
    IO.println("  Betriebssystem: " + info.get("osName") + " " + info.get("osVersion")
            + " (" + info.get("osArchitecture") + ")");
    IO.println("  Java (openHAB): " + info.get("javaVersion")
            + " von " + info.get("javaVendor"));
    IO.println("  Konfiguration:  " + info.get("configFolder"));
    long frei = ((Number) info.get("freeMemory")).longValue() >> 20;     // Bytes -> MiB
    long gesamt = ((Number) info.get("totalMemory")).longValue() >> 20;
    IO.println("  Speicher:       " + frei + " von " + gesamt + " MiB frei");
    IO.println("  Instanz-UUID:   " + new UUID(client).getUUID());
    IO.println("  Java (hier):    " + Runtime.version()
            + " unter " + System.getProperty("os.name"));
}
