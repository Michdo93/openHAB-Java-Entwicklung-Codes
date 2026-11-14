package testhilfe;

import com.sun.net.httpserver.HttpServer;
import io.github.michdo93.openhab.OpenHABClient;
import java.io.IOException;
import java.net.InetSocketAddress;

/** Ein Client für Unit-Tests: Er spricht mit einem winzigen HTTP-Server im Test
 *  selbst, der jede Anfrage mit 200 beantwortet. So entsteht ein gültiger Client,
 *  ohne dass ein openHAB läuft – die Attrappen überschreiben dann die Methoden. */
public final class Attrappe {

    private static OpenHABClient client;

    private Attrappe() {
    }

    public static synchronized OpenHABClient client() {
        if (client == null) {
            try {
                var adresse = new InetSocketAddress("127.0.0.1", 0);   // freier Port
                HttpServer server = HttpServer.create(adresse, 0);
                server.createContext("/", austausch -> {
                    austausch.sendResponseHeaders(200, -1);
                    austausch.close();
                });
                server.start();
                int port = server.getAddress().getPort();
                client = new OpenHABClient("http://127.0.0.1:" + port, null, null,
                        "attrappe", 2);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return client;
    }
}
