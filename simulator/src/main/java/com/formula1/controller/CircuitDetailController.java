package com.formula1.controller;

import com.formula1.model.Circuit;
import com.formula1.model.DrivingMode;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.service.CircuitService;
import com.formula1.service.VehicleService;
import com.formula1.util.FormatUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ficha de un circuito: datos, récord, ganadores históricos, clima típico
 * e impacto de la pista sobre consumo y desgaste.
 */
public class CircuitDetailController {

    @FXML private Label lblNombre;
    @FXML private Label lblPais;
    @FXML private Label lblLongitud;
    @FXML private Label lblVueltas;
    @FXML private Label lblDescripcion;
    @FXML private Label lblRecord;
    @FXML private Label lblFactor;
    @FXML private Label lblClima;
    @FXML private ListView<String> listaGanadores;
    @FXML private ComboBox<String> selectorVehiculo;
    @FXML private ComboBox<WeatherCondition> selectorClima;
    @FXML private TableView<FilaImpacto> tablaImpacto;
    @FXML private TableColumn<FilaImpacto, String> colModo;
    @FXML private TableColumn<FilaImpacto, String> colConsumo;
    @FXML private TableColumn<FilaImpacto, String> colDesgaste;
    @FXML private TableColumn<FilaImpacto, String> colConsumoCarrera;

    private final CircuitService circuitos;
    private final VehicleService vehiculos;
    private Circuit circuito;

    public CircuitDetailController() {
        this(new CircuitService(), new VehicleService());
    }

    public CircuitDetailController(CircuitService circuitos, VehicleService vehiculos) {
        this.circuitos = circuitos;
        this.vehiculos = vehiculos;
    }

    @FXML
    public void initialize() {
        colModo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().modo));
        colConsumo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().consumo));
        colDesgaste.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().desgaste));
        colConsumoCarrera.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().consumoCarrera));

        vehiculos.listar().forEach(v -> selectorVehiculo.getItems().add(v.getModelo()));
        if (!selectorVehiculo.getItems().isEmpty()) {
            selectorVehiculo.setValue(selectorVehiculo.getItems().get(0));
        }
        selectorClima.getItems().addAll(WeatherCondition.values());
        selectorClima.setValue(WeatherCondition.SECO);

        selectorVehiculo.valueProperty().addListener((o, a, b) -> pintarImpacto());
        selectorClima.valueProperty().addListener((o, a, b) -> pintarImpacto());
    }

    /** Carga el circuito indicado en la ficha. */
    public void mostrar(String nombre) {
        circuitos.porNombre(nombre).ifPresent(c -> {
            circuito = c;
            lblNombre.setText(c.getNombre());
            lblPais.setText(c.getPais());
            lblLongitud.setText(c.getLongitudKm() + " km");
            lblVueltas.setText(String.valueOf(c.getVueltas()));
            lblDescripcion.setText(c.getDescripcion());
            lblRecord.setText(c.getRecordVuelta() == null ? "Sin récord registrado"
                    : FormatUtils.formatLapTime(c.getRecordVuelta().getTiempoSegundos())
                    + "  ·  " + c.getRecordVuelta().getPiloto()
                    + (c.getRecordVuelta().getAnio() > 0 ? "  (" + c.getRecordVuelta().getAnio() + ")" : ""));
            lblFactor.setText(String.format("%.3f", c.getFactorTecnico()));

            StringBuilder clima = new StringBuilder();
            for (WeatherCondition condicion : WeatherCondition.values()) {
                if (clima.length() > 0) {
                    clima.append("   ·   ");
                }
                clima.append(condicion.getEtiqueta()).append(' ')
                        .append(FormatUtils.formatPercentage(c.probabilidadDe(condicion)));
            }
            lblClima.setText(clima.toString());

            listaGanadores.setItems(FXCollections.observableArrayList(circuitos.ganadoresDe(c)));
            pintarImpacto();
        });
    }

    private void pintarImpacto() {
        if (circuito == null || selectorVehiculo.getValue() == null) {
            return;
        }
        Vehicle vehiculo = vehiculos.porModelo(selectorVehiculo.getValue()).orElse(null);
        if (vehiculo == null) {
            return;
        }
        Map<DrivingMode, double[]> impacto = circuitos.impactoSobre(circuito, vehiculo, selectorClima.getValue());

        List<FilaImpacto> filas = new ArrayList<>();
        impacto.forEach((modo, valores) -> filas.add(new FilaImpacto(
                modo.getEtiqueta(),
                String.format("%.2f / vuelta", valores[0]),
                String.format("%.2f / vuelta", valores[1]),
                String.format("%.1f en %d vueltas", valores[0] * circuito.getVueltas(), circuito.getVueltas()))));
        tablaImpacto.setItems(FXCollections.observableArrayList(filas));
    }

    @FXML
    private void onVolver() {
        Navigator.ir("circuits");
    }

    /** Consumo y desgaste estimados en este circuito para un modo. */
    public static class FilaImpacto {
        private final String modo;
        private final String consumo;
        private final String desgaste;
        private final String consumoCarrera;

        FilaImpacto(String modo, String consumo, String desgaste, String consumoCarrera) {
            this.modo = modo;
            this.consumo = consumo;
            this.desgaste = desgaste;
            this.consumoCarrera = consumoCarrera;
        }
    }
}
