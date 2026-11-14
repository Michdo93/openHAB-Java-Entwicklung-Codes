package ohapp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/** View 2: grafische Oberfläche mit JavaFX. Jedes Item hat eine StringProperty;
 *  die Labels sind an sie gebunden und zeichnen sich bei jeder Änderung selbst neu. */
public class FxView implements ItemBeobachter {

    private final ItemModel model;
    private final Controller controller;
    private final Map<String, StringProperty> werte = new HashMap<>();
    private final CountDownLatch geschlossen = new CountDownLatch(1);

    public FxView(ItemModel model, Controller controller) {
        this.model = model;
        this.controller = controller;
    }

    @Override
    public void itemGeaendert(Item item, String alt) {
        // Wird im Thread des Ereignisstroms aufgerufen! JavaFX-Knoten und ihre
        // Properties gehören dem FX-Application-Thread – deshalb die Änderung
        // dorthin weiterreichen.
        Platform.runLater(() -> {
            StringProperty wert = werte.get(item.name());
            if (wert != null) {
                wert.set(item.state());
            }
        });
    }

    private Stage aufbauen() {
        var raster = new GridPane();
        raster.setPadding(new Insets(12));
        raster.setHgap(12);
        raster.setVgap(6);
        int zeile = 0;
        for (Item item : model.alle()) {
            var wert = new SimpleStringProperty(item.state());
            werte.put(item.name(), wert);
            var anzeige = new Label();
            anzeige.textProperty().bind(wert);                 // Property als Observer
            raster.addRow(zeile, new Label(item.label()), anzeige);
            switch (item.typ()) {
                case "Switch" -> {
                    var knopf = new Button("umschalten");
                    knopf.setOnAction(e -> controller.umschalten(item.name()));
                    raster.add(knopf, 2, zeile);
                }
                case "Dimmer", "Rollershutter" -> {
                    var regler = new Slider(0, 100, zahl(item.state()));
                    regler.setPrefWidth(140);
                    regler.setOnMouseReleased(e -> controller.senden(item.name(),
                            String.valueOf(Math.round(regler.getValue()))));
                    raster.add(regler, 2, zeile);
                }
                default -> { }
            }
            zeile++;
        }
        var nacht = new Button("Gute Nacht");
        nacht.setOnAction(e -> controller.szene("restbuch-1"));
        raster.add(nacht, 2, zeile);

        var fenster = new Stage();
        fenster.setTitle("openHAB – Wohnzimmer");
        fenster.setScene(new Scene(raster));
        fenster.setOnHidden(e -> geschlossen.countDown());
        model.anmelden(this);  // erst anmelden, wenn die Properties stehen
        return fenster;
    }

    private static double zahl(String state) {
        try {
            return Double.parseDouble(state);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Startet JavaFX, zeigt das Fenster und wartet, bis es geschlossen wird. */
    public void starten() throws InterruptedException {
        Platform.startup(() -> aufbauen().show());
        geschlossen.await();
        Platform.exit();
    }
}
