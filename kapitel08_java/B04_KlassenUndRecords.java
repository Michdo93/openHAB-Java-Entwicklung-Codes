// Kapitel 8: Klassen, Vererbung, Schnittstellen, Records und Aufzählungen

// Eine Aufzählung: genau diese Werte gibt es
enum Zustand { AN, AUS, UNBEKANNT }

// Ein Record: unveränderlicher Datenträger – Konstruktor, Getter, equals,
// hashCode und toString schreibt der Compiler
record Messwert(String item, double wert, String einheit) {
    Messwert {                                       // Prüfung im Konstruktor
        if (item.isBlank()) {
            throw new IllegalArgumentException("item fehlt");
        }
    }

    String anzeige() {
        return "%s: %.1f %s".formatted(item, wert, einheit);
    }
}

// Eine Schnittstelle legt fest, was eine Klasse können muss
interface Geraet {
    String name();

    default String beschreibung() {                  // Methode mit Rumpf
        return "Gerät " + name();
    }
}

// Eine abstrakte Klasse mit Zustand und einer Methode für die Unterklassen
abstract class Sensor implements Geraet {
    private final String name;                       // privat: nur über Methoden
    protected int messungen = 0;

    Sensor(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    abstract Messwert messen();
}

class Temperatursensor extends Sensor {
    private final double basis;

    Temperatursensor(String name, double basis) {
        super(name);                                 // Konstruktor der Oberklasse
        this.basis = basis;
    }

    @Override
    Messwert messen() {
        messungen++;
        return new Messwert(name(), basis + messungen * 0.1, "°C");
    }
}

class Schalter implements Geraet {
    private Zustand zustand = Zustand.UNBEKANNT;
    private final String name;

    Schalter(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    void schalten(boolean an) {
        zustand = an ? Zustand.AN : Zustand.AUS;
    }

    Zustand zustand() {
        return zustand;
    }

    @Override
    public String beschreibung() {
        return "Schalter " + name + " (" + zustand + ")";
    }
}

void main() {
    var werkstatt = new Temperatursensor("Werkstatt_Temperatur", 18.0);
    var licht = new Schalter("Wohnzimmer_Licht");
    licht.schalten(true);

    List<Geraet> geraete = List.of(werkstatt, licht);   // beide sind Geräte
    for (Geraet g : geraete) {
        IO.println(g.beschreibung());
    }

    Messwert m1 = werkstatt.messen();
    Messwert m2 = werkstatt.messen();
    IO.println(m1);                                        // toString des Records
    IO.println(m2.anzeige() + ", Wert " + m2.wert());
    var gleich = new Messwert("Werkstatt_Temperatur", 18.1, "°C");
    IO.println("gleich? " + m1.equals(gleich));
    IO.println("Zustand " + licht.zustand() + ", alle: "
            + Arrays.toString(Zustand.values()));
    try {
        new Messwert(" ", 1, "°C");
    } catch (IllegalArgumentException e) {
        IO.println("abgelehnt: " + e.getMessage());
    }
}
