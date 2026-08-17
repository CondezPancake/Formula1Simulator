package com.formula1.controller;

import com.formula1.model.AerodynamicLoad;
import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.FuelStrategy;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SessionStatistics;
import com.formula1.model.SimulationConfig;
import com.formula1.model.SimulationSnapshot;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.TirePressure;
import com.formula1.service.CircuitService;
import com.formula1.service.DriverService;
import com.formula1.service.QualifyingService;
import com.formula1.service.VehicleService;
import com.formula1.util.Async;
import com.formula1.util.FormatUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Configura y lanza una sesión de clasificación.
 *
 * El cálculo corre en un {@link Task} sobre el pool de hilos compartido, de
 * modo que la ventana sigue respondiendo mientras avanza la sesión.
 */
public class SimulationController {

    @FXML private ComboBox<String> selectorCircuito;
    @FXML private ComboBox<String> selectorVehiculo;
    @FXML private ComboBox<Driver> selectorPiloto;
    @FXML private ComboBox<DrivingMode> selectorModo;
    @FXML private ComboBox<AerodynamicLoad> selectorAero;
    @FXML private ComboBox<TirePressure> selectorPresion;
    @FXML private ComboBox<FuelStrategy> selectorCombustible;
    @FXML private Button btnSimular;
    @FXML private ProgressBar progreso;
    @FXML private Label lblEstado;
    @FXML private Label lblClima;
    @FXML private Label lblPilotoEvolucion;
    @FXML private Label lblVelocidad;
    @FXML private Label lblConsumoEvolucion;
    @FXML private Label lblDesgasteEvolucion;
    @FXML private ProgressBar progresoVuelta;
    @FXML private ProgressBar barraVelocidad;
    @FXML private ProgressBar barraConsumo;
    @FXML private ProgressBar barraDesgaste;
    @FXML private ComboBox<MetricType> selectorMetrica;
    @FXML private BarChart<String, Number> graficoRendimiento;
    @FXML private NumberAxis ejeMetrica;
    @FXML private Label lblPoleEstadistica;
    @FXML private Label lblPromedioEstadistica;
    @FXML private Label lblDiferenciaEstadistica;
    @FXML private Label lblParticipantesEstadistica;
    @FXML private TabPane panelResultados;
    @FXML private Tab tabTelemetria;
    @FXML private Label lblTelemetriaPiloto;
    @FXML private Label lblEstadoPista;
    @FXML private Label lblVelocidadTelemetria;
    @FXML private Label lblRpm;
    @FXML private Label lblCombustible;
    @FXML private Label lblDesgasteTelemetria;
    @FXML private Label lblTempNeumaticos;
    @FXML private Label lblTempMotor;
    @FXML private Label lblSector;
    @FXML private Label lblTiempoVuelta;
    @FXML private Label lblDelta;
    @FXML private ProgressBar barraVelocidadTelemetria;
    @FXML private ProgressBar barraRpm;
    @FXML private ProgressBar barraCombustible;
    @FXML private ProgressBar barraDesgasteTelemetria;
    @FXML private ProgressBar barraTempNeumaticos;
    @FXML private ProgressBar barraTempMotor;
    @FXML private TableView<LapResult> tabla;
    @FXML private TableColumn<LapResult, Number> colPosicion;
    @FXML private TableColumn<LapResult, String> colPiloto;
    @FXML private TableColumn<LapResult, String> colEquipo;
    @FXML private TableColumn<LapResult, String> colVehiculo;
    @FXML private TableColumn<LapResult, String> colTiempo;
    @FXML private TableColumn<LapResult, String> colGap;
    @FXML private TableColumn<LapResult, String> colConsumo;
    @FXML private TableColumn<LapResult, String> colDesgaste;

    private final QualifyingService sesiones;
    private final CircuitService circuitos;
    private final VehicleService vehiculos;
    private final DriverService pilotos;
    private QualifyingSession ultimaSesion;

    public SimulationController() {
        this(new QualifyingService(), new CircuitService(), new VehicleService(), new DriverService());
    }

    public SimulationController(QualifyingService sesiones, CircuitService circuitos,
                                VehicleService vehiculos, DriverService pilotos) {
        this.sesiones = sesiones;
        this.circuitos = circuitos;
        this.vehiculos = vehiculos;
        this.pilotos = pilotos;
    }

