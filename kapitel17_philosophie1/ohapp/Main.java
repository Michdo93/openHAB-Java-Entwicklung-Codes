package ohapp;

import io.github.michdo93.openhab.ItemEvents;
import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Json;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.Rules;
import io.github.michdo93.openhab.SSEConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Startet die MVC-Anwendung:
 *  ./starte.sh kapitel17_philosophie1/ohapp/Main.java [--gui] [--demo SEKUNDEN] */
public class Main {

    public static final List<String> WOHNZIMMER = List.of(
            "Wohnzimmer_Licht", "Wohnzimmer_Dimmer", "Wohnzimmer_Temperatur",
            "Kueche_Rollladen", "Flur_Haustuer", "Haus_Status");

    /** Hintergrund-Thread: Momentaufnahme laden, dann jedem statechanged folgen. */
    public static void ereignisseLesen(OpenHABClient client, ItemModel model,
                                       CountDownLatch verbunden) {
        Items items = new Items(client);
        while (true) {
            try (SSEConnection strom = new ItemEvents(client).ItemStateChangedEvent()) {
                if (strom.isError()) {                // openHAB nicht erreichbar
                    System.err.println("Ereignisstrom: " + strom.error().get("error"));
                } else {
                    model.laden(Json.list(items.getItems(Map.of(
                            "fields", "name,type,label,state"))));    // Momentaufnahme
                    verbunden.countDown();
                    for (var e : strom.events()) {
                        model.setzeZustand(((String) e.get("topic")).split("/")[2],
                                String.valueOf(Json.get(e, "payload", "value")));
                    }
                }
            }
            schlafen(2000);              // Strom beendet: kurz warten, neu verbinden
        }
    }

    /** Spielt einen Benutzer, der ein paar Knöpfe drückt (für --demo). */
    static void demo(Controller controller) {
        List<Runnable> schritte = List.of(
                () -> controller.umschalten("Wohnzimmer_Licht"),
                () -> controller.senden("Wohnzimmer_Dimmer", "70"),
                () -> controller.senden("Kueche_Rollladen", "50"),
                () -> controller.szene("restbuch-1"));
        for (Runnable schritt : schritte) {
            schlafen(1000);
            schritt.run();
        }
    }

    static void schlafen(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        boolean gui = List.of(args).contains("--gui");
        int i = List.of(args).indexOf("--demo");
        double demoSekunden = i >= 0 ? Double.parseDouble(args[i + 1]) : 0;

        var client = new OpenHABClient(
                System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
                null, null, System.getenv("OPENHAB_TOKEN"));
        var model = new ItemModel(WOHNZIMMER);                                 // Model
        var controller = new Controller(new Items(client), new Rules(client), model);
        new KonsolenView(model);                                               // View 1

        var verbunden = new CountDownLatch(1);
        Thread.ofVirtual().start(() -> ereignisseLesen(client, model, verbunden));
        verbunden.await(5, TimeUnit.SECONDS);

        if (gui) {
            new FxView(model, controller).starten();                           // View 2
        } else {
            System.out.println(
                    "Konsole aktiv – Änderungen erscheinen hier. Strg+C beendet.");
            if (demoSekunden > 0) {
                Thread.ofVirtual().start(() -> demo(controller));
                Thread.sleep((long) (demoSekunden * 1000));
            } else {
                Thread.currentThread().join();                  // bis Strg+C
            }
        }
        System.exit(0);
    }
}
