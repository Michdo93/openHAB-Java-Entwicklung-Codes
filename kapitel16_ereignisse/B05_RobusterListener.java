// Kapitel 16: Ein robuster Ereignis-Listener – Neuverbinden, Wachhund, sauberes Beenden
import io.github.michdo93.openhab.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
static final DateTimeFormatter UHR = DateTimeFormatter.ofPattern("HH:mm:ss");

static void log(String text) {
    IO.println(UHR.format(LocalTime.now()) + " " + text);
}

static class EreignisListener {
    private final Events events;
    private final String topics;
    private final Consumer<Map<String, Object>> verarbeiten;
    private final long wachhund;                 // openHAB schickt alle 10 s ein ALIVE
    private final CountDownLatch beendet = new CountDownLatch(1);
    private volatile boolean aktiv;
    private volatile SSEConnection strom;
    private volatile long letzteNachricht;

    EreignisListener(OpenHABClient client, String topics,
                     Consumer<Map<String, Object>> verarbeiten, int wachhundSekunden) {
        this.events = new Events(client);
        this.topics = topics;
        this.verarbeiten = verarbeiten;
        this.wachhund = wachhundSekunden * 1000L;
    }

    void start() throws InterruptedException {
        aktiv = true;
        long pause = 1000;
        Thread wache = Thread.startVirtualThread(this::wachen);
        while (aktiv) {
            SSEConnection s = events.getEvents(topics);
            if (s.isError()) {
                String text = (String) s.error().get("error");
                text = text.replaceAll(" \\(http.*", "");     // ohne die Adresse
                log("keine Verbindung: " + text + " – neuer Versuch in "
                        + pause / 1000 + " s");
            } else {
                strom = s;
                log("verbunden");
                pause = 1000;                     // Erfolg: Wartezeit zurücksetzen
                lesen(s);
                if (aktiv) {
                    log("Verbindung beendet");
                }
                strom = null;
            }
            if (!aktiv) {
                break;
            }
            Thread.sleep(pause);
            pause = Math.min(pause * 2, 30000);  // exponentielles Backoff bis 30 s
        }
        wache.interrupt();
        log("Listener beendet");
        beendet.countDown();
    }

    private void lesen(SSEConnection s) {
        letzteNachricht = System.currentTimeMillis();
        for (var e : s.events(true)) {
            letzteNachricht = System.currentTimeMillis();
            if (e.get("topic") != null) {
                verarbeiten.accept(e);
            }
        }
    }

    // Wachhund: Stille bedeutet eine tote Verbindung
    private void wachen() {
        try {
            while (true) {
                Thread.sleep(1000);
                SSEConnection s = strom;
                long still = System.currentTimeMillis() - letzteNachricht;
                if (s != null && still > wachhund) {
                    log("Wachhund: keine Nachricht – Verbindung wird neu aufgebaut");
                    s.close();
                }
            }
        } catch (InterruptedException e) {
            // Listener beendet
        }
    }

    void trennen() {
        SSEConnection s = strom;
        if (s != null) {
            s.close();
        }
    }

    void stop() {
        aktiv = false;
        trennen();
    }

    boolean warteAufEnde(long ms) throws InterruptedException {
        return beendet.await(ms, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}

void main(String[] args) throws InterruptedException {
    var listener = new EreignisListener(client, "openhab/items/*/statechanged",
            e -> log(((String) e.get("topic")).split("/")[2] + " = "
                    + Json.get(e, "payload", "value")), 25);

    // Strg+C oder systemctl stop: Strom schließen, Schleife verlassen, Ende abwarten
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (listener.aktiv) {
            log("Signal – beende den Listener");
            listener.stop();
            try {
                listener.warteAufEnde(5000);
            } catch (InterruptedException ignored) {
            }
        }
    }));

    // Test: --trennen kappt die Verbindung nach drei Sekunden,
    //       --dauer=20 beendet den Listener nach 20 Sekunden
    for (String a : args) {
        if (a.equals("--trennen")) {
            Thread.startVirtualThread(() -> {
                schlafe(3000);
                log("TEST: Verbindung gekappt");
                listener.trennen();
            });
        } else if (a.startsWith("--dauer=")) {
            long ms = Long.parseLong(a.substring(8)) * 1000;
            Thread.startVirtualThread(() -> {
                schlafe(ms);
                listener.stop();
            });
        }
    }
    listener.start();
}

static void schlafe(long ms) {
    try {
        Thread.sleep(ms);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
