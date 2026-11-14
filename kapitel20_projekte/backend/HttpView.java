package backend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.michdo93.openhab.Json;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ohapp.Controller;
import ohapp.Item;
import ohapp.ItemBeobachter;
import ohapp.ItemModel;

/** Die dritte View des ohapp-Models: eine HTTP-Schnittstelle für Browser – JSON,
 *  Commands und ein eigener SSE-Strom, gebaut mit dem HttpServer des JDK. */
public class HttpView implements ItemBeobachter {

    static final Path SEITEN = Path.of("kapitel20_projekte/backend/public");
    static final Map<String, String> DATEIEN = Map.of(
            "/", "index.html:text/html", "/app.js", "app.js:text/javascript");

    private final ItemModel model;
    private final Controller controller;
    private final Set<String> szenen;                // nur diese Regeln dürfen Browser
    private final Set<OutputStream> browser = new CopyOnWriteArraySet<>();
    private final ScheduledExecutorService puls =
            Executors.newSingleThreadScheduledExecutor();
    private HttpServer server;

    public HttpView(ItemModel model, Controller controller, Set<String> szenen) {
        this.model = model;
        this.controller = controller;
        this.szenen = szenen;
        model.anmelden(this);
    }

    static Map<String, Object> alsMap(Item i) {             // feste Reihenfolge im JSON
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", i.name());
        m.put("typ", i.typ());
        m.put("label", i.label());
        m.put("state", i.state());
        return m;
    }

    // --- Jede Änderung im Model geht an alle verbundenen Browser -------------------
    @Override
    public void itemGeaendert(Item item, String alt) {
        verteilen("data: " + Json.stringify(alsMap(item)) + "\n\n");
    }

    private void verteilen(String text) {
        byte[] daten = text.getBytes(StandardCharsets.UTF_8);
        for (OutputStream aus : browser) {
            try {
                aus.write(daten);
                aus.flush();
            } catch (IOException e) {
                browser.remove(aus);                      // Browser hat die Seite zu
            }
        }
    }

    // --- Anfragen --------------------------------------------------------------------
    private void antworten(HttpExchange ex, int code, String typ, byte[] daten)
            throws IOException {
        ex.getResponseHeaders().set("Content-Type", typ + "; charset=utf-8");
        ex.sendResponseHeaders(code, daten.length);
        try (OutputStream aus = ex.getResponseBody()) {
            aus.write(daten);
        }
    }

    private void json(HttpExchange ex, Object daten) throws IOException {
        int code = Json.isError(daten) ? 400 : 200;
        antworten(ex, code, "application/json",
                Json.stringify(daten).getBytes(StandardCharsets.UTF_8));
    }

    private void bearbeiten(HttpExchange ex) throws IOException {
        String methode = ex.getRequestMethod(), pfad = ex.getRequestURI().getPath();
        String[] teile = pfad.split("/");                 // "", "api", "items", NAME
        if (methode.equals("GET") && DATEIEN.containsKey(pfad)) {
            String[] datei = DATEIEN.get(pfad).split(":");
            antworten(ex, 200, datei[1], Files.readAllBytes(SEITEN.resolve(datei[0])));
        } else if (methode.equals("GET") && pfad.equals("/api/items")) {
            json(ex, model.alle().stream().map(HttpView::alsMap).toList());
        } else if (methode.equals("GET") && pfad.equals("/api/ereignisse")) {
            ereignisstrom(ex);
        } else if (methode.equals("POST") && teile.length == 4
                && teile[2].equals("items")) {
            String name = teile[3];
            String text = new String(ex.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8).strip();
            if (!model.kennt(name)) {               // Freigabe: nur Items des Models
                json(ex, Map.of("error", "Item " + name + " ist nicht freigegeben"));
            } else {
                json(ex, text.equals("TOGGLE") ? controller.umschalten(name)
                                               : controller.senden(name, text));
            }
        } else if (methode.equals("POST") && teile.length == 4
                && teile[2].equals("szenen")) {
            String uid = teile[3];
            json(ex, szenen.contains(uid) ? controller.szene(uid)
                    : Map.of("error", "Szene " + uid + " ist nicht freigegeben"));
        } else {
            antworten(ex, 404, "application/json",
                    "{\"error\":\"nicht gefunden\"}".getBytes(StandardCharsets.UTF_8));
        }
    }

    // Eigener SSE-Strom: Momentaufnahme, danach jede Änderung. Die Antwort bleibt
    // offen und wandert in die Menge der Browser.
    private void ereignisstrom(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "text/event-stream");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, 0);                   // 0 = Länge offen (chunked)
        OutputStream aus = ex.getResponseBody();
        List<Map<String, Object>> alle = model.alle().stream().map(HttpView::alsMap)
                .toList();
        aus.write(("event: momentaufnahme\ndata: " + Json.stringify(alle) + "\n\n")
                .getBytes(StandardCharsets.UTF_8));
        aus.flush();
        browser.add(aus);
    }

    public void starten(String host, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", ex -> {
            try {
                bearbeiten(ex);
            } catch (Exception e) {
                json(ex, Map.of("error", String.valueOf(e.getMessage())));
            }
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        // Lebenszeichen alle 15 s, damit Proxys und Browser die Verbindung halten
        puls.scheduleAtFixedRate(() -> verteilen(": alive\n\n"), 15, 15,
                TimeUnit.SECONDS);
    }

    public void stoppen() {
        puls.shutdownNow();
        for (OutputStream aus : browser) {
            try {
                aus.close();
            } catch (IOException ignored) {
            }
        }
        server.stop(0);
    }
}
