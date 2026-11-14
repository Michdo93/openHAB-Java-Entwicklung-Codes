package werkstatt;

import java.util.Random;

/** Ersetzt einen echten Sensor: warm wird es von selbst, kühler nur mit Lüfter. */
public class SimulierterSensor implements Sensor {

    private final Random zufall;
    private double temperatur = 21.0;
    private double feuchte = 55.0;

    public SimulierterSensor() {
        this(new Random());
    }

    public SimulierterSensor(Random zufall) {          // fester Startwert für Tests
        this.zufall = zufall;
    }

    @Override
    public Messung messen(boolean luefterAn) {
        temperatur += luefterAn ? zufall.nextDouble(-0.6, -0.2)
                                : zufall.nextDouble(0.1, 0.9);
        feuchte = Math.clamp(feuchte + zufall.nextDouble(-2, 2), 20, 95);
        return new Messung(Math.round(temperatur * 10) / 10.0, Math.round(feuchte));
    }
}