    @FXML
    public void initialize() {
        circuitos.listar().forEach(c -> selectorCircuito.getItems().add(c.getNombre()));
        vehiculos.listar().forEach(v -> selectorVehiculo.getItems().add(v.getModelo()));
        selectorModo.getItems().addAll(DrivingMode.values());
        selectorAero.getItems().addAll(AerodynamicLoad.values());
        selectorPresion.getItems().addAll(TirePressure.values());
        selectorCombustible.getItems().addAll(FuelStrategy.values());
        selectorMetrica.getItems().addAll(MetricType.values());
        selectorMetrica.setValue(MetricType.DIFERENCIA_POLE);
        selectorMetrica.valueProperty().addListener((obs, anterior, actual) -> pintarGrafico());
        graficoRendimiento.setAnimated(false);

        // El vehículo delimita los pilotos válidos y evita combinaciones de
        // escuderías distintas antes de que lleguen a la capa de servicio.
        selectorVehiculo.valueProperty().addListener((obs, anterior, actual) ->
                cargarPilotosDelVehiculo(actual));

        colPosicion.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getPosicion()));
        colPiloto.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPiloto()));
        colEquipo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getEquipo()));
        colVehiculo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getVehiculo()));
        colTiempo.setCellValueFactory(f -> new SimpleStringProperty(
                FormatUtils.formatLapTime(f.getValue().getTiempoSegundos())));
        colGap.setCellValueFactory(f -> new SimpleStringProperty(FormatUtils.formatGap(f.getValue().getGap())));
        colConsumo.setCellValueFactory(f -> new SimpleStringProperty(
                String.format("%.2f", f.getValue().getConsumoEstimado())));
        colDesgaste.setCellValueFactory(f -> new SimpleStringProperty(
                String.format("%.2f", f.getValue().getDesgasteEstimado())));

        // La pole se resalta con la clase .pole-row de la hoja de estilos.
        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(LapResult resultado, boolean vacia) {
                super.updateItem(resultado, vacia);
                getStyleClass().remove("pole-row");
                if (!vacia && resultado != null && resultado.getPosicion() == 1) {
                    getStyleClass().add("pole-row");
                }
            }
        });

        precargarUltimaConfiguracion();
    }

    /**
     * La configuración se guarda automáticamente con cada sesión, así que
     * al abrir la pantalla se recupera la última empleada.
     */
    private void precargarUltimaConfiguracion() {
        SimulationConfig ultima = sesiones.historial().stream()
                .map(QualifyingSession::getConfig)
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);

        if (ultima != null) {
            selectorCircuito.setValue(ultima.getCircuito());
            selectorVehiculo.setValue(ultima.getVehiculo());
            seleccionarPiloto(ultima.getPilotoId());
            selectorModo.setValue(ultima.getModo());
            selectorAero.setValue(ultima.getAerodinamica());
            selectorPresion.setValue(ultima.getPresion());
            selectorCombustible.setValue(ultima.getCombustible());
            lblEstado.setText("Configuración recuperada de la última sesión");
            return;
        }

        if (!selectorCircuito.getItems().isEmpty()) {
            selectorCircuito.setValue(selectorCircuito.getItems().get(0));
        }
        if (!selectorVehiculo.getItems().isEmpty()) {
            selectorVehiculo.setValue(selectorVehiculo.getItems().get(0));
        }
        selectorModo.setValue(DrivingMode.NORMAL);
        selectorAero.setValue(AerodynamicLoad.MEDIA);
        selectorPresion.setValue(TirePressure.ESTANDAR);
        selectorCombustible.setValue(FuelStrategy.BALANCEADA);
    }

    @FXML
    private void onSimular() {
        if (!configuracionCompleta()) {
            Navigator.aviso("Falta configuración",
                    "Elige un circuito, un vehículo, un piloto y todos los ajustes.");
            return;
        }

        SimulationConfig config = new SimulationConfig(
                selectorCircuito.getValue(), selectorPiloto.getValue().getId(), selectorVehiculo.getValue(),
                selectorModo.getValue(), selectorAero.getValue(),
                selectorPresion.getValue(), selectorCombustible.getValue());

        reiniciarEvolucion();
        reiniciarEstadisticas();
        reiniciarTelemetria();
        panelResultados.getSelectionModel().select(tabTelemetria);
        Task<QualifyingSession> tarea = sesiones.crearTarea(config,
                muestra -> Platform.runLater(() -> mostrarEvolucion(muestra)),
                muestra -> Platform.runLater(() -> mostrarTelemetria(muestra)));

        // Enlazar en vez de asignar: el Task publica sus cambios en el hilo
        // de JavaFX, así que la interfaz se actualiza sola y sin bloquearse.
        progreso.progressProperty().bind(tarea.progressProperty());
        lblEstado.textProperty().bind(tarea.messageProperty());
        btnSimular.disableProperty().bind(tarea.runningProperty());
        tabla.getItems().clear();
        lblClima.setText("");

        tarea.setOnSucceeded(e -> {
            desenlazar();
            QualifyingSession sesion = tarea.getValue();
            lblClima.setText("Clima de la sesión: " + sesion.getClima().getEtiqueta());
            tabla.setItems(FXCollections.observableArrayList(sesion.getResultados()));
            mostrarEstadisticas(sesion);
            LapResult pole = sesion.getPole();
            lblEstado.setText(pole == null ? "Sesión sin resultados"
                    : "Pole: " + pole.getPiloto() + " — " + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
            // Guardar en segundo plano: la parrilla ya está en pantalla.
            Async.ejecutar(() -> sesiones.guardar(sesion));
        });

        tarea.setOnFailed(e -> {
            desenlazar();
            progreso.setProgress(0);
            lblEstado.setText("La simulación falló");
            Throwable causa = tarea.getException();
            Navigator.error("No se pudo completar la clasificación",
                    causa == null ? "Error desconocido" : String.valueOf(causa.getMessage()));
        });

        Async.ejecutar(tarea);
    }

    private void reiniciarEvolucion() {
        lblPilotoEvolucion.setText("Esperando inicio de vuelta");
        lblVelocidad.setText("0 km/h");
        lblConsumoEvolucion.setText("0.00 / 0.00 u");
        lblDesgasteEvolucion.setText("0.00 / 0.00 u");
        progresoVuelta.setProgress(0);
        barraVelocidad.setProgress(0);
        barraConsumo.setProgress(0);
        barraDesgaste.setProgress(0);
    }

    /** Actualiza exclusivamente controles JavaFX; el motor entrega datos inmutables. */
    private void mostrarEvolucion(SimulationSnapshot muestra) {
        lblPilotoEvolucion.setText(muestra.piloto() + " · " + muestra.vehiculo());
        lblVelocidad.setText(String.format("%.0f km/h", muestra.velocidadKmh()));
        lblConsumoEvolucion.setText(String.format("%.2f / %.2f u",
                muestra.consumoAcumulado(), muestra.consumoTotal()));
        lblDesgasteEvolucion.setText(String.format("%.2f / %.2f u",
                muestra.desgasteAcumulado(), muestra.desgasteTotal()));
        progresoVuelta.setProgress(muestra.progreso());
        barraVelocidad.setProgress(muestra.velocidadRelativa());
        barraConsumo.setProgress(muestra.progreso());
        barraDesgaste.setProgress(muestra.progreso());
    }

    private void reiniciarTelemetria() {
        lblTelemetriaPiloto.setText("Esperando datos del vehículo seleccionado");
        lblEstadoPista.setText("Estado de pista —");
        lblVelocidadTelemetria.setText("0 km/h");
        lblRpm.setText("0 rpm");
        lblCombustible.setText("100.0 %");
        lblDesgasteTelemetria.setText("0.0 %");
        lblTempNeumaticos.setText("0.0 °C");
        lblTempMotor.setText("0.0 °C");
        lblSector.setText("S—");
        lblTiempoVuelta.setText("0:00.000");
        lblDelta.setText("±0.000");
        lblDelta.getStyleClass().removeAll("delta-faster", "delta-slower");
        barraVelocidadTelemetria.setProgress(0);
        barraRpm.setProgress(0);
        barraCombustible.setProgress(1);
        barraDesgasteTelemetria.setProgress(0);
        barraTempNeumaticos.setProgress(0);
        barraTempMotor.setProgress(0);
    }

    /** Representa una lectura ya calculada; no ejecuta lógica del motor en JavaFX. */
    private void mostrarTelemetria(TelemetrySnapshot muestra) {
        lblTelemetriaPiloto.setText(muestra.piloto() + " · " + muestra.vehiculo());
        lblEstadoPista.setText(muestra.estadoPista());
        lblVelocidadTelemetria.setText(String.format("%.0f km/h", muestra.velocidadKmh()));
        lblRpm.setText(String.format("%,d rpm", muestra.rpm()));
        lblCombustible.setText(String.format("%.1f %%", muestra.combustibleRestantePorcentaje()));
        lblDesgasteTelemetria.setText(String.format("%.1f %%", muestra.desgasteNeumaticosPorcentaje()));
        lblTempNeumaticos.setText(String.format("%.1f °C", muestra.temperaturaNeumaticosC()));
        lblTempMotor.setText(String.format("%.1f °C", muestra.temperaturaMotorC()));
        lblSector.setText("S" + muestra.sectorActual());
        lblTiempoVuelta.setText(FormatUtils.formatLapTime(muestra.tiempoVueltaSegundos()));
        lblDelta.setText(FormatUtils.formatDelta(muestra.deltaSegundos()));

        lblDelta.getStyleClass().removeAll("delta-faster", "delta-slower");
        lblDelta.getStyleClass().add(muestra.deltaSegundos() <= 0
                ? "delta-faster" : "delta-slower");
        barraVelocidadTelemetria.setProgress(muestra.velocidadRelativa());
        barraRpm.setProgress(muestra.rpmRelativas());
        barraCombustible.setProgress(muestra.combustibleRestantePorcentaje() / 100);
        barraDesgasteTelemetria.setProgress(muestra.desgasteNeumaticosPorcentaje() / 100);
        barraTempNeumaticos.setProgress(muestra.temperaturaNeumaticosC() / 125);
        barraTempMotor.setProgress(muestra.temperaturaMotorC() / 125);
    }

    private void reiniciarEstadisticas() {
        ultimaSesion = null;
        graficoRendimiento.getData().clear();
        lblPoleEstadistica.setText("—");
        lblPromedioEstadistica.setText("—");
        lblDiferenciaEstadistica.setText("—");
        lblParticipantesEstadistica.setText("0");
    }

    private void mostrarEstadisticas(QualifyingSession sesion) {
        ultimaSesion = sesion;
        SessionStatistics estadisticas = sesiones.calcularEstadisticas(sesion);
        LapResult pole = sesion.getPole();

        lblPoleEstadistica.setText(pole == null
                ? "—"
                : pole.getPiloto() + " · " + FormatUtils.formatLapTime(estadisticas.tiempoPole()));
        lblPromedioEstadistica.setText(FormatUtils.formatLapTime(estadisticas.tiempoPromedio()));
        lblDiferenciaEstadistica.setText(FormatUtils.formatGap(estadisticas.diferenciaMaxima()));
        lblParticipantesEstadistica.setText(String.valueOf(estadisticas.participantes()));
        pintarGrafico();
    }

    /** Convierte resultados del dominio en una única serie visual. */
    private void pintarGrafico() {
        graficoRendimiento.getData().clear();
        MetricType metrica = selectorMetrica.getValue();
        if (ultimaSesion == null || metrica == null) {
            return;
        }

        ejeMetrica.setLabel(metrica.getUnidad());
        graficoRendimiento.setTitle(metrica.getEtiqueta());
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(ultimaSesion.getCircuito());
        ultimaSesion.getResultados().forEach(resultado -> serie.getData().add(
                new XYChart.Data<>(resultado.getPiloto(), metrica.valorDe(resultado))));
        graficoRendimiento.getData().add(serie);
    }

    private boolean configuracionCompleta() {
        return selectorCircuito.getValue() != null
                && selectorVehiculo.getValue() != null
                && selectorPiloto.getValue() != null
                && selectorModo.getValue() != null
                && selectorAero.getValue() != null
                && selectorPresion.getValue() != null
                && selectorCombustible.getValue() != null;
    }

    private void cargarPilotosDelVehiculo(String modelo) {
        selectorPiloto.getItems().clear();
        selectorPiloto.setValue(null);

        vehiculos.porModelo(modelo).ifPresent(vehiculo ->
                selectorPiloto.getItems().setAll(vehiculos.pilotosDe(vehiculo)));
        selectorPiloto.setDisable(selectorPiloto.getItems().isEmpty());

        if (!selectorPiloto.getItems().isEmpty()) {
            selectorPiloto.setValue(selectorPiloto.getItems().get(0));
        }
    }

    /** Recupera por identificador porque el historial se persiste entre ejecuciones. */
    private void seleccionarPiloto(Integer pilotoId) {
        if (pilotoId == null) {
            return;
        }
        pilotos.porId(pilotoId)
                .filter(selectorPiloto.getItems()::contains)
                .ifPresent(selectorPiloto::setValue);
    }

    private void desenlazar() {
        progreso.progressProperty().unbind();
        lblEstado.textProperty().unbind();
        btnSimular.disableProperty().unbind();
    }

    private enum MetricType {
        TIEMPO_VUELTA("Tiempo de vuelta", "Segundos") {
            @Override
            double valorDe(LapResult resultado) {
                return resultado.getTiempoSegundos();
            }
        },
        DIFERENCIA_POLE("Diferencia con la pole", "Segundos") {
            @Override
            double valorDe(LapResult resultado) {
                return resultado.getGap();
            }
        },
        CONSUMO("Consumo estimado", "Unidades por vuelta") {
            @Override
            double valorDe(LapResult resultado) {
                return resultado.getConsumoEstimado();
            }
        },
        DESGASTE("Desgaste estimado", "Unidades por vuelta") {
            @Override
            double valorDe(LapResult resultado) {
                return resultado.getDesgasteEstimado();
            }
        };

        private final String etiqueta;
        private final String unidad;

        MetricType(String etiqueta, String unidad) {
            this.etiqueta = etiqueta;
            this.unidad = unidad;
        }

        abstract double valorDe(LapResult resultado);

        String getEtiqueta() {
            return etiqueta;
        }

        String getUnidad() {
            return unidad;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
