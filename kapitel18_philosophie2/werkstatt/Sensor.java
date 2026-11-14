package werkstatt;

/** Was ein Sensor können muss: Temperatur in °C und Feuchte in % messen. */
public interface Sensor {

    record Messung(double temperatur, long feuchte) {}

    Messung messen(boolean luefterAn);
}
