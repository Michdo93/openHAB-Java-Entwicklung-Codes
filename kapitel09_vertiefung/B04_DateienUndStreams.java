// Kapitel 9: JSON speichern, CSV anhängen und zeilenweise auswerten (java.nio.file)
// Start: ./starte.sh kapitel09_vertiefung/B04_DateienUndStreams.java
import io.github.michdo93.openhab.Json;
import java.nio.file.StandardOpenOption;

void main() throws IOException {
    // 1. Zustand als JSON speichern und wieder laden
    Path zustand = Path.of("zustand.json");
    Map<String, Object> daten = new LinkedHashMap<>();
    daten.put("Wohnzimmer_Licht", "ON");
    daten.put("Wohnzimmer_Dimmer", 40);
    Files.writeString(zustand, Json.stringify(daten, 2));
    Map<String, Object> geladen = Json.map(Json.parse(Files.readString(zustand)));
    IO.println("geladen: " + geladen + " (" + Files.size(zustand) + " Bytes)");

    // 2. Messwerte als CSV anhängen – die Datei wächst mit jedem Lauf
    Path csv = Path.of("messwerte.csv");
    if (Files.notExists(csv)) {
        Files.writeString(csv, "zeit;item;wert\n");
    }
    List<String> zeilen = List.of(
            "2026-11-14T10:00;Werkstatt_Temperatur;18,5",
            "2026-11-14T10:05;Werkstatt_Temperatur;19,0",
            "2026-11-14T10:10;Werkstatt_Temperatur;23,5");
    Files.write(csv, zeilen, StandardOpenOption.APPEND);

    // 3. zeilenweise lesen – lines() liest erst beim Durchlaufen, auch große Dateien
    try (var lines = Files.lines(csv)) {
        DoubleSummaryStatistics stat = lines.skip(1)                     // Kopfzeile
                .map(z -> z.split(";")[2].replace(',', '.'))
                .mapToDouble(Double::parseDouble)
                .summaryStatistics();
        IO.println("Werte: %d, Mittel %.2f, Maximum %.1f"
                .formatted(stat.getCount(), stat.getAverage(), stat.getMax()));
    }

    // 4. Verzeichnisse durchsuchen
    try (var dateien = Files.list(Path.of("."))) {
        dateien.filter(p -> p.toString().matches(".*\\.(json|csv)"))
                .sorted()
                .forEach(p -> IO.println("Datei: " + p.getFileName()));
    }
    Files.delete(zustand);
    Files.delete(csv);                  // aufräumen: der nächste Lauf sieht gleich aus
}
