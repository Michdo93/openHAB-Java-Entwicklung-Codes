// Kapitel 9: CompletableFuture – nacheinander, gleichzeitig, mit Zeitlimit
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Ohne eigenen Executor liefe alles im gemeinsamen ForkJoinPool – der hat auf
// kleinen Rechnern nur einen Thread. Virtuelle Threads: einer je Aufgabe.
final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

// Ein simulierter Sensor, der nach "dauer" Millisekunden antwortet
CompletableFuture<Double> sensor(String name, long dauer, double wert) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(dauer);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (wert < 0) {
            throw new IllegalStateException(name + " antwortet nicht");
        }
        return wert;
    }, pool);
}

long ms(long start) {
    return Math.round((System.nanoTime() - start) / 1e8) * 100;
}

void main() {
    long start = System.nanoTime();
    double a = sensor("Wohnzimmer", 300, 21.5).join();      // join wartet
    double b = sensor("Werkstatt", 300, 18.0).join();
    IO.println("nacheinander: " + a + " / " + b + " nach " + ms(start) + " ms");

    start = System.nanoTime();
    var w = sensor("Wohnzimmer", 300, 21.5);
    var k = sensor("Werkstatt", 300, 18.0);
    CompletableFuture.allOf(w, k).join();                    // beide gleichzeitig
    IO.println("gleichzeitig: " + w.join() + " / " + k.join()
            + " nach " + ms(start) + " ms");

    // weiterverarbeiten, sobald das Ergebnis da ist
    String text = sensor("Flur", 100, 19.25)
            .thenApply(t -> "%.1f °C".formatted(t))
            .join();
    IO.println("umgewandelt:  " + text);

    // Zeitlimit und Fehler
    var langsam = sensor("Keller", 2000, 12.0).orTimeout(500, TimeUnit.MILLISECONDS);
    try {
        langsam.join();
    } catch (CompletionException e) {
        IO.println("Zeitlimit:    " + (e.getCause() instanceof TimeoutException));
    }
    double ersatz = sensor("Dachboden", 100, -1).exceptionally(fehler -> {
        IO.println("Fehler:       " + fehler.getCause().getMessage());
        return Double.NaN;                                   // Ersatzwert
    }).join();
    IO.println("Ersatzwert:   " + ersatz);
}
