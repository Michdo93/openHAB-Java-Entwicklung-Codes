// Kapitel 8: Sammlungen sortieren, filtern, gruppieren – mit Streams
import java.util.stream.Collectors;

record Item(String name, String typ, String zustand, String raum) {
}

void main() {
    List<Item> items = List.of(
            new Item("Wohnzimmer_Licht", "Switch", "ON", "Wohnzimmer"),
            new Item("Wohnzimmer_Dimmer", "Dimmer", "40", "Wohnzimmer"),
            new Item("Kueche_Licht", "Switch", "OFF", "Küche"),
            new Item("Kueche_Rollladen", "Rollershutter", "100", "Küche"),
            new Item("Werkstatt_Luefter", "Switch", "ON", "Werkstatt"));

    // filtern und umwandeln
    List<String> eingeschaltet = items.stream()
            .filter(i -> i.typ().equals("Switch") && i.zustand().equals("ON"))
            .map(Item::name)
            .toList();
    IO.println("an: " + eingeschaltet);

    // zählen, prüfen, erstes Element
    long schalter = items.stream().filter(i -> i.typ().equals("Switch")).count();
    boolean rollladen = items.stream().anyMatch(i -> i.typ().equals("Rollershutter"));
    IO.println(schalter + " Schalter, Rollladen vorhanden: " + rollladen);
    items.stream().filter(i -> i.raum().equals("Keller")).findFirst()
            .ifPresentOrElse(IO::println, () -> IO.println("kein Item im Keller"));

    // sortieren
    List<Item> sortiert = new ArrayList<>(items);
    sortiert.sort(Comparator.comparing(Item::raum).thenComparing(Item::name));
    sortiert.forEach(i -> IO.println("  " + i.raum() + " / " + i.name()));

    // gruppieren: Raum -> Namen
    Map<String, List<String>> proRaum = items.stream().collect(Collectors.groupingBy(
            Item::raum, TreeMap::new,
            Collectors.mapping(Item::name, Collectors.toList())));
    proRaum.forEach((raum, namen) -> IO.println(raum + ": " + namen));

    // eine Map durchlaufen und ändern
    Map<String, Integer> zaehler = new TreeMap<>();
    for (Item i : items) {
        zaehler.merge(i.typ(), 1, Integer::sum);
    }
    IO.println(zaehler);
    IO.println(String.join(", ", zaehler.keySet()));
}
