package mcp;

// Kapitel 20: openHAB als MCP-Server für Sprachmodelle (Model Context Protocol).
// Ein MCP-fähiger Assistent startet dieses Programm und spricht über stdin/stdout mit
// ihm: eine JSON-RPC-Nachricht pro Zeile. Er erfährt, welche Werkzeuge es gibt, und
// ruft sie mit JSON-Argumenten auf. Ganz ohne Fremdbibliothek – das Protokoll ist
// klein, und die Json-Hilfsklasse der Client-Bibliothek genügt.
// Sicherheit zuerst: Das Modell sieht und bedient nur ausdrücklich freigegebene Items.
import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Json;
import io.github.michdo93.openhab.OpenHABClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenHABMcp {

    static final Set<String> FREIGEGEBEN = Set.of("Wohnzimmer_Licht",
            "Wohnzimmer_Dimmer", "Kueche_Rollladen", "Wohnzimmer_Temperatur",
            "Flur_Haustuer", "Werkstatt_Luefter");
    static final Set<String> NUR_LESEN = Set.of("Wohnzimmer_Temperatur",
            "Flur_Haustuer");

    // Die Werkzeuge: Name, Zweck für das Modell, Argumente als JSON-Schema
    static final List<Object> WERKZEUGE = List.of(
            Map.of("name", "list_devices",
                    "description", "Listet die freigegebenen Geräte mit Typ und "
                            + "Zustand.",
                    "inputSchema", Map.of("type", "object", "properties", Map.of())),
            Map.of("name", "get_state",
                    "description", "Liest den aktuellen Zustand eines Geräts.",
                    "inputSchema", Map.of("type", "object", "required", List.of("item"),
                            "properties", Map.of("item", Map.of("type", "string",
                                    "description", "Itemname")))),
            Map.of("name", "send_command",
                    "description", "Sendet einen Befehl an ein Gerät "
                            + "(ON/OFF, 0-100, UP/DOWN/STOP).",
                    "inputSchema", Map.of("type", "object",
                            "required", List.of("item", "command"),
                            "properties", Map.of("item", Map.of("type", "string"),
                                    "command", Map.of("type", "string")))));

    private final Items items;

    // Ein JSON-Objekt mit fester Reihenfolge der Felder: json("item", x, "state", y)
    static Map<String, Object> json(Object... paare) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < paare.length; i += 2) {
            m.put((String) paare[i], paare[i + 1]);
        }
        return m;
    }

    OpenHABMcp(Items items) {
        this.items = items;
    }

    // --- Die Werkzeuge ausführen ---------------------------------------------------
    Object werkzeug(String name, Map<String, Object> args) {
        String item = String.valueOf(args.get("item"));
        return switch (name) {
            case "list_devices" -> Json.list(items.getItems(
                            Map.of("fields", "name,label,type,state"))).stream()
                    .filter(i -> FREIGEGEBEN.contains((String) i.get("name")))
                    .map(i -> json("item", i.get("name"), "label", i.get("label"),
                            "type", i.get("type"), "state", i.get("state"),
                            "steuerbar", !NUR_LESEN.contains((String) i.get("name"))))
                    .toList();
            case "get_state" -> FREIGEGEBEN.contains(item)
                    ? json("item", item, "state", items.getItemState(item))
                    : Map.of("error", item + " ist nicht freigegeben");
            case "send_command" -> {
                if (!FREIGEGEBEN.contains(item) || NUR_LESEN.contains(item)) {
                    yield Map.of("error", item + " darf nicht gesteuert werden");
                }
                String command = String.valueOf(args.get("command"));
                Map<String, Object> antwort = json("item", item, "command", command);
                antwort.putAll(Json.map(items.sendCommand(item, command, "mcp")));
                yield antwort;
            }
            default -> Map.of("error", "unbekanntes Werkzeug " + name);
        };
    }

    // --- JSON-RPC: eine Anfrage beantworten (null: Benachrichtigung, keine Antwort) -
    Map<String, Object> beantworten(Map<String, Object> anfrage) {
        Object id = anfrage.get("id");
        Map<String, Object> params = anfrage.get("params") == null ? Map.of()
                : Json.map(anfrage.get("params"));
        Object ergebnis = switch (String.valueOf(anfrage.get("method"))) {
            case "initialize" -> Map.of(
                    "protocolVersion",
                    params.getOrDefault("protocolVersion", "2025-06-18"),
                    "capabilities", Map.of("tools", Map.of()),
                    "serverInfo", Map.of("name", "openhab-buch", "version", "1.0.0"));
            case "ping" -> Map.of();
            case "tools/list" -> Map.of("tools", WERKZEUGE);
            case "tools/call" -> {
                Map<String, Object> args = params.get("arguments") == null ? Map.of()
                        : Json.map(params.get("arguments"));
                Object e = werkzeug(String.valueOf(params.get("name")), args);
                yield Map.of("content", List.of(Map.of("type", "text",
                        "text", Json.stringify(e))), "isError", Json.isError(e));
            }
            default -> null;
        };
        if (id == null) {
            return null;                        // z. B. notifications/initialized
        }
        Map<String, Object> antwort = new LinkedHashMap<>();
        antwort.put("jsonrpc", "2.0");
        antwort.put("id", id);
        if (ergebnis != null) {
            antwort.put("result", ergebnis);
        } else {
            antwort.put("error", Map.of("code", -32601, "message", "Method not found"));
        }
        return antwort;
    }

    public static void main(String[] args) throws Exception {
        // stdout gehört dem Protokoll – Meldungen für Menschen gehen nach stderr
        var protokoll = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        System.setOut(System.err);              // ein vergessenes println stört nicht
        var client = new OpenHABClient(
                System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
                null, null, System.getenv("OPENHAB_TOKEN"));
        var server = new OpenHABMcp(new Items(client));
        var ein = new BufferedReader(new InputStreamReader(System.in,
                StandardCharsets.UTF_8));
        System.err.println("openHAB-MCP-Server bereit");
        String zeile;
        while ((zeile = ein.readLine()) != null) {     // eine Nachricht pro Zeile
            if (zeile.isBlank()) {
                continue;
            }
            var antwort = server.beantworten(Json.map(Json.parse(zeile)));
            if (antwort != null) {
                protokoll.println(Json.stringify(antwort));
            }
        }
    }
}
