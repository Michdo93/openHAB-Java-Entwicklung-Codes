// Kapitel 15: Änderungen auf einer Sitemap-Seite live verfolgen
import io.github.michdo93.openhab.*;
import java.util.concurrent.atomic.AtomicBoolean;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Sitemaps sitemaps = new Sitemaps(client);
final Items items = new Items(client);
final String SITEMAP = "buch_tablet";                    // aus B02_SitemapAnlegen

void main() {
    // 1. Abonnement anlegen und an eine Seite binden:
    //    die Seite einmal mit der ID abrufen
    String abo = (String) Json.get(sitemaps.subscribeToSitemapEvents(),
            "subscriptionID");
    Object seite = sitemaps.getSitemapPage(SITEMAP, SITEMAP, abo);
    IO.println("Abonnement " + abo.substring(0, 8) + "… für Seite »"
            + Json.get(seite, "title") + "«");

    // 2. Etwas ändern, das auf der Seite zu sehen ist. Achtung: openHAB antwortet
    //    auf den Ereignisstrom erst mit dem ersten Ereignis – deshalb die Änderung
    //    vorher einplanen.
    String dimmer = items.getItemState("Wohnzimmer_Dimmer").toString();
    String tuer = items.getItemState("Flur_Haustuer").toString();
    var offen = new AtomicBoolean(false);
    Thread.startVirtualThread(() -> {
        try {
            Thread.sleep(1000);
            items.sendCommand("Wohnzimmer_Dimmer", dimmer.equals("35") ? "65" : "35");
            items.postUpdate("Flur_Haustuer", tuer.equals("OPEN") ? "CLOSED" : "OPEN");
            Thread.sleep(4000);  // kam nichts an? Noch eine Änderung
            if (!offen.get()) {
                items.sendCommand("Wohnzimmer_Dimmer", "50");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    });

    // 3. Den Strom lesen: Sitemap-Ereignisse haben kein "topic" -> includeAlive = true
    try (SSEConnection strom = sitemaps.getSitemapEvents(abo, SITEMAP, SITEMAP)) {
        offen.set(true);
        int anzahl = 0;
        for (var e : strom.events(true)) {
            if (e.get("widgetId") == null) {
                continue;
            }
            IO.println("  Widget %-5s %-24s Zustand %s".formatted(e.get("widgetId"),
                    e.get("label"), Json.get(e, "item", "state")));
            if (++anzahl == 3) {
                break;                     // try-with-resources schließt den Strom
            }
        }
    }
    items.postUpdate("Flur_Haustuer", tuer);
    IO.println("Sitemap löschen: "
            + new UI(client).deleteUIComponent("system:sitemap", SITEMAP));
}
