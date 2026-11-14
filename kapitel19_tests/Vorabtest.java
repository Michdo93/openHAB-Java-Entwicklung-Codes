// Kapitel 19: Vorab-Test, bevor ein Dienst startet.
// Prüft in Sekunden alles, was die Anwendung aus Kapitel 17 und die Geräte aus
// Kapitel 18 voraussetzen, und endet mit Exit-Code 0 (bereit) oder 1 (nicht starten!).
// Ideal als ExecStartPre= in einer systemd-Unit oder als Schritt einer CI-Pipeline.
import io.github.michdo93.openhab.*;
import io.github.michdo93.openhab.testsuite.*;

final Map<String, String> ITEMS = Map.of(
        "Wohnzimmer_Licht", "Switch", "Wohnzimmer_Dimmer", "Dimmer",
        "Kueche_Rollladen", "Rollershutter", "Werkstatt_Luefter", "Switch",
        "Werkstatt_Temperatur", "Number:Temperature", "Haus_Status", "String");
final List<String> THINGS = List.of("astro:sun:zuhause");
final List<String> REGELN = List.of("restbuch-1");
final String SITEMAP = "restbuch";
final List<String> AUF_DER_SITEMAP = List.of("Werkstatt_Luefter", "Wohnzimmer_Licht");

void main() {
    var client = new OpenHABClient(
            System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
            null, null, System.getenv("OPENHAB_TOKEN"), 5);
    if (!client.isLoggedIn) {
        IO.println("NICHT BEREIT: openHAB nicht erreichbar");
        System.exit(1);
    }
    var it = new ItemTester(client);
    var tt = new ThingTester(client);
    var rt = new RuleTester(client);
    var st = new SitemapTester(client);
    List<String> fehler = new ArrayList<>();

    ITEMS.forEach((name, typ) -> {
        if (!it.doesItemExist(name)) {
            fehler.add("Item fehlt: " + name);
        } else if (!it.checkItemIsType(name, typ)) {
            fehler.add("Item " + name + " ist kein " + typ);
        }
    });
    THINGS.stream().filter(uid -> !tt.isThingOnline(uid))
            .forEach(uid -> fehler.add("Thing nicht ONLINE: " + uid));
    REGELN.stream().filter(uid -> !rt.isRuleIdle(uid))
            .forEach(uid -> fehler.add("Regel nicht bereit: " + uid));
    AUF_DER_SITEMAP.stream().filter(n -> !st.doesSitemapContainItem(SITEMAP, n))
            .forEach(n -> fehler.add("Sitemap " + SITEMAP + " zeigt " + n + " nicht"));
    var pt = new PersistenceTester(client);
    if (!pt.isItemPersisted("inmemory", "Wohnzimmer_Dimmer")) {
        fehler.add("Wohnzimmer_Dimmer wird in inmemory nicht gespeichert");
    }

    IO.println("=".repeat(50));
    if (fehler.isEmpty()) {
        IO.println("BEREIT – " + ITEMS.size() + " Items, " + THINGS.size() + " Thing, "
                + REGELN.size() + " Regel, Sitemap und Persistenz geprüft");
    } else {
        IO.println("NICHT BEREIT:\n  " + String.join("\n  ", fehler));
        System.exit(1);
    }
}
