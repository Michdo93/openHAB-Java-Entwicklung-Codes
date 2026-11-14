package backend;

// Kapitel 20: Das Backend – Model, Controller und Konsole aus Kapitel 17, dazu eine
// HTTP-View für Browser. Start nach mvn package aus dem Repository:
//   java -cp target/ohwerkzeuge.jar backend.Backend
import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.OpenHABClient;
import io.github.michdo93.openhab.Rules;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import ohapp.Controller;
import ohapp.ItemModel;
import ohapp.KonsolenView;
import ohapp.Main;

public class Backend {

    public static void main(String[] args) throws Exception {
        var env = System.getenv();
        String host = env.getOrDefault("HOST", "127.0.0.1");
        int port = Integer.parseInt(env.getOrDefault("PORT", "3000"));

        // Das Token bleibt hier im Backend – kein Browser bekommt es je zu sehen
        var client = new OpenHABClient(
                env.getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
                null, null, env.get("OPENHAB_TOKEN"));
        var model = new ItemModel(Main.WOHNZIMMER);                          // Model
        var controller = new Controller(new Items(client), new Rules(client), model);
        new KonsolenView(model);                                             // View 1
        var http = new HttpView(model, controller, Set.of("restbuch-1"));   // View 2

        // Ereignisstrom: derselbe Hintergrund-Leser wie in Kapitel 17
        Thread.ofVirtual().start(() -> Main.ereignisseLesen(client, model,
                new CountDownLatch(1)));
        http.starten(host, port);
        System.out.println("Backend läuft: http://" + host + ":" + port + "/");

        // Sauber beenden: Browser trennen, Server zu
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Backend wird beendet");
            http.stoppen();
        }));
        String demo = env.get("DEMO_SEKUNDEN");
        if (demo != null) {
            Thread.sleep(Long.parseLong(demo) * 1000);
            System.exit(0);
        }
    }
}
