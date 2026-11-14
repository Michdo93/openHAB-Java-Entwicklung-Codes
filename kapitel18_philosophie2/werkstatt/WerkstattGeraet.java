package werkstatt;

// Kapitel 18: Ein virtuelles Gerät, das sich wie ein echtes in openHAB einbindet.
// Philosophie 2: Das Programm ist die Quelle der Wahrheit für seine Items.
//  * Es MELDET Messwerte und Zustände per postUpdate (wie ein Binding).
//  * Es EMPFÄNGT Commands über den Ereignisstrom und bestätigt sie per postUpdate.
//  * Es meldet sich mit einem Lebenszeichen an, das openHAB per expire überwacht.
import io.github.michdo93.openhab.ItemEvents;
import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Json;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.SSEConnection;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WerkstattGeraet {

    static final String LUEFTER = "Werkstatt_Luefter";
    static final String TEMP = "Werkstatt_Temperatur";
    static final String FEUCHTE = "Werkstatt_Feuchte";
    static final String MELDUNG = "Werkstatt_LetzteMeldung";
    static final String ONLINE = "Werkstatt_Online";
    static final double GRENZE_C = 24.0;           // ab hier lüftet das Gerät selbst
    static final DateTimeFormatter UHR = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final OpenHABClient client;
    private final Items items;
    private final int intervall;
    private final Sensor sensor;
    private final ScheduledExecutorService takt =
            Executors.newSingleThreadScheduledExecutor();
    private volatile boolean luefterAn;
    private volatile boolean aktiv = true;
    private volatile SSEConnection strom;

    public WerkstattGeraet(OpenHABClient client, int intervall, Sensor sensor) {
        this(client, new Items(client), intervall, sensor);
    }

    // Für Tests: eine Attrappe statt der echten Items (paketweit sichtbar)
    WerkstattGeraet(OpenHABClient client, Items items, int intervall, Sensor sensor) {
        this.client = client;
        this.items = items;
        this.intervall = intervall;
        this.sensor = sensor;
    }

    static void log(String text) {
        System.out.println(UHR.format(LocalTime.now()) + " " + text);
    }

    public boolean luefterAn() {
        return luefterAn;
    }

    // --- Anmelden: eigenes Item für das Lebenszeichen anlegen (falls nötig) ----------
    public void anmelden() {
        if (Json.isError(items.getItem(ONLINE))) {
            items.addOrUpdateItem(ONLINE, Map.of("type", "Switch", "name", ONLINE,
                    "label", "Werkstatt-Gerät erreichbar",
                    "groupNames", List.of("gWerkstatt"), "tags", List.of("Status")));
        }
        // fällt nach drei Intervallen ohne Meldung automatisch auf OFF
        items.addMetadata(ONLINE, "expire",
                Map.of("value", intervall * 3 + "s,state=OFF"));
    }

    // --- States melden ---------------------------------------------------------------
    void meldeAlles() {
        items.postUpdate(LUEFTER, luefterAn ? "ON" : "OFF");
        messenUndMelden();
    }

    synchronized void messenUndMelden() {
        Sensor.Messung m = sensor.messen(luefterAn);
        items.postUpdate(TEMP, String.format(Locale.ROOT, "%.1f °C", m.temperatur()));
        items.postUpdate(FEUCHTE, m.feuchte() + " %");
        items.postUpdate(MELDUNG, Instant.now().toString());
        items.postUpdate(ONLINE, "ON");                            // Lebenszeichen
        log(String.format(Locale.ROOT, "gemeldet: %.1f °C, %d %%, Lüfter %s",
                m.temperatur(), m.feuchte(), luefterAn ? "an" : "aus"));
        if (m.temperatur() > GRENZE_C && !luefterAn) {             // lokal entscheiden
            log("zu warm – Lüfter wird lokal eingeschaltet");
            schalten(true);
        }
    }

    // --- Commands empfangen ----------------------------------------------------------
    void schalten(boolean an) {
        luefterAn = an;                               // hier säße die echte Hardware
        items.postUpdate(LUEFTER, an ? "ON" : "OFF");  // Bestätigung als State
    }

    void commandsEmpfangen() {
        while (aktiv) {
            SSEConnection s = new ItemEvents(client).ItemCommandEvent(LUEFTER);
            if (s.isError()) {
                log("kein Ereignisstrom: " + s.error().get("error"));
            } else {
                strom = s;
                log("warte auf Commands für " + LUEFTER);
                meldeAlles();                  // nach (Wieder-)Verbinden alles melden
                for (var e : s.events()) {
                    Object command = Json.get(e, "payload", "value");
                    log("Command empfangen: " + LUEFTER + " <- " + command);
                    if ("ON".equals(command) || "OFF".equals(command)) {
                        schalten("ON".equals(command));
                    }
                }
            }
            if (aktiv) {
                schlafen(aktiv && s.isError() ? 5000 : 2000);
            }
        }
    }

    public void laufen() {
        Thread.ofVirtual().start(this::commandsEmpfangen);
        takt.scheduleAtFixedRate(this::messenUndMelden, intervall, intervall,
                TimeUnit.SECONDS);
    }

    public void abmelden() {
        aktiv = false;
        takt.shutdownNow();
        SSEConnection s = strom;
        if (s != null) {
            s.close();
        }
        items.postUpdate(ONLINE, "OFF");
        log("abgemeldet");
    }

    static void schlafen(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --- Hauptprogramm --------------------------------------------------------------
    // ./starte.sh kapitel18_philosophie2/werkstatt/WerkstattGeraet.java
    //     [--intervall=10] [--dauer=60]
    static int option(String[] args, String name, int standard) {
        for (String a : args) {
            if (a.startsWith("--" + name + "=")) {
                return Integer.parseInt(a.substring(name.length() + 3));
            }
        }
        return standard;
    }

    public static void main(String[] args) throws InterruptedException {
        var client = new OpenHABClient(
                System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
                null, null, System.getenv("OPENHAB_TOKEN"));
        var geraet = new WerkstattGeraet(client, option(args, "intervall", 10),
                new SimulierterSensor());
        geraet.anmelden();
        geraet.laufen();
        // Strg+C oder systemctl stop: sauber abmelden, bevor die JVM endet
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (geraet.aktiv) {
                geraet.abmelden();
            }
        }));
        int dauer = option(args, "dauer", 0);
        if (dauer > 0) {
            Thread.sleep(dauer * 1000L);
            geraet.abmelden();
            System.exit(0);
        }
    }
}
