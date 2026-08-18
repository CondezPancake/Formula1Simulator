package com.formula1.controller;

import com.formula1.model.AerodynamicLoad;
import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.EventOccurrence;
import com.formula1.model.FuelStrategy;
import com.formula1.model.LapResult;
import com.formula1.model.LapStatus;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SessionStatistics;
import com.formula1.model.SimulationConfig;
import com.formula1.model.SimulationSnapshot;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.TirePressure;
import com.formula1.model.TrackFlag;
import com.formula1.model.WeatherSnapshot;
import com.formula1.service.CircuitService;
import com.formula1.service.DriverService;
import com.formula1.service.QualifyingService;
import com.formula1.service.VehicleService;
import com.formula1.util.Async;
import com.formula1.util.TeamColors;
import com.formula1.util.FormatUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

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
    @FXML private Label lblPuestaAPunto;
    @FXML private Button btnSimular;
    @FXML private ProgressBar progreso;
    @FXML private Label lblEstado;
    @FXML private Label lblClima;
    @FXML private Label lblPilotoEvolucion;
    @FXML private Label lblVelocidad;
    @FXML private Label lblVelocidadMaxima;
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
    @FXML private Label lblVelocidadMaximaTelemetria;
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
    @FXML private Label lblEventoTelemetria;
    @FXML private Label lblEstadoPiloto;
    @FXML private LapEvolutionController evolucionVueltaController;
    @FXML private SectorComparisonController comparacionSectoresController;
    @FXML private TrackEvolutionController evolucionPistaController;
    @FXML private SessionAnalysisController analisisSesionController;
    @FXML private Tab tabClima;
    @FXML private Label lblEstadoClimaDinamico;
    @FXML private Label lblTempAmbiente;
    @FXML private Label lblHumedad;
    @FXML private Label lblProbLluvia;
    @FXML private Label lblIntensidadLluvia;
    @FXML private Label lblTempPista;
    @FXML private Label lblGrip;
    @FXML private Label lblTraccion;
    @FXML private Label lblFrenado;
    @FXML private Label lblNeumaticoClima;
    @FXML private Label lblEstrategiaClima;
    @FXML private ProgressBar barraGrip;
    @FXML private ProgressBar barraTraccion;
    @FXML private ProgressBar barraFrenado;
    @FXML private ComboBox<WeatherMetric> selectorMetricaClima;
    @FXML private LineChart<Number, Number> graficoClima;
    @FXML private NumberAxis ejeClimaValor;
    @FXML private TableView<LapResult> tabla;
    @FXML private TableColumn<LapResult, Number> colPosicion;
    @FXML private TableColumn<LapResult, String> colPiloto;
    @FXML private TableColumn<LapResult, String> colEquipo;
    @FXML private TableColumn<LapResult, String> colVehiculo;
    @FXML private TableColumn<LapResult, String> colTiempo;
    @FXML private TableColumn<LapResult, String> colGap;
    @FXML private TableColumn<LapResult, String> colConsumo;
    @FXML private TableColumn<LapResult, String> colDesgaste;
    @FXML private TableColumn<LapResult, String> colEstadoVuelta;
    @FXML private TableColumn<LapResult, String> colEventoResultado;
    @FXML private TableView<EventOccurrence> tablaEventos;
    @FXML private TableColumn<EventOccurrence, String> colEventoPiloto;
    @FXML private TableColumn<EventOccurrence, String> colEventoNombre;
    @FXML private TableColumn<EventOccurrence, String> colEventoCategoria;
    @FXML private TableColumn<EventOccurrence, String> colEventoAlcance;
    @FXML private TableColumn<EventOccurrence, String> colEventoSector;
    @FXML private TableColumn<EventOccurrence, String> colEventoImpacto;
    @FXML private TableColumn<EventOccurrence, String> colEventoBandera;

    private final QualifyingService sesiones;
    private final CircuitService circuitos;
    private final VehicleService vehiculos;
    private final DriverService pilotos;
    private QualifyingSession ultimaSesion;
    private double velocidadMaximaAlcanzada;
    private final List<WeatherSnapshot> muestrasClima = new ArrayList<>();

    // Puesta a punto elegida en CONFIG. & HISTORIAL; aquí solo se consulta.
    private DrivingMode modo = DrivingMode.NORMAL;
    private AerodynamicLoad aero = AerodynamicLoad.MEDIA;
    private TirePressure presion = TirePressure.ESTANDAR;
    private FuelStrategy combustible = FuelStrategy.BALANCEADA;

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
        selectorMetrica.getItems().addAll(MetricType.values());
        selectorMetrica.setValue(MetricType.DIFERENCIA_POLE);
        selectorMetrica.valueProperty().addListener((obs, anterior, actual) -> pintarGrafico());
        graficoRendimiento.setAnimated(false);
        selectorMetricaClima.getItems().addAll(WeatherMetric.values());
        selectorMetricaClima.setValue(WeatherMetric.TEMPERATURA_PISTA);
        selectorMetricaClima.valueProperty().addListener(
                (obs, anterior, actual) -> pintarGraficoClima());

        // El vehículo delimita los pilotos válidos y evita combinaciones de
        // escuderías distintas antes de que lleguen a la capa de servicio.
        selectorVehiculo.valueProperty().addListener((obs, anterior, actual) ->
                cargarPilotosDelVehiculo(actual));

        colPosicion.setCellValueFactory(f -> new SimpleIntegerProperty(f.getValue().getPosicion()));
        colPiloto.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPiloto()));
        colEquipo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getEquipo()));
        colVehiculo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getVehiculo()));
        colTiempo.setCellValueFactory(f -> new SimpleStringProperty(
                FormatUtils.formatLapResult(f.getValue())));
        colGap.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().isVueltaValida() ? FormatUtils.formatGap(f.getValue().getGap()) : "—"));
        colConsumo.setCellValueFactory(f -> new SimpleStringProperty(
                String.format("%.2f", f.getValue().getConsumoEstimado())));
        colDesgaste.setCellValueFactory(f -> new SimpleStringProperty(
                String.format("%.2f", f.getValue().getDesgasteEstimado())));
        colEstadoVuelta.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getEstadoVuelta().getEtiqueta()));
        colEventoResultado.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getEventoResumen()));
        configurarTablaEventos();

        // La pole se resalta con la clase .pole-row de la hoja de estilos.
        // Cada fila lleva a la izquierda la franja del color de su escudería,
        // igual que la tabla de tiempos del diseño.
        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(LapResult resultado, boolean vacia) {
                super.updateItem(resultado, vacia);
                getStyleClass().removeAll("pole-row", "invalid-row");
                if (vacia || resultado == null) {
                    setStyle("");
                    return;
                }
                if (!resultado.isVueltaValida()) {
                    getStyleClass().add("invalid-row");
                } else if (resultado.getPosicion() == 1) {
                    getStyleClass().add("pole-row");
                }
                setStyle("-fx-border-color: transparent transparent #17171B "
                        + TeamColors.hex(resultado.getEquipo())
                        + "; -fx-border-width: 0 0 1 3;");
            }
        });

        colPosicion.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Number valor, boolean vacia) {
                super.updateItem(valor, vacia);
                getStyleClass().removeAll("pos-badge", "p1", "p2", "p3");
                if (vacia || valor == null) {
                    setText(null);
                    return;
                }
                setText(String.valueOf(valor.intValue()));
                getStyleClass().add("pos-badge");
                switch (valor.intValue()) {
                    case 1 -> getStyleClass().add("p1");
                    case 2 -> getStyleClass().add("p2");
                    case 3 -> getStyleClass().add("p3");
                    default -> { }
                }
            }
        });
        colTiempo.getStyleClass().add("mono-col");
        colGap.getStyleClass().add("mono-col");

        precargarUltimaConfiguracion();
    }

    /**
     * La configuración se guarda automáticamente con cada sesión, así que
     * al abrir la pantalla se recupera la última empleada.
     */
    private void precargarUltimaConfiguracion() {
        // Lo que el usuario dejó preparado en la pantalla de configuración
        // manda sobre lo que se usó en la última sesión guardada.
        SimulationConfig ultima = com.formula1.data.DataStore.getInstance().configuracionActual();
        if (ultima == null) {
            ultima = sesiones.historial().stream()
                    .map(QualifyingSession::getConfig)
                    .filter(c -> c != null)
                    .findFirst()
                    .orElse(null);
        }

        // Los ajustes guardados pueden venir incompletos (la pantalla de
        // configuración solo fija la puesta a punto), así que cada campo cae
        // a su valor por defecto en vez de quedarse vacío.
        if (!selectorCircuito.getItems().isEmpty()) {
            selectorCircuito.setValue(selectorCircuito.getItems().get(0));
        }
        if (!selectorVehiculo.getItems().isEmpty()) {
            selectorVehiculo.setValue(selectorVehiculo.getItems().get(0));
        }
        modo = DrivingMode.NORMAL;
        aero = AerodynamicLoad.MEDIA;
        presion = TirePressure.ESTANDAR;
        combustible = FuelStrategy.BALANCEADA;

        if (ultima == null) {
            return;
        }
        if (ultima.getCircuito() != null) {
            selectorCircuito.setValue(ultima.getCircuito());
        }
        if (ultima.getVehiculo() != null) {
            selectorVehiculo.setValue(ultima.getVehiculo());
        }
        seleccionarPiloto(ultima.getPilotoId());
        if (ultima.getModo() != null) {
            modo = ultima.getModo();
        }
        if (ultima.getAerodinamica() != null) {
            aero = ultima.getAerodinamica();
        }
        if (ultima.getPresion() != null) {
            presion = ultima.getPresion();
        }
        if (ultima.getCombustible() != null) {
            combustible = ultima.getCombustible();
        }
        lblEstado.setText("Configuración recuperada");
        mostrarPuestaAPunto();
    }

    /** Resume la puesta a punto vigente, que se ajusta en la otra pantalla. */
    private void mostrarPuestaAPunto() {
        lblPuestaAPunto.setText(modo.getEtiqueta() + " · " + aero.getEtiqueta()
                + " · " + presion.getEtiqueta() + " · " + combustible.getEtiqueta());
    }

    /** Deep-link desde Explorar » Garaje: precarga el vehículo elegido. */
    public void precargarVehiculo(String modelo) {
        if (selectorVehiculo.getItems().contains(modelo)) {
            selectorVehiculo.setValue(modelo);
        }
    }

    @FXML
    private void onSimular() {
        if (!configuracionCompleta()) {
            Navigator.aviso("Falta configuración",
                    "Elige un circuito, un vehículo y un piloto.");
            return;
        }

        SimulationConfig config = new SimulationConfig(
                selectorCircuito.getValue(), selectorPiloto.getValue().getId(), selectorVehiculo.getValue(),
                modo, aero, presion, combustible);

        reiniciarEvolucion();
        reiniciarEstadisticas();
        reiniciarTelemetria();
        evolucionVueltaController.reiniciar();
        comparacionSectoresController.reiniciar();
        evolucionPistaController.reiniciar();
        analisisSesionController.reiniciar();
        reiniciarClimaDinamico();
        panelResultados.getSelectionModel().select(tabTelemetria);

        // La cabecera de la aplicación acompaña a la sesión: evento, estado,
        // contador de segmento, clima y bandera.
        circuitos.porNombre(config.getCircuito()).ifPresent(
                c -> ShellController.evento(c.getNombre(), c.getPais()));
        ShellController.estadoSesion(ShellController.Estado.EN_CURSO);
        ShellController.bandera(null);
        Task<QualifyingSession> tarea = sesiones.crearTarea(config,
                muestra -> Platform.runLater(() -> mostrarEvolucion(muestra)),
                muestra -> Platform.runLater(() -> {
                    mostrarTelemetria(muestra);
                    mostrarClimaDinamico(muestra.clima());
                }),
                muestra -> Platform.runLater(() -> evolucionPistaController.actualizar(muestra)));

        // Enlazar en vez de asignar: el Task publica sus cambios en el hilo
        // de JavaFX, así que la interfaz se actualiza sola y sin bloquearse.
        progreso.progressProperty().bind(tarea.progressProperty());
        lblEstado.textProperty().bind(tarea.messageProperty());
        btnSimular.disableProperty().bind(tarea.runningProperty());
        tabla.getItems().clear();
        tablaEventos.getItems().clear();
        lblClima.setText("");

        tarea.setOnSucceeded(e -> {
            desenlazar();
            QualifyingSession sesion = tarea.getValue();
            lblClima.setText(resumenClimatico(sesion));
            tabla.setItems(FXCollections.observableArrayList(sesion.getResultados()));
            tablaEventos.setItems(FXCollections.observableArrayList(sesion.getEventos()));
            evolucionVueltaController.cargar(sesion.getEvolucionVuelta());
            comparacionSectoresController.cargar(sesion.getResultados());
            evolucionPistaController.cargar(sesion.getEvolucionPista());
            analisisSesionController.cargar(sesion.getAnalisis());
            mostrarEstadisticas(sesion);
            LapResult pole = sesion.getPole();
            lblEstado.setText(pole == null ? "Sesión sin resultados"
                    : "Pole: " + pole.getPiloto() + " — " + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
            ShellController.estadoSesion(ShellController.Estado.TERMINADA);
            // Terminada la vuelta, lo que interesa es la parrilla, no la
            // telemetría en directo que ya dejó de moverse.
            panelResultados.getSelectionModel().selectFirst();
            // Guardar en segundo plano: la parrilla ya está en pantalla.
            Async.ejecutar(() -> sesiones.guardar(sesion));
        });

        tarea.setOnFailed(e -> {
            desenlazar();
            progreso.setProgress(0);
            lblEstado.setText("La simulación falló");
            ShellController.estadoSesion(ShellController.Estado.REPOSO);
            Throwable causa = tarea.getException();
            Navigator.error("No se pudo completar la clasificación",
                    causa == null ? "Error desconocido" : String.valueOf(causa.getMessage()));
        });

        Async.ejecutar(tarea);
    }

    private void reiniciarEvolucion() {
        velocidadMaximaAlcanzada = 0;
        lblPilotoEvolucion.setText("Esperando inicio de vuelta");
        lblVelocidad.setText("0 km/h");
        lblVelocidadMaxima.setText("Máxima alcanzada: 0 km/h");
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
        actualizarVelocidadMaxima(muestra.velocidadKmh());
        lblConsumoEvolucion.setText(String.format("%.2f / %.2f u",
                muestra.consumoAcumulado(), muestra.consumoTotal()));
        lblDesgasteEvolucion.setText(String.format("%.2f / %.2f u",
                muestra.desgasteAcumulado(), muestra.desgasteTotal()));
        progresoVuelta.setProgress(muestra.progreso());
        barraVelocidad.setProgress(muestra.velocidadRelativa());
        ShellController.segmento(muestra.segmento(), muestra.totalSegmentos());
        barraConsumo.setProgress(muestra.progreso());
        barraDesgaste.setProgress(muestra.progreso());
    }

    private void reiniciarTelemetria() {
        lblTelemetriaPiloto.setText("Esperando datos del vehículo seleccionado");
        lblEstadoPista.setText("Estado de pista —");
        lblVelocidadTelemetria.setText("0 km/h");
        lblVelocidadMaximaTelemetria.setText("Máxima alcanzada: 0 km/h");
        lblRpm.setText("0 rpm");
        lblCombustible.setText("100.0 %");
        lblDesgasteTelemetria.setText("0.0 %");
        lblTempNeumaticos.setText("0.0 °C");
        lblTempMotor.setText("0.0 °C");
        lblSector.setText("S—");
        lblTiempoVuelta.setText("0:00.000");
        lblDelta.setText("±0.000");
        lblEventoTelemetria.setText("Sin evento");
        lblEstadoPiloto.setText("Vuelta válida");
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
        evolucionVueltaController.actualizar(muestra);
        actualizarVelocidadMaxima(muestra.velocidadKmh());
        lblTelemetriaPiloto.setText(muestra.piloto() + " · " + muestra.vehiculo());
        String bandera = muestra.evento().impacto().bandera() == TrackFlag.GREEN
                ? "" : " · " + muestra.evento().impacto().bandera().getEtiqueta();
        lblEstadoPista.setText(muestra.estadoPista() + bandera);
        ShellController.bandera(muestra.evento().impacto().bandera());
        lblVelocidadTelemetria.setText(String.format("%.0f km/h", muestra.velocidadKmh()));
        lblRpm.setText(String.format("%,d rpm", muestra.rpm()));
        lblCombustible.setText(String.format("%.1f %%", muestra.combustibleRestantePorcentaje()));
        lblDesgasteTelemetria.setText(String.format("%.1f %%", muestra.desgasteNeumaticosPorcentaje()));
        lblTempNeumaticos.setText(String.format("%.1f °C", muestra.temperaturaNeumaticosC()));
        lblTempMotor.setText(String.format("%.1f °C", muestra.temperaturaMotorC()));
        lblSector.setText("S" + muestra.sectorActual());
        lblTiempoVuelta.setText(FormatUtils.formatLapTime(muestra.tiempoVueltaSegundos()));
        lblDelta.setText(muestra.estadoVuelta() == LapStatus.VALID
                ? FormatUtils.formatDelta(muestra.deltaSegundos()) : "INVALID");
        lblEstadoPiloto.setText(muestra.estadoVuelta().getEtiqueta());
        if (muestra.evento().ocurrio()) {
            lblEventoTelemetria.setText(muestra.evento().resumen());
        }

        lblDelta.getStyleClass().removeAll("delta-faster", "delta-slower");
        lblDelta.getStyleClass().add(muestra.estadoVuelta() != LapStatus.VALID
                || muestra.deltaSegundos() > 0 ? "delta-slower" : "delta-faster");
        barraVelocidadTelemetria.setProgress(muestra.velocidadRelativa());
        barraRpm.setProgress(muestra.rpmRelativas());
        barraCombustible.setProgress(muestra.combustibleRestantePorcentaje() / 100);
        barraDesgasteTelemetria.setProgress(muestra.desgasteNeumaticosPorcentaje() / 100);
        barraTempNeumaticos.setProgress(muestra.temperaturaNeumaticosC() / 125);
        barraTempMotor.setProgress(muestra.temperaturaMotorC() / 125);
    }

    private void actualizarVelocidadMaxima(double velocidadActual) {
        velocidadMaximaAlcanzada = Math.max(velocidadMaximaAlcanzada, velocidadActual);
        String maxima = String.format("Máxima alcanzada: %.0f km/h", velocidadMaximaAlcanzada);
        lblVelocidadMaxima.setText(maxima);
        lblVelocidadMaximaTelemetria.setText(maxima);
    }

    private void reiniciarClimaDinamico() {
        lblEstadoClimaDinamico.setText("Esperando evolución climática");
        lblTempAmbiente.setText("— °C");
        lblHumedad.setText("— %");
        lblProbLluvia.setText("— %");
        lblIntensidadLluvia.setText("— %");
        lblTempPista.setText("— °C");
        lblGrip.setText("— %");
        lblTraccion.setText("— %");
        lblFrenado.setText("— %");
        lblNeumaticoClima.setText("—");
        lblEstrategiaClima.setText("—");
        barraGrip.setProgress(0);
        barraTraccion.setProgress(0);
        barraFrenado.setProgress(0);
        graficoClima.setAnimated(false);
        muestrasClima.clear();
        pintarGraficoClima();
    }

    /** Actualiza el panel y su tendencia con una única muestra inmutable. */
    private void mostrarClimaDinamico(WeatherSnapshot muestra) {
        lblEstadoClimaDinamico.setText(
                muestra.estado().getEtiqueta() + " · " + muestra.estadoPista());
        lblTempAmbiente.setText(String.format("%.1f °C", muestra.temperaturaC()));
        lblHumedad.setText(String.format("%.0f %%", muestra.humedadPorcentaje()));
        lblProbLluvia.setText(String.format("%.0f %%", muestra.probabilidadLluviaPorcentaje()));
        lblIntensidadLluvia.setText(String.format("%.0f %%", muestra.intensidadLluviaPorcentaje()));
        lblTempPista.setText(String.format("%.1f °C", muestra.temperaturaPistaC()));
        ShellController.clima(muestra);
        lblGrip.setText(String.format("%.0f %%", muestra.gripPorcentaje()));
        lblTraccion.setText(String.format("%.0f %%", muestra.traccionPorcentaje()));
        lblFrenado.setText(String.format("%.0f %%", muestra.frenadoPorcentaje()));
        lblNeumaticoClima.setText(muestra.neumaticoRecomendado());
        lblEstrategiaClima.setText(muestra.estrategiaRecomendada());
        barraGrip.setProgress(muestra.gripPorcentaje() / 100);
        barraTraccion.setProgress(muestra.traccionPorcentaje() / 100);
        barraFrenado.setProgress(muestra.frenadoPorcentaje() / 100);
        int indice = muestra.segmento() - 1;
        if (indice < muestrasClima.size()) {
            muestrasClima.set(indice, muestra);
        } else {
            muestrasClima.add(muestra);
        }
        pintarGraficoClima();
    }

    private void pintarGraficoClima() {
        WeatherMetric metrica = selectorMetricaClima.getValue();
        graficoClima.getData().clear();
        if (metrica == null) {
            return;
        }
        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        serie.setName(metrica.etiqueta);
        muestrasClima.forEach(muestra -> serie.getData().add(
                new XYChart.Data<>(muestra.segmento(), metrica.valorDe(muestra))));
        ejeClimaValor.setLabel(metrica.unidad);
        graficoClima.setTitle(metrica.etiqueta + " durante la vuelta");
        graficoClima.getData().add(serie);
    }

    private String resumenClimatico(QualifyingSession sesion) {
        if (sesion.getEvolucionClimatica().isEmpty()) {
            return "Clima de la sesión: " + sesion.getClima().getEtiqueta();
        }
        WeatherSnapshot inicial = sesion.getEvolucionClimatica().get(0);
        WeatherSnapshot finalSesion = sesion.getEvolucionClimatica()
                .get(sesion.getEvolucionClimatica().size() - 1);
        return "Clima: " + inicial.estado().getEtiqueta()
                + " → " + finalSesion.estado().getEtiqueta();
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
        ultimaSesion.getResultados().stream()
                .filter(LapResult::isVueltaValida)
                .forEach(resultado -> serie.getData().add(
                        new XYChart.Data<>(resultado.getPiloto(), metrica.valorDe(resultado))));
        graficoRendimiento.getData().add(serie);
    }

    private void configurarTablaEventos() {
        colEventoPiloto.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().piloto()));
        colEventoNombre.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().tipo().getEtiqueta()));
        colEventoCategoria.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().categoria().getEtiqueta()));
        colEventoAlcance.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().alcance().getEtiqueta()));
        colEventoSector.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().sector().getEtiqueta()));
        colEventoImpacto.setCellValueFactory(f -> new SimpleStringProperty(
                describirImpacto(f.getValue())));
        colEventoBandera.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().impacto().bandera().getEtiqueta()));
    }

    private String describirImpacto(EventOccurrence evento) {
        if (evento.impacto().vueltaInvalidada()) {
            return evento.impacto().pilotoFuera() ? "INVALID · OUT" : "INVALID";
        }
        if (evento.impacto().deltaIntensidadLluviaPorcentaje() != 0) {
            return String.format("Lluvia %+.0f %%",
                    evento.impacto().deltaIntensidadLluviaPorcentaje());
        }
        return String.format("%+.3f s", evento.impacto().deltaTiempoSegundos());
    }

    private boolean configuracionCompleta() {
        return selectorCircuito.getValue() != null
                && selectorVehiculo.getValue() != null
                && selectorPiloto.getValue() != null;
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

    private enum WeatherMetric {
        TEMPERATURA_AMBIENTE("Temperatura ambiente", "°C", WeatherSnapshot::temperaturaC),
        TEMPERATURA_PISTA("Temperatura de pista", "°C", WeatherSnapshot::temperaturaPistaC),
        HUMEDAD("Humedad", "%", WeatherSnapshot::humedadPorcentaje),
        PROBABILIDAD_LLUVIA("Probabilidad de lluvia", "%",
                WeatherSnapshot::probabilidadLluviaPorcentaje),
        INTENSIDAD_LLUVIA("Intensidad de lluvia", "%",
                WeatherSnapshot::intensidadLluviaPorcentaje),
        GRIP("Grip de pista", "%", WeatherSnapshot::gripPorcentaje);

        private final String etiqueta;
        private final String unidad;
        private final ToDoubleFunction<WeatherSnapshot> extractor;

        WeatherMetric(String etiqueta, String unidad,
                      ToDoubleFunction<WeatherSnapshot> extractor) {
            this.etiqueta = etiqueta;
            this.unidad = unidad;
            this.extractor = extractor;
        }

        double valorDe(WeatherSnapshot muestra) {
            return extractor.applyAsDouble(muestra);
        }

        @Override
        public String toString() {
            return etiqueta;
        }
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
