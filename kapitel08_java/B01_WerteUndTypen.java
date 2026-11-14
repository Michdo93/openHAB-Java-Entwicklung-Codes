// Kapitel 8: Werte, Typen und Datenstrukturen – eine kompakte Quelldatei (Java 25)
// Start: java kapitel08_java/B01_WerteUndTypen.java
void main() {
    // Primitive Typen und ihre Hüllklassen
    int helligkeit = 70;                       // ganze Zahl
    double temperatur = 21.5;                  // Gleitkommazahl
    boolean an = true;
    Integer leer = null;                       // Hüllklasse: darf null sein
    IO.println("int " + helligkeit + ", double " + temperatur + ", boolean " + an
            + ", Integer " + leer);

    // var: Der Compiler leitet den Typ ab – er steht trotzdem fest
    var name = "Wohnzimmer_Licht";
    var zustand = an ? "ON" : "OFF";
    IO.println(name + " = " + zustand + " (" + name.length() + " Zeichen)");

    // Zeichenketten: Methoden, formatieren, Textblock
    String text = "  Küche  ";
    IO.println("[" + text.strip() + "] " + text.strip().toUpperCase()
            + " beginnt mit K: " + text.strip().startsWith("K"));
    IO.println(String.format("%-10s %5.1f °C", "Werkstatt", 18.25));
    IO.println("%s ist %s".formatted(name, zustand));
    String json = """
            {"name": "Wohnzimmer_Licht", "state": "ON"}""";
    IO.println(json);

    // Vergleiche: == prüft bei Objekten die Identität, equals den Inhalt
    String a = "ON", b = new String("ON");
    IO.println("== " + (a == b) + ", equals " + a.equals(b));

    // Felder (Arrays) haben eine feste Länge
    int[] werte = {18, 21, 19};
    IO.println("Array: " + Arrays.toString(werte) + ", Länge " + werte.length);

    // Listen, Mengen und Abbildungen
    List<String> raeume = new ArrayList<>(List.of("Wohnzimmer", "Küche"));
    raeume.add("Werkstatt");
    Set<String> typen = Set.of("Switch", "Dimmer");
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("name", "Wohnzimmer_Dimmer");
    item.put("state", 55);
    item.put("tags", List.of("Light"));
    IO.println("Liste " + raeume + ", Dimmer dabei: " + typen.contains("Dimmer"));
    IO.println("Map " + item + ", state = " + item.get("state"));
    IO.println("getOrDefault: " + item.getOrDefault("label", "(ohne Beschriftung)"));

    // Typumwandlung: aus Text wird eine Zahl und umgekehrt
    double wert = Double.parseDouble("21.5");
    int prozent = Integer.parseInt("55");
    IO.println((wert + 1) + " / " + (prozent + 1) + " / " + prozent + "1");
}
