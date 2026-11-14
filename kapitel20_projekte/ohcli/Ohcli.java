package ohcli;

// Kapitel 20: ohcli – openHAB auf der Kommandozeile
//
//   ohcli info                                 Version und System
//   ohcli list [--typ Switch] [--tag Light]    Items auflisten
//   ohcli get NAME | send NAME CMD | update NAME STATE
//   ohcli watch [MUSTER]                       Zustandsänderungen live verfolgen
//   ohcli history NAME [--stunden 24] [--dienst rrd4j]
//   ohcli things                               Geräte und ihr Status
//   ohcli rules | run UID                      Regeln auflisten und ausführen
//   ohcli sag "Schalte das Deckenlicht ein"    Text wie einen Sprachbefehl auswerten
import io.github.michdo93.openhab.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Ohcli {

    // Ein Befehl bekommt die freien Argumente und die Optionen (--name wert)
    interface Befehl {
        void ausfuehren(List<String> args, Map<String, String> opt) throws Exception;
    }

    static OpenHABClient client;
    static Items items;

    /** Fehlerobjekte der Bibliothek in eine Meldung und den Exit-Code 1 übersetzen. */
    static Object pruefen(Object antwort) {
        if (Json.isError(antwort)) {
            System.err.println("Fehler: " + Json.error(antwort));
            System.exit(1);
        }
        return antwort;
    }

    static final Map<String, Befehl> BEFEHLE = new LinkedHashMap<>();

    static {
        BEFEHLE.put("info", (args, opt) -> {
            Object s = Json.get(pruefen(new Systeminfo(client).getSystemInfo()),
                    "systemInfo");
            Object wurzel = pruefen(client.get("/"));         // die Wurzel der REST API
            Object version = Json.get(wurzel, "runtimeInfo", "version");
            System.out.printf("openHAB %s, Java %s, %s %s%n", version,
                    Json.get(s, "javaVersion"), Json.get(s, "osName"),
                    Json.get(s, "osVersion"));
        });
        BEFEHLE.put("list", (args, opt) -> {
            Map<String, Object> filter = new HashMap<>();
            filter.put("fields", "name,type,state");
            if (opt.containsKey("typ")) {
                filter.put("type", opt.get("typ"));
            }
            if (opt.containsKey("tag")) {
                filter.put("tags", opt.get("tag"));
            }
            Json.list(pruefen(items.getItems(filter))).stream()
                    .sorted(Comparator.comparing(i -> (String) i.get("name")))
                    .forEach(i -> System.out.printf("%-28s %-22s %s%n", i.get("name"),
                            i.get("type"), i.get("state")));
        });
        BEFEHLE.put("get", (args, opt) ->
                System.out.println(pruefen(items.getItemState(args.get(0)))));
        BEFEHLE.put("send", (args, opt) ->
                pruefen(items.sendCommand(args.get(0), args.get(1), "ohcli")));
        BEFEHLE.put("update", (args, opt) ->
                pruefen(items.postUpdate(args.get(0), args.get(1), "ohcli")));
        BEFEHLE.put("watch", (args, opt) -> {
            String muster = args.isEmpty() ? "*" : args.get(0);
            SSEConnection strom = new ItemEvents(client).ItemStateChangedEvent(muster);
            if (strom.isError()) {
                pruefen(strom.error());
            }
            // Strg+C: die JVM ruft den Hook, der Strom wird sauber geschlossen
            Runtime.getRuntime().addShutdownHook(new Thread(strom::close));
            var uhr = DateTimeFormatter.ofPattern("HH:mm:ss");
            for (var e : strom.events()) {
                String name = ((String) e.get("topic")).split("/")[2];
                System.out.printf("%s  %-26s %s -> %s%n", uhr.format(LocalTime.now()),
                        name, Json.get(e, "payload", "oldValue"),
                        Json.get(e, "payload", "value"));
            }
        });
        BEFEHLE.put("history", (args, opt) -> {
            Instant ende = Instant.now();
            Instant start = ende.minus(Duration.ofHours(
                    Long.parseLong(opt.getOrDefault("stunden", "24"))));
            Object daten = pruefen(new Persistence(client).getItemPersistenceData(
                    args.get(0), opt.getOrDefault("dienst", "rrd4j"),
                    start.toString(), ende.toString()));
            var format = DateTimeFormatter.ofPattern("dd.MM., HH:mm")
                    .withZone(ZoneId.systemDefault());
            for (var p : Json.list(Json.get(daten, "data"))) {
                long ms = ((Number) p.get("time")).longValue();
                System.out.println(format.format(Instant.ofEpochMilli(ms)) + "  "
                        + p.get("state"));
            }
        });
        BEFEHLE.put("things", (args, opt) -> {
            for (var t : Json.list(pruefen(new Things(client).getThings(true)))) {
                System.out.printf("%-28s %-14s %s%n", t.get("UID"),
                        Json.get(t, "statusInfo", "status"), t.get("label"));
            }
        });
        BEFEHLE.put("rules", (args, opt) -> {
            Object regeln = pruefen(new Rules(client).getRules(null, null, true));
            for (var r : Json.list(regeln)) {
                System.out.printf("%-20s %-14s %s%n", r.get("uid"),
                        Json.get(r, "status", "status"), r.get("name"));
            }
        });
        BEFEHLE.put("run", (args, opt) ->
                pruefen(new Rules(client).runNow(args.get(0))));
        BEFEHLE.put("sag", (args, opt) -> {
            var voice = new Voice(client);
            System.out.println(pruefen(
                    voice.interpretTextBatch(args.get(0), List.of("system"), "de")));
        });
    }

    public static void main(String[] argv) throws Exception {
        // Kommandozeile zerlegen: --name wert sind Optionen, alles andere Positionen
        List<String> positionen = new ArrayList<>();
        Map<String, String> opt = new HashMap<>();
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].startsWith("--") && i + 1 < argv.length) {
                opt.put(argv[i].substring(2), argv[++i]);
            } else {
                positionen.add(argv[i]);
            }
        }
        Befehl befehl = positionen.isEmpty() ? null : BEFEHLE.get(positionen.get(0));
        if (befehl == null) {
            System.err.println("Aufruf: ohcli " + String.join("|", BEFEHLE.keySet())
                    + " …");
            System.exit(2);
        }
        client = new OpenHABClient(
                System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
                null, null, System.getenv("OPENHAB_TOKEN"));
        items = new Items(client);
        befehl.ausfuehren(positionen.subList(1, positionen.size()), opt);
    }
}
