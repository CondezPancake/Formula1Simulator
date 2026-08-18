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
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
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

        var imagen = ExploreCardVisuals.circuito(circuito);

        Label nombre = new Label(circuito.getNombre());
        nombre.getStyleClass().add("explore-card-name");
        nombre.setWrapText(true);

        Label ubicacion = new Label(circuito.getPais() + "  ·  " + circuito.getLongitudKm() + " km  ·  "
                + circuito.getVueltas() + " vueltas");
        ubicacion.getStyleClass().add("hint");

        String record = circuito.getRecordVuelta() == null ? "—" : circuito.getRecordVuelta().getTiempo();
        String titular = circuito.getRecordVuelta() == null || circuito.getRecordVuelta().getPiloto() == null
                ? "—" : circuito.getRecordVuelta().getPiloto();
        // En columna y no en fila: en el ancho de la tarjeta, tres celdas de
        // texto dejaban valores como «Rubens Bar...» cortados a la mitad.
        VBox stats = new VBox(4,
                statFila("RÉC. VUELTA", record),
                statFila("TITULAR", titular),
                statFila("CLIMA MED.", climaDominanteEtiqueta(circuito)));

        Button verDetalles = new Button("VER DETALLE  →");
        verDetalles.getStyleClass().add("explore-card-cta");
        verDetalles.setMaxWidth(Double.MAX_VALUE);
        verDetalles.setOnAction(e -> {
            Navigator.irConRetorno("circuit-detail");
            if (Navigator.ultimoControlador() instanceof CircuitDetailController detalle) {
                detalle.mostrar(circuito.getNombre());
            }
        });

        VBox acciones = new VBox(verDetalles);
        acciones.getStyleClass().add("explore-card-actions");
        VBox contenido = new VBox(8, nombre, ubicacion, stats, acciones);
        contenido.getStyleClass().add("explore-card-body");
        card.getChildren().addAll(imagen, contenido);
        return card;
    }

    /** Etiqueta a la izquierda y valor a la derecha, en una sola linea. */
    private HBox statFila(String etiqueta, String valor) {
        Label etiquetaLbl = new Label(etiqueta);
        etiquetaLbl.getStyleClass().add("card-label");
        Region relleno = new Region();
        HBox.setHgrow(relleno, Priority.ALWAYS);
        Label valorLbl = new Label(valor);
        valorLbl.getStyleClass().add("stat-value");
        HBox fila = new HBox(8, etiquetaLbl, relleno, valorLbl);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
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

}
