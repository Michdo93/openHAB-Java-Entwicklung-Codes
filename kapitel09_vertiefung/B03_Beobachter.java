// Kapitel 9: Das Beobachter-Muster – ein Sensor meldet Messwerte an seine Zuhörer
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

record Messwert(String sensor, double wert) {
}

class Sensor {
    private final String name;
    private final List<Consumer<Messwert>> zuhoerer = new CopyOnWriteArrayList<>();
    private final List<Consumer<Exception>> fehlerZuhoerer =
            new CopyOnWriteArrayList<>();

    Sensor(String name) {
        this.name = name;
    }

    // anmelden – zurück kommt eine Funktion zum Abmelden
    Runnable beiMesswert(Consumer<Messwert> z) {
        zuhoerer.add(z);
        return () -> zuhoerer.remove(z);
    }

    void beiFehler(Consumer<Exception> z) {
        fehlerZuhoerer.add(z);
    }

    // misst im Hintergrund und benachrichtigt alle Zuhörer
    Thread starten(double... werte) {
        return Thread.ofVirtual().start(() -> {
            for (double w : werte) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
                if (Double.isNaN(w)) {
                    var fehler = new IllegalStateException("Messfehler");
                    fehlerZuhoerer.forEach(z -> z.accept(fehler));
                    continue;
                }
                Messwert m = new Messwert(name, w);
                zuhoerer.forEach(z -> z.accept(m));
            }
        });
    }
}

void main() throws InterruptedException {
    Sensor werkstatt = new Sensor("Werkstatt_Temperatur");
    werkstatt.beiMesswert(m -> IO.println("Anzeige:  " + m.wert() + " °C"));
    Runnable abmelden = werkstatt.beiMesswert(m -> {
        if (m.wert() > 20) {
            IO.println("Warnung:  " + m.sensor() + " zu warm");
        }
    });
    werkstatt.beiFehler(e -> IO.println("Fehler:   " + e.getMessage()));

    Thread t = werkstatt.starten(19.5, 21.0, Double.NaN);
    t.join();
    abmelden.run();                                  // Warnung abmelden
    werkstatt.starten(22.0).join();                  // nur noch die Anzeige
}
