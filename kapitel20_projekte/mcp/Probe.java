// Kapitel 20: Den MCP-Server ohne Sprachmodell ausprobieren –
// genau so startet und ruft ihn ein Assistent auf.
import io.github.michdo93.openhab.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

PrintStream an;
BufferedReader von;
int naechsteId = 1;

// Argumente in fester Reihenfolge, damit die Ausgabe lesbar bleibt
Map<String, Object> args(String... paare) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < paare.length; i += 2) {
        m.put(paare[i], paare[i + 1]);
    }
    return m;
}

// Eine JSON-RPC-Anfrage schicken und auf die Antwort mit derselben id warten
Map<String, Object> anfrage(String methode, Map<String, Object> params)
        throws Exception {
    int id = naechsteId++;
    an.println(Json.stringify(Map.of("jsonrpc", "2.0", "id", id, "method", methode,
            "params", params)));
    while (true) {
        var antwort = Json.map(Json.parse(von.readLine()));
        if (((Number) antwort.get("id")).intValue() == id) {
            return Json.map(antwort.get("result"));
        }
    }
}

void main() throws Exception {
    Process server = new ProcessBuilder("java", "-cp", "target/ohwerkzeuge.jar",
            "mcp.OpenHABMcp")
            .redirectError(ProcessBuilder.Redirect.INHERIT)   // Meldungen des Servers
            .start();                          // Adresse und Token erbt er von uns
    an = new PrintStream(server.getOutputStream(), true, StandardCharsets.UTF_8);
    von = new BufferedReader(new InputStreamReader(server.getInputStream(),
            StandardCharsets.UTF_8));

    // Handschlag: initialize, dann die Benachrichtigung initialized (ohne id)
    anfrage("initialize", Map.of("protocolVersion", "2025-06-18",
            "capabilities", Map.of(), "clientInfo", Map.of("name", "probe",
                    "version", "1.0.0")));
    an.println(Json.stringify(Map.of("jsonrpc", "2.0",
            "method", "notifications/initialized")));

    var werkzeuge = Json.list(anfrage("tools/list", Map.of()).get("tools"));
    IO.println("Werkzeuge: " + werkzeuge.stream().map(w -> (String) w.get("name"))
            .collect(java.util.stream.Collectors.joining(", ")));

    // So sähen die Aufrufe aus, die ein Modell auf »Mach es gemütlich« erzeugen könnte
    record Aufruf(String name, Map<String, Object> args) {}
    for (var a : List.of(
            new Aufruf("list_devices", args()),
            new Aufruf("send_command",
                    args("item", "Wohnzimmer_Dimmer", "command", "30")),
            new Aufruf("send_command",
                    args("item", "Kueche_Rollladen", "command", "DOWN")),
            new Aufruf("send_command",
                    args("item", "Flur_Haustuer", "command", "OPEN")),
            new Aufruf("get_state", args("item", "Wohnzimmer_Dimmer")))) {
        var antwort = anfrage("tools/call", Map.of("name", a.name(),
                "arguments", a.args()));
        String text = (String) Json.get(antwort, "content", 0, "text");
        String abgelehnt = Boolean.TRUE.equals(antwort.get("isError"))
                ? " – abgelehnt" : "";
        IO.println(a.name() + "(" + Json.stringify(a.args()) + ")" + abgelehnt);
        IO.println("  -> " + (text.length() > 76 ? text.substring(0, 76) + " …"
                                                 : text));
    }
    an.close();                                 // stdin zu: der Server endet
    server.waitFor();
}
