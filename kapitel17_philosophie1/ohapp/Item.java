package ohapp;

/** Ein Item, wie das Model es kennt – unveränderlich:
 *  ein neuer Zustand ist ein neues Item. */
public record Item(String name, String typ, String label, String state) {

    Item mitZustand(String neu) {
        return new Item(name, typ, label, neu);
    }
}
