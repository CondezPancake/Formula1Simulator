package com.formula1.controller;

import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.service.QualifyingService;
import com.formula1.util.FormatUtils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Historial de sesiones: parrillas guardadas, comparación de tiempos entre
 * sesiones del mismo circuito y reutilización de configuraciones previas.
 */
public class HistoryController {

    @FXML private TableView<QualifyingSession> tablaSesiones;
    @FXML private TableColumn<QualifyingSession, String> colFecha;
    @FXML private TableColumn<QualifyingSession, String> colCircuito;
    @FXML private TableColumn<QualifyingSession, String> colClima;
    @FXML private TableColumn<QualifyingSession, String> colPole;
    @FXML private TableColumn<QualifyingSession, String> colTiempoPole;
    @FXML private TableColumn<QualifyingSession, String> colConfig;

    @FXML private TableView<LapResult> tablaResultados;
    @FXML private TableColumn<LapResult, Number> colPosicion;
    @FXML private TableColumn<LapResult, String> colPiloto;
    @FXML private TableColumn<LapResult, String> colEquipo;
    @FXML private TableColumn<LapResult, String> colTiempo;
    @FXML private TableColumn<LapResult, String> colGap;

    @FXML private ComboBox<String> selectorCircuito;
    @FXML private LineChart<String, Number> grafico;
    @FXML private Label lblVacio;

    private final QualifyingService sesiones;

    public HistoryController() {
        this(new QualifyingService());
    }

    public HistoryController(QualifyingService sesiones) {
        this.sesiones = sesiones;
    }

    @FXML
    public void initialize() {
        colFecha.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getFecha()));
        colCircuito.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getCircuito()));
        colClima.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getClima() == null ? "—" : f.getValue().getClima().getEtiqueta()));
        colPole.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getPole() == null ? "—" : f.getValue().getPole().getPiloto()));
        colTiempoPole.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getPole() == null ? "—"
                        : FormatUtils.formatLapTime(f.getValue().getPole().getTiempoSegundos())));
        colConfig.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getConfig() == null ? "—" : f.getValue().getConfig().toString()));

        colPosicion.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getPosicion()));
        colPiloto.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPiloto()));
        colEquipo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getEquipo()));
        colTiempo.setCellValueFactory(f -> new SimpleStringProperty(
                FormatUtils.formatLapResult(f.getValue())));
        colGap.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().isVueltaValida() ? FormatUtils.formatGap(f.getValue().getGap()) : "—"));

        tablaSesiones.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) ->
                tablaResultados.setItems(ahora == null
                        ? FXCollections.observableArrayList()
                        : FXCollections.observableArrayList(ahora.getResultados())));

        selectorCircuito.valueProperty().addListener((obs, antes, ahora) -> pintarGrafico(ahora));

        refrescar();
    }

    private void refrescar() {
        List<QualifyingSession> historial = sesiones.historial();
        tablaSesiones.setItems(FXCollections.observableArrayList(historial));
        lblVacio.setVisible(historial.isEmpty());

        List<String> circuitos = historial.stream()
                .map(QualifyingSession::getCircuito)
                .distinct()
                .collect(Collectors.toList());
        selectorCircuito.setItems(FXCollections.observableArrayList(circuitos));
        if (!circuitos.isEmpty()) {
            selectorCircuito.setValue(circuitos.get(0));
        }
    }

    /** Evolución del tiempo de pole en las sesiones de un mismo circuito. */
    private void pintarGrafico(String circuito) {
        grafico.getData().clear();
        if (circuito == null) {
            return;
        }
        List<QualifyingSession> delCircuito = sesiones.historial().stream()
                .filter(s -> circuito.equals(s.getCircuito()))
                .filter(s -> s.getPole() != null)
                .collect(Collectors.toList());

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Tiempo de pole en " + circuito);
        for (int i = 0; i < delCircuito.size(); i++) {
            QualifyingSession sesion = delCircuito.get(i);
            serie.getData().add(new XYChart.Data<>(
                    "S" + (delCircuito.size() - i), sesion.getPole().getTiempoSegundos()));
        }
        grafico.getData().add(serie);
    }

    /** Vuelve a la simulación; allí se precarga la última configuración. */
    @FXML
    private void onReutilizar() {
        QualifyingSession seleccionada = tablaSesiones.getSelectionModel().getSelectedItem();
        if (seleccionada == null || seleccionada.getConfig() == null) {
            Navigator.aviso("Sin selección", "Elige una sesión con configuración guardada.");
            return;
        }
        Navigator.ir("simulation");
    }

    @FXML
    private void onRefrescar() {
        refrescar();
    }
}
