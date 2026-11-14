// Kapitel 8: Verzweigungen, Schleifen und switch
void main() {
    double temperatur = 23.4;

    // if / else if / else
    if (temperatur > 24) {
        IO.println("warm");
    } else if (temperatur > 19) {
        IO.println("angenehm");
    } else {
        IO.println("kühl");
    }

    // switch als Ausdruck: liefert einen Wert, kein break nötig
    String zustand = "OPEN";
    String anzeige = switch (zustand) {
        case "OPEN" -> "offen";
        case "CLOSED" -> "geschlossen";
        case "NULL", "UNDEF" -> "unbekannt";
        default -> "? " + zustand;
    };
    IO.println("Haustür: " + anzeige);

    // Schleifen
    List<String> raeume = List.of("Wohnzimmer", "Küche", "Werkstatt");
    for (String raum : raeume) {                       // über eine Sammlung
        IO.println("- " + raum);
    }
    for (int i = 0; i < raeume.size(); i += 2) {       // mit Zähler
        IO.println(i + ": " + raeume.get(i));
    }
    int versuch = 0;
    while (versuch < 3) {                              // solange die Bedingung gilt
        versuch++;
        if (versuch == 2) {
            continue;                                  // nächster Durchlauf
        }
        IO.println("Versuch " + versuch);
    }

    // Musterabgleich mit instanceof und switch über Typen
    Object wert = 55L;                                 // Zahlen aus JSON sind Long
    if (wert instanceof Long l && l > 50) {
        IO.println("Long über 50: " + l);
    }
    for (Object o : List.of("ON", 21.5, 3L, List.of("Light"))) {
        String art = switch (o) {
            case String s -> "Text " + s;
            case Double d -> "Kommazahl " + d;
            case Long n when n < 10 -> "kleine Zahl " + n;
            case List<?> liste -> "Liste mit " + liste.size() + " Element(en)";
            default -> "etwas anderes";
        };
        IO.println(art);
    }
}
