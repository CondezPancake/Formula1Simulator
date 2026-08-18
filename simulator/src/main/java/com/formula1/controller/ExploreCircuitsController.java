package com.formula1.controller;

import com.formula1.model.Circuit;
import com.formula1.model.WeatherCondition;
import com.formula1.service.CircuitService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Catálogo de circuitos en tarjetas, de solo lectura. */
public class ExploreCircuitsController {

    private static final String ACENTO = "#e10600";

    @FXML private FlowPane tarjetas;
    @FXML private Label lblConteo;

    private final CircuitService circuitos;

    public ExploreCircuitsController() {
        this(new CircuitService());
    }

    public ExploreCircuitsController(CircuitService circuitos) {
        this.circuitos = circuitos;
    }

    @FXML
    public void initialize() {
        List<Circuit> lista = circuitos.listar();
        tarjetas.getChildren().setAll(lista.stream().map(this::tarjeta).toList());
        lblConteo.setText(lista.size() + " TRAZADOS");
    }

    private VBox tarjeta(Circuit circuito) {
        VBox card = new VBox(0);
        card.getStyleClass().add("explore-card");
        card.setStyle("-fx-border-color: " + ACENTO + ";");

        StackPane placeholder = new StackPane();
        placeholder.getStyleClass().add("explore-card-photo");
        placeholder.setPrefSize(208, 100);
        placeholder.setMinSize(208, 100);
        Label emoji = new Label(climaDominanteIcono(circuito));
        emoji.setStyle("-fx-font-size: 30px;");
        placeholder.getChildren().add(emoji);

        Label nombre = new Label(circuito.getNombre());
        nombre.getStyleClass().add("explore-card-name");
        nombre.setWrapText(true);

        Label ubicacion = new Label(circuito.getPais() + "  ·  " + circuito.getLongitudKm() + " km  ·  "
                + circuito.getVueltas() + " vueltas");
        ubicacion.getStyleClass().add("hint");

        HBox stats = new HBox(18);
        String record = circuito.getRecordVuelta() == null ? "—" : circuito.getRecordVuelta().getTiempo();
        String titular = circuito.getRecordVuelta() == null || circuito.getRecordVuelta().getPiloto() == null
                ? "—" : circuito.getRecordVuelta().getPiloto();
        stats.getChildren().addAll(
                statCell(record, "RÉC. VUELTA"),
                statCell(titular, "TITULAR"),
                statCell(climaDominanteEtiqueta(circuito), "CLIMA MED."));

        Button verDetalles = new Button("▼ VER DETALLES");
        verDetalles.getStyleClass().add("icon-button");
        verDetalles.setMaxWidth(Double.MAX_VALUE);
        verDetalles.setOnAction(e -> {
            Navigator.ir("circuit-detail");
            if (Navigator.ultimoControlador() instanceof CircuitDetailController detalle) {
                detalle.mostrar(circuito.getNombre());
            }
        });

        VBox acciones = new VBox(verDetalles);
        acciones.getStyleClass().add("explore-card-actions");
        VBox contenido = new VBox(8, nombre, ubicacion, stats, acciones);
        contenido.getStyleClass().add("explore-card-body");
        card.getChildren().addAll(placeholder, contenido);
        return card;
    }

    private WeatherCondition climaDominante(Circuit circuito) {
        Map<WeatherCondition, Double> prob = circuito.getProbabilidadClima();
        if (prob == null || prob.isEmpty()) {
            return WeatherCondition.SECO;
        }
        return prob.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(WeatherCondition.SECO);
    }

    private String climaDominanteEtiqueta(Circuit circuito) {
        return climaDominante(circuito).getEtiqueta();
    }

    /** Glifos con cobertura en las fuentes del sistema (los emojis no la tienen). */
    private String climaDominanteIcono(Circuit circuito) {
        return switch (climaDominante(circuito)) {
            case SECO -> "☀";
            case LLUVIOSO -> "▒";
            case EXTREMO -> "▓";
        };
    }

    private VBox statCell(String valor, String etiqueta) {
        VBox celda = new VBox(2);
        celda.setAlignment(Pos.CENTER_LEFT);
        Label valorLbl = new Label(valor);
        valorLbl.getStyleClass().add("stat-value");
        Label etiquetaLbl = new Label(etiqueta);
        etiquetaLbl.getStyleClass().add("card-label");
        celda.getChildren().addAll(valorLbl, etiquetaLbl);
        return celda;
    }
}
