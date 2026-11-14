// Kapitel 9: Threads, ein Thread-Pool und virtuelle Threads
// Ein "Sensor" braucht 300 ms für eine Messung (simuliert mit sleep).
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

double messen(String sensor) throws InterruptedException {
    Thread.sleep(300);
    return 18 + sensor.length() % 5;
}

long ms(long start) {
    return Math.round((System.nanoTime() - start) / 1e8) * 100;   // auf 100 ms gerundet
}

void main() throws Exception {
    List<String> sensoren = List.of("Wohnzimmer", "Kueche", "Werkstatt", "Flur");

    // 1. nacheinander: 4 × 300 ms
    long start = System.nanoTime();
    for (String s : sensoren) {
        messen(s);
    }
    IO.println("nacheinander:     " + ms(start) + " ms");

    // 2. ein eigener Thread je Sensor
    start = System.nanoTime();
    List<Thread> threads = new ArrayList<>();
    for (String s : sensoren) {
        Thread t = new Thread(() -> {
            try {
                messen(s);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();
        threads.add(t);
    }
    for (Thread t : threads) {
        t.join();                                   // warten, bis alle fertig sind
    }
    IO.println("eigene Threads:   " + ms(start) + " ms");

    // 3. ein Pool mit 2 Threads: je zwei Messungen gleichzeitig
    start = System.nanoTime();
    try (var pool = Executors.newFixedThreadPool(2)) {
        List<Future<Double>> ergebnisse = new ArrayList<>();
        for (String s : sensoren) {
            ergebnisse.add(pool.submit(() -> messen(s)));
        }
        double summe = 0;
        for (Future<Double> f : ergebnisse) {
            summe += f.get();                       // blockiert bis zum Ergebnis
        }
        IO.println("Pool mit 2:       " + ms(start) + " ms, Summe " + summe);
    }

    // 4. virtuelle Threads: leichtgewichtig, ideal für viele wartende Anfragen
    start = System.nanoTime();
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < 1000; i++) {
            pool.submit(() -> messen("Sensor"));
        }
    }                                               // close() wartet auf alle Aufgaben
    IO.println("1000 virtuelle:   " + ms(start) + " ms");
}
