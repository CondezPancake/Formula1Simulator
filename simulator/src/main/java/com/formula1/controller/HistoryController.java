package com.formula1.controller;

import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.service.QualifyingService;
import com.formula1.util.FormatUtils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Historial de sesiones: parrillas guardadas, comparación de tiempos entre
 * sesiones del mismo circuito y reutilización de configuraciones previas.
 */
public class HistoryController {

    /** Más recientes primero; la fecha es un texto ISO, así que ordena bien. */
    private static final Comparator<QualifyingSession> POR_FECHA =
            Comparator.comparing(QualifyingSession::getFecha,
                    Comparator.nullsLast(Comparator.reverseOrder()));

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

    @FXML private Label lblConteoSesiones;
    @FXML private Label lblVictorias;
    @FXML private Label lblPodios;
    @FXML private Label lblPosicionMedia;
    @FXML private Label lblMejorVuelta;
    @FXML private Label lblMejorVueltaCircuito;
    @FXML private Button chipFecha;
    @FXML private Button chipPosicion;
    @FXML private Button chipTiempo;

    private final QualifyingService sesiones;
    private Comparator<QualifyingSession> orden = POR_FECHA;

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
        colPosicion.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Number valor, boolean vacia) {
                super.updateItem(valor, vacia);
                getStyleClass().removeAll("pos-badge", "p1", "p2", "p3");
                if (vacia || valor == null) {
                    setText(null);
                    return;
                }
                setText("P" + valor.intValue());
                getStyleClass().add("pos-badge");
                switch (valor.intValue()) {
                    case 1 -> getStyleClass().add("p1");
                    case 2 -> getStyleClass().add("p2");
                    case 3 -> getStyleClass().add("p3");
                    default -> { }
                }
            }
        });
        colTiempoPole.getStyleClass().add("mono-col");
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
        List<QualifyingSession> historial = sesiones.historial().stream()
                .sorted(orden)
                .collect(Collectors.toList());
        tablaSesiones.setItems(FXCollections.observableArrayList(historial));
        lblVacio.setVisible(historial.isEmpty());
        lblConteoSesiones.setText(historial.size() + " SESIONES");
        mostrarBalance(historial);

        List<String> circuitos = historial.stream()
                .map(QualifyingSession::getCircuito)
                .distinct()
                .collect(Collectors.toList());
        selectorCircuito.setItems(FXCollections.observableArrayList(circuitos));
        if (!circuitos.isEmpty()) {
            selectorCircuito.setValue(circuitos.get(0));
        }
    }

    /**
     * Victorias, podios y posición media del piloto que se eligió en cada
     * sesión. No hay ningún resumen guardado: se recorre la parrilla buscando
     * al piloto de la configuración, que puede no estar fijado.
     */
    private void mostrarBalance(List<QualifyingSession> historial) {
        List<LapResult> propios = historial.stream()
                .map(this::resultadoPropio)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        long victorias = propios.stream().filter(r -> r.getPosicion() == 1).count();
        long podios = propios.stream().filter(r -> r.getPosicion() <= 3).count();
        lblVictorias.setText(String.valueOf(victorias));
        lblPodios.setText(String.valueOf(podios));
        lblPosicionMedia.setText(propios.isEmpty() ? "—"
                : String.format(Locale.ROOT, "%.1f",
                        propios.stream().mapToInt(LapResult::getPosicion).average().orElse(0)));

        historial.stream()
                .filter(s -> s.getPole() != null)
                .min(Comparator.comparingDouble(s -> s.getPole().getTiempoSegundos()))
                .ifPresentOrElse(mejor -> {
                    lblMejorVuelta.setText(FormatUtils.formatLapTime(mejor.getPole().getTiempoSegundos()));
                    lblMejorVueltaCircuito.setText(mejor.getCircuito());
                }, () -> {
                    lblMejorVuelta.setText("—");
                    lblMejorVueltaCircuito.setText("");
                });
    }

    /** Vuelta del piloto que el usuario configuró para esa sesión. */
    private Optional<LapResult> resultadoPropio(QualifyingSession sesion) {
        if (sesion.getConfig() == null || sesion.getConfig().getPilotoId() == null) {
            return Optional.empty();
        }
        int propio = sesion.getConfig().getPilotoId();
        return sesion.getResultados().stream()
                .filter(r -> r.getPilotoId() == propio)
                .findFirst();
    }

    private void ordenarPor(Comparator<QualifyingSession> comparador, Button activo) {
        orden = comparador;
        for (Button chip : new Button[]{chipFecha, chipPosicion, chipTiempo}) {
            chip.getStyleClass().remove("chip-selected");
        }
        activo.getStyleClass().add("chip-selected");
        refrescar();
    }

    @FXML
    private void onOrdenarFecha() {
        ordenarPor(POR_FECHA, chipFecha);
    }

    @FXML
    private void onOrdenarPosicion() {
        ordenarPor(Comparator.comparingInt(
                s -> resultadoPropio(s).map(LapResult::getPosicion).orElse(Integer.MAX_VALUE)),
                chipPosicion);
    }

    @FXML
    private void onOrdenarTiempo() {
        ordenarPor(Comparator.comparingDouble(
                s -> s.getPole() == null ? Double.MAX_VALUE : s.getPole().getTiempoSegundos()),
                chipTiempo);
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
        SimulationConfig config = seleccionada.getConfig();
        com.formula1.data.DataStore.getInstance().guardarConfiguracion(config);
        ShellController.irACarrera();
        if (Navigator.ultimoControlador() instanceof SimulationController simulacion) {
            simulacion.precargarConfiguracion(config);
        }
    }

    @FXML
    private void onRefrescar() {
        refrescar();
    }
}
