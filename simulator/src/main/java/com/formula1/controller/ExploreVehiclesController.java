package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.service.VehicleService;
import com.formula1.util.TeamColors;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Catálogo de vehículos en tarjetas, de solo lectura. */
public class ExploreVehiclesController {

    @FXML private FlowPane tarjetas;
    @FXML private Label lblConteo;

    private final VehicleService vehiculos;

    public ExploreVehiclesController() {
        this(new VehicleService());
    }

    public ExploreVehiclesController(VehicleService vehiculos) {
        this.vehiculos = vehiculos;
    }

    @FXML
    public void initialize() {
        List<Vehicle> lista = vehiculos.listar();
        tarjetas.getChildren().setAll(lista.stream().map(this::tarjeta).toList());
        lblConteo.setText(lista.size() + " VEHÍCULOS");
    }

    private VBox tarjeta(Vehicle vehiculo) {
        String color = TeamColors.hex(vehiculo.getEquipo());

        VBox card = new VBox(8);
        card.getStyleClass().add("explore-card");
        card.setStyle("-fx-border-color: " + color + ";");

        StackPane placeholder = new StackPane();
        placeholder.getStyleClass().add("explore-card-photo");
        placeholder.setPrefSize(208, 100);
        placeholder.setMinSize(208, 100);
        Label modeloGrande = new Label(vehiculo.getModelo());
        modeloGrande.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        placeholder.getChildren().add(modeloGrande);

        Label equipo = new Label(vehiculo.getEquipo() == null ? "" : vehiculo.getEquipo().toUpperCase(Locale.ROOT));
        equipo.getStyleClass().add("explore-card-team");
        equipo.setStyle("-fx-text-fill: " + color + ";");

        Vehicle.Performance normal = vehiculo.rendimientoDe(DrivingMode.NORMAL);
        double consumo = normal == null ? 0 : normal.consumoCon(WeatherCondition.SECO);
        double desgaste = normal == null ? 0 : normal.desgasteCon(WeatherCondition.SECO);

        FlowPane stats = new FlowPane(18, 10);
        stats.getChildren().addAll(
                statCell(vehiculo.getVelocidadMaximaKmh() + " km/h", "VEL. MÁX."),
                statCell(String.format(Locale.ROOT, "%.1f s", vehiculo.getAceleracion0100()), "ACEL. 0-100"),
                statCell(String.format(Locale.ROOT, "%.1f kg/v", consumo), "CONSUMO"),
                statCell(String.format(Locale.ROOT, "%.0f%%", desgaste), "DESGASTE"));

        String pilotoAsignado = vehiculos.pilotosDe(vehiculo).stream()
                .map(Driver::getNombre)
                .collect(Collectors.joining(", "));
        Label piloto = new Label((pilotoAsignado.isBlank() ? "Sin asignar" : pilotoAsignado));
        piloto.getStyleClass().add("hint");

        Button usar = new Button("USAR ESTE VEHÍCULO");
        usar.getStyleClass().add("icon-button");
        usar.setMaxWidth(Double.MAX_VALUE);
        usar.setOnAction(e -> {
            Navigator.ir("simulation");
            if (Navigator.ultimoControlador() instanceof SimulationController simulacion) {
                simulacion.precargarVehiculo(vehiculo.getModelo());
            }
        });

        Label nombre = new Label(vehiculo.getModelo());
        nombre.getStyleClass().add("explore-card-name");

        card.getChildren().addAll(placeholder, nombre, equipo, stats, piloto, usar);
        return card;
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
