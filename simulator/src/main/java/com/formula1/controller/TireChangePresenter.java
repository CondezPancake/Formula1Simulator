package com.formula1.controller;

import com.formula1.domain.model.TireChangeRecord;
import com.formula1.domain.model.TireCompound;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Representa el compuesto actual y cada cambio sin decidir la estrategia. */
final class TireChangePresenter {

    private static final int MAX_FEED_ITEMS = 14;

    private final Label compoundLabel;
    private final Label eventLabel;
    private final Label messageLabel;
    private final VBox feed;

    TireChangePresenter(Label compoundLabel, Label eventLabel,
                        Label messageLabel, VBox feed) {
        this.compoundLabel = compoundLabel;
        this.eventLabel = eventLabel;
        this.messageLabel = messageLabel;
        this.feed = feed;
    }

    void reset(TireCompound initialCompound) {
        showCompound(initialCompound == null ? TireCompound.MEDIUM : initialCompound);
    }

    void present(TireChangeRecord change, Integer configuredDriverId) {
        if (configuredDriverId != null && change.pilotoId() == configuredDriverId) {
            showCompound(change.nuevo());
        }
        eventLabel.setText("CAMBIO DE NEUMÁTICOS");
        messageLabel.setText(change.piloto() + " · "
                + change.anterior().getCodigo() + " → " + change.nuevo().getCodigo()
                + " · " + change.motivo().getEtiqueta());
        addToFeed(change);
    }

    void showSession(List<TireChangeRecord> changes, Integer configuredDriverId,
                     TireCompound initialCompound) {
        TireCompound startingCompound = initialCompound == null
                ? TireCompound.MEDIUM : initialCompound;
        TireCompound compound = configuredDriverId == null
                ? startingCompound
                : changes.stream()
                        .filter(change -> change.pilotoId() == configuredDriverId)
                        .reduce((first, second) -> second)
                        .map(TireChangeRecord::nuevo)
                        .orElse(startingCompound);
        showCompound(compound);
    }

    void addToFeed(TireChangeRecord change) {
        Region dot = new Region();
        dot.getStyleClass().addAll("feed-dot", "tire");
        Label text = new Label("Cambio " + change.anterior().getCodigo()
                + " → " + change.nuevo().getCodigo());
        text.getStyleClass().add("feed-text");
        Label driver = new Label(change.piloto());
        driver.getStyleClass().add("feed-driver");
        Label segment = new Label("SEG " + change.segmento());
        segment.getStyleClass().add("feed-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, dot, new VBox(text, driver), spacer, segment);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("feed-row");
        feed.getChildren().add(0, row);
        while (feed.getChildren().size() > MAX_FEED_ITEMS) {
            feed.getChildren().remove(feed.getChildren().size() - 1);
        }
    }

    private void showCompound(TireCompound compound) {
        compoundLabel.setText(compound.getCodigo());
        compoundLabel.getStyleClass().removeAll(
                "compound-soft", "compound-medium", "compound-hard");
        compoundLabel.getStyleClass().add(switch (compound) {
            case SOFT -> "compound-soft";
            case MEDIUM -> "compound-medium";
            case HARD -> "compound-hard";
        });
    }
}
