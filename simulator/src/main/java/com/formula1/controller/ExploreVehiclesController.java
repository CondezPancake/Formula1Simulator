package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.service.VehicleService;
import com.formula1.util.TeamColors;
import com.formula1.util.VehicleImages;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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

    /**
     * La foto abre el visor con las demás vistas del monoplaza. Solo se hace
     * pulsable cuando hay algo más que enseñar, para no ofrecer un clic que
     * no lleva a ninguna parte.
     */
    private void prepararGaleria(StackPane imagen, Vehicle vehiculo) {
        if (!VehicleImages.tieneGaleria(vehiculo.getModelo())) {
            return;
        }
        imagen.getStyleClass().add("explore-card-photo-clickable");
        Tooltip.install(imagen, new Tooltip("Ver más vistas del " + vehiculo.getModelo()));
        imagen.setOnMouseClicked(e ->
                VehicleGallery.abrir(vehiculo.getModelo(), vehiculo.getEquipo()));
    }

    private VBox tarjeta(Vehicle vehiculo) {
        String color = TeamColors.hex(vehiculo.getEquipo());

        VBox card = new VBox(0);
        card.getStyleClass().add("explore-card");
        card.setStyle("-fx-border-color: " + color + ";");

        var imagen = ExploreCardVisuals.vehiculo(vehiculo, color);
        prepararGaleria(imagen, vehiculo);

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

        Button usar = new Button("USAR ESTE VEHÍCULO  →");
        usar.getStyleClass().add("explore-card-cta");
        usar.setMaxWidth(Double.MAX_VALUE);
        usar.setOnAction(e -> {
            ShellController.irACarrera();
            if (Navigator.ultimoControlador() instanceof SimulationController simulacion) {
                simulacion.precargarVehiculo(vehiculo.getModelo());
            }
        });

        Label nombre = new Label(vehiculo.getModelo());
        nombre.getStyleClass().add("explore-card-name");

        VBox acciones = new VBox(usar);
        acciones.getStyleClass().add("explore-card-actions");
        VBox contenido = new VBox(8, nombre, equipo, stats, piloto, acciones);
        contenido.getStyleClass().add("explore-card-body");
        card.getChildren().addAll(imagen, contenido);
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
