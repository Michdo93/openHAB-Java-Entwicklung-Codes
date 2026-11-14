// Kapitel 9: Unter der Haube – die REST API mit java.net.http und mit der Bibliothek
// Start: ./starte.sh kapitel09_vertiefung/B05_HttpClientRoh.java
import io.github.michdo93.openhab.Items;
import io.github.michdo93.openhab.Json;
import io.github.michdo93.openhab.OpenHABClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {
    String url = System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080");
    String token = System.getenv("OPENHAB_TOKEN");

    // 1. von Hand: Anfrage bauen, senden, Status prüfen, Text zerlegen
    HttpClient http = HttpClient.newHttpClient();
    URI adresse = URI.create(url + "/rest/items/Wohnzimmer_Licht");
    HttpRequest anfrage = HttpRequest.newBuilder(adresse)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .GET()
            .build();
    HttpResponse<String> antwort =
            http.send(anfrage, HttpResponse.BodyHandlers.ofString());
    IO.println("Status " + antwort.statusCode() + ", "
            + antwort.headers().firstValue("Content-Type").orElse("?"));
    Map<String, Object> item = Json.map(Json.parse(antwort.body()));
    IO.println("von Hand:   " + item.get("name") + " = " + item.get("state"));

    HttpRequest befehl = HttpRequest.newBuilder(adresse)
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString("OFF"))
            .build();
    var gesendet = http.send(befehl, HttpResponse.BodyHandlers.ofString());
    IO.println("Command:    Status " + gesendet.statusCode());

    // 2. dasselbe mit der Bibliothek
    Items items = new Items(new OpenHABClient(url, null, null, token));
    IO.println("Bibliothek: " + Json.str(items.getItem("Wohnzimmer_Licht"), "state"));
    IO.println("Command:    " + items.sendCommand("Wohnzimmer_Licht", "ON"));
    IO.println("Fehler:     " + items.getItem("Gibtsnicht"));
}
