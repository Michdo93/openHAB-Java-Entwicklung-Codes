package ohapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Model: hält die Item-Zustände und benachrichtigt Beobachter (Subject im
 *  Observer-Pattern). Es kennt weder openHAB noch Bildschirme. */
public class ItemModel {

    private final Set<String> filter;  // nur diese Items, oder alle
    private final Map<String, Item> items = new TreeMap<>();
    private final List<ItemBeobachter> beobachter = new CopyOnWriteArrayList<>();

    public ItemModel(Collection<String> namen) {
        this.filter = namen == null ? null : Set.copyOf(namen);
    }

    // --- Observer-Verwaltung ----------------------------------------------------
    public void anmelden(ItemBeobachter b) {
        beobachter.add(b);
    }

    private void benachrichtigen(Item item, String alt) {
        for (ItemBeobachter b : beobachter) {
            b.itemGeaendert(item, alt);
        }
    }

    // --- Daten --------------------------------------------------------------------
    /** Momentaufnahme übernehmen (Liste von Item-Maps aus getItems). */
    public void laden(List<Map<String, Object>> daten) {
        for (Map<String, Object> d : daten) {
            setzeZustand((String) d.get("name"), String.valueOf(d.get("state")),
                    (String) d.get("type"), (String) d.get("label"));
        }
    }

    public void setzeZustand(String name, String state) {
        setzeZustand(name, state, "", "");
    }

    /** Einziger Weg, auf dem sich ein Zustand ändert. */
    public void setzeZustand(String name, String state, String typ, String label) {
        if (filter != null && !filter.contains(name)) {
            return;
        }
        Item neu;
        String alt;
        synchronized (items) {                // Strom-Thread schreibt, Views lesen
            Item item = items.get(name);
            if (item == null) {
                String text = label == null || label.isEmpty() ? name : label;
                item = new Item(name, typ, text, "NULL");
            }
            alt = item.state();
            neu = item.mitZustand(state);
            items.put(name, neu);
        }
        if (!alt.equals(state)) {
            benachrichtigen(neu, alt);        // außerhalb der Sperre benachrichtigen
        }
    }

    public boolean kennt(String name) {
        synchronized (items) {
            return items.containsKey(name);
        }
    }

    public Item get(String name) {
        synchronized (items) {
            return items.get(name);
        }
    }

    /** Alle Items, nach Name sortiert – eine Kopie, die Views gefahrlos durchlaufen. */
    public List<Item> alle() {
        synchronized (items) {
            return new ArrayList<>(items.values());
        }
    }
}
