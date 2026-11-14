// Kapitel 18: Der Rechner als Gerät – echte Messwerte des eigenen Rechners in openHAB
//  * meldet Last, Speicher, Temperatur, Netzverkehr und Laufzeit per postUpdate
//  * führt Aktionen aus, die openHAB als Command an Rechner_Aktion schickt
//  * hält ein Lebenszeichen aufrecht, das openHAB per expire überwacht
import io.github.michdo93.openhab.*;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;

final OpenHABClient client = new OpenHABClient(
        System.getenv().getOrDefault("OPENHAB_URL", "http://127.0.0.1:8080"),
        null, null, System.getenv("OPENHAB_TOKEN"));
final Items items = new Items(client);
final OperatingSystemMXBean os =
        (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
final DateTimeFormatter UHR = DateTimeFormatter.ofPattern("HH:mm:ss");
int intervall = 10;
volatile boolean aktiv = true;
volatile SSEConnection strom;

void log(String text) {
    IO.println(UHR.format(LocalTime.now()) + " " + text);
}

String zahl(String muster, double wert) {
    return String.format(Locale.ROOT, muster, wert);
}

// --- Die Items des Geräts: das Gerät legt sie selbst an (idempotent) ---------------
record ItemDef(String typ, String name, String label, String einheit, String muster) {}

final List<ItemDef> ITEMS = List.of(
        new ItemDef("Number:Dimensionless", "Rechner_Last", "Last", "%", "%.0f %%"),
        new ItemDef("Number:Dimensionless", "Rechner_Speicher", "Speicher belegt", "%",
                "%.0f %%"),
        new ItemDef("Number:Temperature", "Rechner_Temperatur", "CPU-Temperatur", "°C",
                "%.1f °C"),
        new ItemDef("Number:DataTransferRate", "Rechner_Netz", "Netzverkehr", "kbit/s",
                "%.1f kbit/s"),
        new ItemDef("Number:Time", "Rechner_Laufzeit", "Laufzeit", "h", "%.1f h"),
        new ItemDef("Switch", "Rechner_Online", "Rechner erreichbar", null, null),
        new ItemDef("String", "Rechner_Aktion", "Aktion", null, null),
        new ItemDef("String", "Rechner_Antwort", "Antwort", null, null));

void anmelden() throws Exception {
    List<Object> liste = new ArrayList<>();
    liste.add(Map.of("type", "Group", "name", "gRechner",
            "label", "Rechner " + InetAddress.getLocalHost().getHostName(),
            "tags", List.of("Equipment")));
    for (ItemDef i : ITEMS) {
        liste.add(Map.of("type", i.typ(), "name", i.name(), "label", i.label(),
                "groupNames", List.of("gRechner")));
    }
    items.addOrUpdateItems(liste);
    for (ItemDef i : ITEMS) {
        if (i.einheit() != null) {
            items.addMetadata(i.name(), "unit", Map.of("value", i.einheit()));
            items.addMetadata(i.name(), "stateDescription", Map.of("value", " ",
                    "config", Map.of("pattern", i.muster())));
        }
    }
    items.addMetadata("Rechner_Online", "expire",
            Map.of("value", intervall * 3 + "s,state=OFF"));
    items.addMetadata("Rechner_Aktion", "autoupdate", Map.of("value", "false"));
    items.addMetadata("Rechner_Aktion", "stateDescription", Map.of("value", " ",
            "config", Map.of("options", "ERREICHBARKEIT=openHAB anpingen,"
                    + "DATENTRAEGER=Datenträger,PROZESSE=Prozesse")));
    log("angemeldet: " + ITEMS.size() + " Items in gRechner");
}

// --- Messen ----------------------------------------------------------------------
long netzVorherBytes = -1, netzVorherZeit;

long netzBytes() {                               // Summe aller Schnittstellen (Linux)
    try {
        List<String> zeilen = Files.readAllLines(Path.of("/proc/net/dev"));
        long summe = 0;
        for (String z : zeilen.subList(2, zeilen.size())) {
            if (z.contains(":") && !z.strip().startsWith("lo:")) {
                String[] f = z.split(":")[1].strip().split("\\s+");
                summe += Long.parseLong(f[0]) + Long.parseLong(f[8]);  // rein + raus
            }
        }
        return summe;
    } catch (Exception e) {
        return -1;                               // kein Linux: kein Netzwert
    }
}

String temperatur() {
    try {
        Path sensor = Path.of("/sys/class/thermal/thermal_zone0/temp");
        String milli = Files.readString(sensor);
        return zahl("%.1f °C", Long.parseLong(milli.strip()) / 1000.0);
    } catch (Exception e) {
        return "UNDEF";                          // ohne Sensor: »undefiniert« melden
    }
}

String laufzeit() {                              // Laufzeit des Rechners, nicht der JVM
    try {
        String sekunden = Files.readString(Path.of("/proc/uptime")).split(" ")[0];
        return zahl("%.1f h", Double.parseDouble(sekunden) / 3600);
    } catch (Exception e) {
        return "UNDEF";
    }
}

void messenUndMelden() {
    double last = os.getCpuLoad() * 100;         // Auslastung aller Kerne, 0 bis 100
    double speicher = (1 - (double) os.getFreeMemorySize() / os.getTotalMemorySize())
            * 100;
    long bytes = netzBytes(), jetzt = System.currentTimeMillis();
    String netz = "UNDEF";
    if (bytes >= 0 && netzVorherBytes >= 0) {
        double sekunden = (jetzt - netzVorherZeit) / 1000.0;
        netz = zahl("%.1f kbit/s", (bytes - netzVorherBytes) * 8 / 1000.0 / sekunden);
    }
    if (bytes >= 0) {
        netzVorherBytes = bytes;
        netzVorherZeit = jetzt;
    }
    Map<String, String> werte = new LinkedHashMap<>();
    werte.put("Rechner_Last", zahl("%.0f %%", last));
    werte.put("Rechner_Speicher", zahl("%.0f %%", speicher));
    werte.put("Rechner_Temperatur", temperatur());
    werte.put("Rechner_Netz", netz);
    werte.put("Rechner_Laufzeit", laufzeit());
    werte.put("Rechner_Online", "ON");                          // Lebenszeichen
    werte.forEach(items::postUpdate);
    log("gemeldet: Last " + werte.get("Rechner_Last") + ", Speicher "
            + werte.get("Rechner_Speicher") + ", Temperatur "
            + werte.get("Rechner_Temperatur") + ", Netz " + netz);
}

// --- Aktionen: nur diese drei, jede mit festem Programm und festen Argumenten -----
String ausgabe(String... befehl) throws Exception {
    Process p = new ProcessBuilder(befehl).redirectErrorStream(true).start();
    if (!p.waitFor(5, TimeUnit.SECONDS)) {
        p.destroyForcibly();
        throw new IllegalStateException("Zeitüberschreitung");
    }
    return new String(p.getInputStream().readAllBytes()).strip();
}

final Map<String, Callable<String>> AKTIONEN = Map.of(
        "ERREICHBARKEIT", () -> {
            long start = System.nanoTime();
            client.get("/");                     // Anfrage an die Wurzel der REST API
            return "openHAB antwortet in " + (System.nanoTime() - start) / 1_000_000
                    + " ms";
        },
        "DATENTRAEGER", () -> {
            String zeile = ausgabe("df", "-h", "--output=pcent,size", "/").lines()
                    .toList().get(1);
            String[] f = zeile.strip().split("\\s+");
            return "/ belegt: " + f[0] + " von " + f[1];
        },
        "PROZESSE", () ->                        // ganz ohne externes Programm
                ProcessHandle.allProcesses().count() + " Prozesse laufen");

void ausfuehren(String command) {
    Callable<String> aktion = AKTIONEN.get(command);
    String antwort;
    try {
        antwort = aktion != null ? aktion.call() : "unbekannte Aktion " + command;
    } catch (Exception e) {
        antwort = "Fehler: " + e.getMessage();
    }
    log("Aktion " + command + ": " + antwort);
    items.postUpdate("Rechner_Antwort", antwort);
    items.postUpdate("Rechner_Aktion", command);              // Bestätigung als State
}

// --- Commands empfangen, mit Neuverbinden ----------------------------------------
void commandsEmpfangen() {
    while (aktiv) {
        SSEConnection s = new ItemEvents(client).ItemCommandEvent("Rechner_Aktion");
        if (s.isError()) {
            log("kein Ereignisstrom: " + s.error().get("error"));
        } else {
            strom = s;
            log("warte auf Commands für Rechner_Aktion");
            for (var e : s.events()) {
                ausfuehren(String.valueOf(Json.get(e, "payload", "value")));
            }
        }
        if (aktiv) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}

void abmelden(String grund) {
    if (!aktiv) {
        return;                                  // schon abgemeldet
    }
    aktiv = false;
    if (strom != null) {
        strom.close();
    }
    items.postUpdate("Rechner_Online", "OFF");
    log("abgemeldet (" + grund + ")");
}

// ./starte.sh kapitel18_philosophie2/RechnerGeraet.java [--intervall=10] [--dauer=60]
void main(String[] args) throws Exception {
    int dauer = 0;
    for (String a : args) {
        if (a.startsWith("--intervall=")) {
            intervall = Integer.parseInt(a.substring(12));
        } else if (a.startsWith("--dauer=")) {
            dauer = Integer.parseInt(a.substring(8));
        }
    }
    anmelden();
    var takt = Executors.newSingleThreadScheduledExecutor();
    takt.scheduleAtFixedRate(this::messenUndMelden, 0, intervall, TimeUnit.SECONDS);
    Thread.ofVirtual().start(this::commandsEmpfangen);
    // SIGINT und SIGTERM (systemctl stop) beenden die JVM – vorher abmelden
    Runtime.getRuntime().addShutdownHook(new Thread(() -> abmelden("Signal")));
    if (dauer > 0) {
        Thread.sleep(dauer * 1000L);
        abmelden("Dauer abgelaufen");
        System.exit(0);
    }
}
