package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.PitStopPhase;
import com.formula1.domain.model.PitStopRecord;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

/** Traduce snapshots de boxes a los controles del Dashboard. */
final class PitStopPresenter {

    private static final int MAX_FEED_ITEMS = 14;

    private final Label phaseLabel;
    private final Label timeLabel;
    private final Label eventLabel;
    private final Label messageLabel;
    private final VBox feed;

    PitStopPresenter(Label phaseLabel, Label timeLabel, Label eventLabel,
                     Label messageLabel, VBox feed) {
        this.phaseLabel = phaseLabel;
        this.timeLabel = timeLabel;
        this.eventLabel = eventLabel;
        this.messageLabel = messageLabel;
        this.feed = feed;
    }

    void reset() {
        phaseLabel.setText("SIN PARADA");
        timeLabel.setText("—");
    }

    void present(PitStopRecord stop) {
        phaseLabel.setText(stop.fase().getEtiqueta().toUpperCase());
        timeLabel.setText(stop.fase() == PitStopPhase.ENTERING
                ? String.format(Locale.ROOT, "Pérdida parcial +%.3f s",
                        stop.tiempoPerdidoSegundos())
                : String.format(Locale.ROOT, "Detenido %.3f s · pérdida +%.3f s",
                        stop.tiempoDetenidoSegundos(), stop.tiempoPerdidoSegundos()));
        eventLabel.setText("PIT STOP");
        messageLabel.setText(radioMessage(stop));
        addToFeed(stop);
    }

    void showLatest(List<PitStopRecord> stops) {
        if (stops.isEmpty()) {
            reset();
            return;
        }
        PitStopRecord latest = stops.get(stops.size() - 1);
        phaseLabel.setText(latest.fase().getEtiqueta().toUpperCase());
        timeLabel.setText(String.format(Locale.ROOT,
                "%s · +%.3f s · P%d → P%d", latest.piloto(),
                latest.tiempoPerdidoSegundos(), latest.posicionEntrada(),
                latest.posicionActual()));
    }

    void addToFeed(PitStopRecord stop) {
        Region dot = new Region();
        dot.getStyleClass().addAll("feed-dot", "pit");
        Label text = new Label(stop.fase().getEtiqueta());
        text.getStyleClass().add("feed-text");
        Label driver = new Label(stop.piloto() + " · " + stop.motivo().getEtiqueta());
        driver.getStyleClass().add("feed-driver");
        Label time = new Label(String.format(Locale.ROOT,
                "+%.3f s", stop.tiempoPerdidoSegundos()));
        time.getStyleClass().add("feed-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox body = new VBox(text, driver);
        HBox row = new HBox(8, dot, body, spacer, time);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("feed-row");
        feed.getChildren().add(0, row);
        while (feed.getChildren().size() > MAX_FEED_ITEMS) {
            feed.getChildren().remove(feed.getChildren().size() - 1);
        }
    }

    private String radioMessage(PitStopRecord stop) {
        return switch (stop.fase()) {
            case ENTERING -> "BOX, BOX · " + stop.piloto() + " entra por "
                    + stop.motivo().getEtiqueta().toLowerCase(Locale.ROOT);
            case STOPPED -> stop.piloto() + " detenido · cronómetro de boxes activo";
            case EXITING -> stop.piloto() + " sale de boxes · reincorporación P"
                    + stop.posicionActual();
            case COMPLETED -> "Parada completada · " + stop.piloto() + " P"
                    + stop.posicionEntrada() + " → P" + stop.posicionActual();
        };
    }
}
