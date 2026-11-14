// Kapitel 8: Methoden, Überladen, variable Parameter und Lambdas
import java.util.function.Function;
import java.util.function.Predicate;

// Eine Methode mit Rückgabewert
double celsiusZuFahrenheit(double celsius) {
    return celsius * 9 / 5 + 32;
}

// Überladen: gleicher Name, andere Parameter – so bietet die Bibliothek
// "optionale" Parameter an
String schalten(String item) {
    return schalten(item, "ON");
}

String schalten(String item, String befehl) {
    return item + " <- " + befehl;
}

// Variable Anzahl von Parametern
double mittelwert(double... werte) {
    double summe = 0;
    for (double w : werte) {
        summe += w;
    }
    return werte.length == 0 ? 0 : summe / werte.length;
}

// Eine Methode, die eine Funktion als Parameter bekommt
List<String> filtern(List<String> namen, Predicate<String> bedingung) {
    List<String> treffer = new ArrayList<>();
    for (String n : namen) {
        if (bedingung.test(n)) {
            treffer.add(n);
        }
    }
    return treffer;
}

void main() {
    IO.println("21.5 °C = " + celsiusZuFahrenheit(21.5) + " °F");
    IO.println(schalten("Wohnzimmer_Licht"));
    IO.println(schalten("Wohnzimmer_Dimmer", "40"));
    IO.println("Mittelwert: " + mittelwert(18, 21, 19.5));

    List<String> items =
            List.of("Wohnzimmer_Licht", "Kueche_Rollladen", "Wohnzimmer_Dimmer");
    // Lambda: eine Funktion ohne Namen
    IO.println(filtern(items, n -> n.startsWith("Wohnzimmer")));
    // Methodenreferenz: eine vorhandene Methode als Funktion
    Function<String, Integer> laenge = String::length;
    IO.println("Länge: " + laenge.apply("Flur_Haustuer"));

    // Rückruffunktion: wird später aufgerufen
    Runnable meldung = () -> IO.println("Fertig.");
    meldung.run();
}
