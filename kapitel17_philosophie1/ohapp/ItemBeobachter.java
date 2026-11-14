package ohapp;

/** Die Schnittstelle, die jede View erfüllen muss (Observer). */
@FunctionalInterface
public interface ItemBeobachter {

    void itemGeaendert(Item item, String alt);
}
