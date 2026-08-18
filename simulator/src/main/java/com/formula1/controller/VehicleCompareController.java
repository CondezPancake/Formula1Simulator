package com.formula1.controller;

import com.formula1.model.DrivingMode;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.service.VehicleService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;

/**
 * Compara dos o más vehículos en velocidad, consumo y desgaste.
 *
 * La tabla va transpuesta —una fila por métrica y una columna por
 * vehículo— porque así se leen las diferencias de un vistazo, que es el
 * objetivo de la comparación.
 */
public class VehicleCompareController {

    @FXML private TableView<Fila> tabla;
    @FXML private TableColumn<Fila, String> colMetrica;
    @FXML private BarChart<String, Number> grafico;
    @FXML private ComboBox<DrivingMode> modo;
    @FXML private ComboBox<WeatherCondition> clima;
    @FXML private Label lblVacio;

    private final VehicleService vehiculos;
    private List<Vehicle> seleccion = new ArrayList<>();

    public VehicleCompareController() {
        this(new VehicleService());
    }

    public VehicleCompareController(VehicleService vehiculos) {
        this.vehiculos = vehiculos;
    }

    @FXML
    public void initialize() {
        colMetrica.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().metrica));
        modo.getItems().addAll(DrivingMode.values());
        modo.setValue(DrivingMode.NORMAL);
        clima.getItems().addAll(WeatherCondition.values());
        clima.setValue(WeatherCondition.SECO);
        modo.valueProperty().addListener((o, a, b) -> pintar());
        clima.valueProperty().addListener((o, a, b) -> pintar());
    }

    /** Recibe los modelos elegidos en la lista de vehículos. */
    public void comparar(List<String> modelos) {
        seleccion = modelos.stream()
                .map(m -> vehiculos.porModelo(m).orElse(null))
                .filter(v -> v != null)
                .toList();
        pintar();
    }

    private void pintar() {
        tabla.getColumns().removeIf(c -> c != colMetrica);
        tabla.getItems().clear();
        grafico.getData().clear();

        if (seleccion.size() < 2) {
            lblVacio.setVisible(true);
            return;
        }
        lblVacio.setVisible(false);

        for (int i = 0; i < seleccion.size(); i++) {
            final int indice = i;
            TableColumn<Fila, String> columna = new TableColumn<>(seleccion.get(i).getModelo());
            columna.setPrefWidth(120);
            columna.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().valores.get(indice)));
            tabla.getColumns().add(columna);
        }

        DrivingMode m = modo.getValue();
        WeatherCondition c = clima.getValue();

        tabla.setItems(FXCollections.observableArrayList(
                fila("Equipo", v -> v.getEquipo()),
                fila("Motor", v -> v.getMotor()),
                fila("Velocidad máxima (km/h)", v -> String.valueOf(v.getVelocidadMaximaKmh())),
                fila("Aceleración 0-100 (s)", v -> String.valueOf(v.getAceleracion0100())),
                fila("Velocidad media (km/h)", v -> String.valueOf(v.rendimientoDe(m).getVelocidadPromedioKmh())),
                fila("Consumo por vuelta", v -> String.format("%.2f", v.rendimientoDe(m).consumoCon(c))),
                fila("Desgaste por vuelta", v -> String.format("%.2f", v.rendimientoDe(m).desgasteCon(c)))
        ));

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Velocidad media — " + m.getEtiqueta());
        seleccion.forEach(v -> serie.getData().add(
                new XYChart.Data<>(v.getModelo(), v.rendimientoDe(m).getVelocidadPromedioKmh())));
        grafico.getData().add(serie);
    }

    @FXML
    private void onVolver() {
        Navigator.ir("gestion");
        if (Navigator.ultimoControlador() instanceof ManagementController gestion) {
            gestion.mostrarVehiculos();
        }
    }

    private Fila fila(String metrica, java.util.function.Function<Vehicle, String> extractor) {
        Fila fila = new Fila(metrica);
        seleccion.forEach(v -> fila.valores.add(extractor.apply(v)));
        return fila;
    }

    /** Una métrica y su valor en cada vehículo comparado. */
    public static class Fila {
        private final String metrica;
        private final List<String> valores = new ArrayList<>();

        Fila(String metrica) {
            this.metrica = metrica;
        }

        public String getMetrica() {
            return metrica;
        }
    }
}
