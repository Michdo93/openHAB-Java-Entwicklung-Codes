package ohapp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** View 1: jede Änderung als Zeile auf der Konsole. */
public class KonsolenView implements ItemBeobachter {

    private static final DateTimeFormatter UHR =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public KonsolenView(ItemModel model) {
        model.anmelden(this);
    }

    @Override
    public void itemGeaendert(Item item, String alt) {
        System.out.printf("[%s] %-20s %10s -> %s%n", UHR.format(LocalTime.now()),
                item.label(), alt, item.state());
    }
}
