// Kapitel 8: Generische Typen, versiegelte Schnittstellen und Record-Muster

// Eine generische Klasse: T steht für einen beliebigen Typ
class Puffer<T> {
    private final Deque<T> werte = new ArrayDeque<>();
    private final int groesse;

    Puffer(int groesse) {
        this.groesse = groesse;
    }

    void hinzu(T wert) {
        if (werte.size() == groesse) {
            werte.removeFirst();                      // ältesten Wert verwerfen
        }
        werte.addLast(wert);
    }

    List<T> inhalt() {
        return List.copyOf(werte);
    }
}

// Eine generische Methode
<T extends Comparable<T>> T groesster(List<T> werte) {
    T max = werte.getFirst();
    for (T w : werte) {
        if (w.compareTo(max) > 0) {
            max = w;
        }
    }
    return max;
}

// sealed: Es gibt genau diese drei Arten von Ereignissen
sealed interface Ereignis permits Zustand, Befehl, Lebenszeichen {
}

record Zustand(String item, String alt, String neu) implements Ereignis {
}

record Befehl(String item, String befehl) implements Ereignis {
}

record Lebenszeichen() implements Ereignis {
}

String beschreiben(Ereignis e) {
    // der Compiler prüft, dass alle Fälle behandelt sind – kein default nötig
    return switch (e) {
        case Zustand(String item, String alt, String neu) ->
                item + ": " + alt + " -> " + neu;
        case Befehl(var item, var befehl) -> item + " soll " + befehl;
        case Lebenszeichen l -> "(lebt)";
    };
}

void main() {
    var puffer = new Puffer<Double>(3);
    for (double t : new double[] {18.0, 18.5, 19.0, 19.5}) {
        puffer.hinzu(t);
    }
    List<Double> inhalt = puffer.inhalt();
    IO.println("Puffer: " + inhalt + ", größter: " + groesster(inhalt));
    IO.println("größter Name: " + groesster(List.of("Flur", "Wohnzimmer", "Küche")));

    List<Ereignis> ereignisse = List.of(
            new Befehl("Wohnzimmer_Licht", "ON"),
            new Zustand("Wohnzimmer_Licht", "OFF", "ON"),
            new Lebenszeichen());
    ereignisse.forEach(e -> IO.println(beschreiben(e)));
}
