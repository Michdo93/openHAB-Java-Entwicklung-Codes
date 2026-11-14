// Kapitel 9: Umgebung, Kindprozess, Logging und sauberes Beenden
// Läuft, bis Strg+C gedrückt wird; DEMO_SEKUNDEN=3 beendet nach 3 Sekunden.
import java.util.concurrent.atomic.AtomicBoolean;

final System.Logger log = System.getLogger("demo");
final AtomicBoolean laufen = new AtomicBoolean(true);

void main() throws Exception {
    // Umgebungsvariablen und Systemeigenschaften
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    IO.println("openHAB:  " + url);
    boolean token = System.getenv("OPENHAB_TOKEN") != null;
    IO.println("Token:    " + (token ? "gesetzt" : "fehlt"));
    IO.println("Java " + Runtime.version().feature() + " auf "
            + System.getProperty("os.name") + ", "
            + Runtime.getRuntime().availableProcessors() + " Prozessorkern(e)");

    // ein anderes Programm starten und seine Ausgabe lesen
    Process p = new ProcessBuilder("uname", "-sr").redirectErrorStream(true).start();
    String ausgabe = new String(p.getInputStream().readAllBytes()).strip();
    IO.println("uname:    " + ausgabe + " (Code " + p.waitFor() + ")");

    // Logging mit Stufen – landet auf stderr, nicht zwischen den Nutzdaten
    log.log(System.Logger.Level.INFO, "Dienst startet");

    // Strg+C oder kill: die JVM ruft vor dem Beenden die Shutdown-Hooks auf
    Thread hauptThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (!laufen.getAndSet(false)) {
            return;                              // normales Ende: nichts zu tun
        }
        IO.println("Signal empfangen – räume auf ...");
        try {
            hauptThread.join(2000);        // dem Hauptprogramm Zeit zum Aufräumen geben
        } catch (InterruptedException ignored) {
        }
    }));

    int sekunden = Integer.parseInt(System.getenv().getOrDefault("DEMO_SEKUNDEN", "0"));
    long ende = System.currentTimeMillis() + sekunden * 1000L;
    int runde = 0;
    while (laufen.get() && (sekunden == 0 || System.currentTimeMillis() < ende)) {
        IO.println("Runde " + ++runde);
        Thread.sleep(1000);
    }
    log.log(System.Logger.Level.INFO, "Dienst beendet nach " + runde + " Runden");
    laufen.set(false);
}
