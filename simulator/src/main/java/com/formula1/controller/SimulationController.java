package com.formula1.controller;

import com.formula1.domain.model.AerodynamicLoad;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.EventCategory;
import com.formula1.domain.model.EventOccurrence;
import com.formula1.domain.model.FuelStrategy;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.LapStatus;
import com.formula1.domain.model.LiveClassificationFrame;
import com.formula1.domain.model.PitStopRecord;
import com.formula1.domain.model.QualifyingSession;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.SimulationSnapshot;
import com.formula1.domain.model.TelemetrySnapshot;
import com.formula1.domain.model.TireChangeRecord;
import com.formula1.domain.model.TireCompound;
import com.formula1.domain.model.TirePressure;
import com.formula1.domain.model.TrackEvolutionSnapshot;
import com.formula1.domain.model.TrackFlag;
import com.formula1.domain.model.WeatherSnapshot;
import com.formula1.application.usecase.CircuitService;
import com.formula1.application.usecase.DriverService;
import com.formula1.application.usecase.QualifyingService;
import com.formula1.service.QualifyingSessionTaskFactory;
import com.formula1.domain.service.RaceRadioService;
import com.formula1.application.usecase.VehicleService;
import com.formula1.domain.model.TrackSector;
import com.formula1.domain.service.SectorComparisonService;
import com.formula1.util.Async;
import com.formula1.util.F1Assets;
import com.formula1.util.Imagenes;
import com.formula1.util.TeamColors;
import com.formula1.util.FormatUtils;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.animation.Interpolator;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @FXML private ComboBox<OpcionDuracion> selectorDuracion;
    @FXML private Label lblPuestaAPunto;
    @FXML private Button btnSimular;
    @FXML private Button btnFinalizar;
    @FXML private ProgressBar progreso;
    @FXML private Label lblEstado;
    @FXML private Label lblContador;
    @FXML private Label lblClima;
    @FXML private Label lblGripPista;
    @FXML private ProgressBar barraGripPista;
    @FXML private Label lblCambioGrip;
    @FXML private Label lblGomaPista;
    @FXML private Label lblTempClimaEvolucion;
    @FXML private Label lblEstadoClimaEvolucion;
    @FXML private Label lblLluviaEvolucion;
    @FXML private Label lblNeumaticoEvolucion;
    @FXML private Label lblEstrategiaEvolucion;
    @FXML private TabPane panelResultados;
    @FXML private Tab tabDashboard;
    @FXML private Tab tabResultadosFinales;
    @FXML private Tab tabTelemetria;
    @FXML private Tab tabEventos;
    @FXML private Button btnVerDetalles;
    @FXML private Button btnVerEventos;
    @FXML private ComboBox<Integer> selectorVueltaTelemetria;
    @FXML private ListView<LapResult> torreTiempos;
    @FXML private Label lblMapaTitulo;
    @FXML private Label lblMapaPista;
    @FXML private Label lblMapaGrip;
    @FXML private Label lblMapaVueltas;
    @FXML private StackPane contenedorMapa;
    @FXML private Label lblHudVuelta;
    @FXML private Label lblHudSector;
    @FXML private Label lblHudClima;
    @FXML private Label lblHudClimaDetalle;
    @FXML private Label lblMejorVueltaTiempo;
    @FXML private Label lblMejorVueltaPiloto;
    @FXML private Label lblDashboardEvento;
    @FXML private Label lblDashboardMensaje;
    @FXML private Label lblPitStopEstado;
    @FXML private Label lblPitStopTiempo;
    @FXML private Label lblDashboardCombustible;
    @FXML private Label lblDashboardDesgaste;
    @FXML private Label lblCompuestoDashboard;
    @FXML private Label lblTempNeumaticoPanel;
    @FXML private Label lblTempMotorPanel;
    @FXML private Label lblIconoClimaPanel;
    @FXML private Label lblNeumaticoRecomendado;
    @FXML private Label lblPolePiloto;
    @FXML private Label lblPoleTiempo;
    @FXML private Label lblSeleccion;
    @FXML private ImageView fotoPilotoUno;
    @FXML private ImageView siluetaCoche;
    @FXML private ProgressBar barraLlantasUno;
    @FXML private ProgressBar barraFuelUno;
    @FXML private VBox feedEventos;

    // Radio del box (HU-49): hilo lateral y rótulo sobre el trazado.
    @FXML private VBox radioHilo;
    @FXML private ScrollPane radioScroll;
    @FXML private Label radioCabeceraPiloto;
    @FXML private Region radioBarraEquipo;
    @FXML private HBox rotuloRadio;
    @FXML private Label rotuloRadioPiloto;
    @FXML private Label rotuloRadioTexto;
    @FXML private Label lblPilotoUno;
    @FXML private Label lblEquipoUno;
    @FXML private Label lblVehiculoUno;
    @FXML private Label lblExperienciaUno;
    @FXML private Label lblPilotoDos;
    @FXML private Label lblEquipoDos;
    @FXML private Label lblVehiculoDos;
    @FXML private Label lblExperienciaDos;
    @FXML private Label lblLlantasUno;
    @FXML private Label lblTempLlantasUno;
    @FXML private Label lblFuelUno;
    @FXML private Label lblTempMotorUno;
    @FXML private Label lblMarchaUno;
    @FXML private Label lblRpmUno;
    @FXML private Label lblPaceUno;
    @FXML private Label lblFuelModeUno;
    @FXML private Label lblErsUno;
    @FXML private ProgressBar barraErsUno;
    @FXML private Label lblLlantasDos;
    @FXML private Label lblTempLlantasDos;
    @FXML private Label lblFuelDos;
    @FXML private Label lblTempMotorDos;
    @FXML private Label lblMarchaDos;
    @FXML private Label lblRpmDos;
    @FXML private Label lblPaceDos;
    @FXML private Label lblFuelModeDos;
    @FXML private Label lblErsDos;
    @FXML private ProgressBar barraErsDos;
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
    @FXML private VBox notificacionEvento;
    @FXML private Label lblNotificacionTipo;
    @FXML private Label lblNotificacionCategoria;
    @FXML private Label lblNotificacionPiloto;
    @FXML private Label lblNotificacionSector;
    @FXML private Label lblNotificacionImpacto;
    @FXML private LineChart<Number, Number> graficaVelocidadDetalle;
    @FXML private LineChart<Number, Number> graficaRpmDetalle;
    @FXML private LineChart<Number, Number> graficaCombustibleDetalle;
    @FXML private LineChart<Number, Number> graficaDesgasteDetalle;
    @FXML private LineChart<Number, Number> graficaTemperaturasDetalle;
    @FXML private LineChart<Number, Number> graficaDeltaDetalle;
    @FXML private SectorComparisonController comparacionSectoresController;
    @FXML private PostQualifyingController resultadosFinalesController;
    @FXML private TableView<EventOccurrence> tablaEventos;
    @FXML private TableColumn<EventOccurrence, String> colEventoPiloto;
    @FXML private TableColumn<EventOccurrence, String> colEventoNombre;
    @FXML private TableColumn<EventOccurrence, String> colEventoCategoria;
    @FXML private TableColumn<EventOccurrence, String> colEventoAlcance;
    @FXML private TableColumn<EventOccurrence, String> colEventoSector;
    @FXML private TableColumn<EventOccurrence, String> colEventoImpacto;
    @FXML private TableColumn<EventOccurrence, String> colEventoBandera;

    private final QualifyingService sesiones;
    private final QualifyingSessionTaskFactory tareas;
    private final com.formula1.application.port.out.PreparedConfigPort configuracionPreparada;
    private final CircuitService circuitos;
    private final VehicleService vehiculos;
    private final DriverService pilotos;
    private double velocidadMaximaAlcanzada;

    // Puesta a punto elegida en CONFIG. & HISTORIAL; aquí solo se consulta.
    private DrivingMode modo = DrivingMode.NORMAL;
    private AerodynamicLoad aero = AerodynamicLoad.MEDIA;
    private TirePressure presion = TirePressure.ESTANDAR;
    private TireCompound compuestoInicial = TireCompound.MEDIUM;
    private FuelStrategy combustible = FuelStrategy.BALANCEADA;
    private int duracionSegundos = SimulationConfig.DURACION_PREDETERMINADA_SEGUNDOS;
    private long versionConfiguracionAplicada = -1;
    private double consumoVueltaAcumulado;
    private double consumoVueltaTotal;
    private double ersPiloto = 100;
    private double ersCompanero = 100;
    private final AtomicBoolean finalizarSolicitado = new AtomicBoolean();
    private Timeline relojInterfaz;
    private long inicioInterfazNanos;
    private boolean actualizandoSelectorDuracion;
    private final List<TelemetrySnapshot> telemetriaSesionActual = new ArrayList<>();
    private final ObservableList<LapResult> clasificacionVisible =
            FXCollections.observableArrayList();
    private final ObservableList<EventOccurrence> eventosVisibles =
            FXCollections.observableArrayList();
    private PauseTransition temporizadorNotificacionEvento;
    private final Set<String> eventosEnVivoRegistrados = new HashSet<>();
    private final Deque<EventOccurrence> colaNotificaciones = new ArrayDeque<>();
    private CircuitoEnVivo circuitoEnVivo;
    private PitStopPresenter pitStopPresenter;
    private TireChangePresenter tireChangePresenter;
    private TelemetryDetailPresenter telemetryDetailPresenter;
    private RadioPresenter radioPresenter;
    /** Genera el habla del muro de boxes a partir de lo que emite el motor. */
    private final RaceRadioService radio = new RaceRadioService();
    private Integer pilotoSesionActual;
    /** Código de tres letras por piloto; lo consulta cada fila de la torre. */
    private final Map<Integer, String> codigosPiloto = new HashMap<>();
    /** Mejor tiempo por sector, para los rótulos flotantes del mapa. */
    private final SectorComparisonService comparadorSectores = new SectorComparisonService();
    /** Piloto fijado desde la torre; null mientras no se elige ninguno. */
    private Integer pilotoFijado;
    private final MapaProgreso mapaProgreso = new MapaProgreso();
    /** Segmento de dominio correspondiente al último fotograma recibido. */
    private int segmentoEnVivo;

    public SimulationController() {
        this(new QualifyingService(), new CircuitService(), new VehicleService(), new DriverService(),
                com.formula1.adapter.out.memory.DataStore.getInstance());
    }

    public SimulationController(QualifyingService sesiones, CircuitService circuitos,
                                VehicleService vehiculos, DriverService pilotos,
                                com.formula1.application.port.out.PreparedConfigPort configuracionPreparada) {
        this.sesiones = sesiones;
        this.tareas = new QualifyingSessionTaskFactory(sesiones);
        this.circuitos = circuitos;
        this.vehiculos = vehiculos;
        this.pilotos = pilotos;
        this.configuracionPreparada = configuracionPreparada;
    }

    @FXML
    public void initialize() {
        // Antes que nada: actualizarMapaCircuito se llama al final de este
        // método y ya necesita el componente montado.
        circuitoEnVivo = new CircuitoEnVivo(contenedorMapa);
        pitStopPresenter = new PitStopPresenter(lblPitStopEstado, lblPitStopTiempo,
                lblDashboardEvento, lblDashboardMensaje, feedEventos);
        radioPresenter = new RadioPresenter(radioHilo, radioCabeceraPiloto, radioBarraEquipo,
                rotuloRadio, rotuloRadioPiloto, rotuloRadioTexto);
        tireChangePresenter = new TireChangePresenter(lblCompuestoDashboard,
                lblDashboardEvento, lblDashboardMensaje, feedEventos);
        telemetryDetailPresenter = new TelemetryDetailPresenter(
                selectorVueltaTelemetria, btnVerDetalles, lblTelemetriaPiloto,
                graficaVelocidadDetalle, graficaRpmDetalle,
                graficaCombustibleDetalle, graficaDesgasteDetalle,
                graficaTemperaturasDetalle, graficaDeltaDetalle);
        circuitoEnVivo.setAlSeleccionarPiloto(ExploreDriversController::abrirFicha);
        circuitoEnVivo.pilotoResaltadoProperty().addListener((o, a, b) -> torreTiempos.refresh());
        circuitoEnVivo.setResolucionCodigo(this::codigoDe);

        circuitos.listar().forEach(c -> selectorCircuito.getItems().add(c.getNombre()));
        vehiculos.listar().forEach(v -> selectorVehiculo.getItems().add(v.getModelo()));
        configurarSelectorDuracion();

        // El vehículo delimita los pilotos válidos y evita combinaciones de
        // escuderías distintas antes de que lleguen a la capa de servicio.
        selectorVehiculo.valueProperty().addListener((obs, anterior, actual) -> {
            cargarPilotosDelVehiculo(actual);
            actualizarTarjetasPilotos();
        });

        configurarTablaEventos();

        // Torre de tiempos del Dashboard, pintada como la señal de TV.
        torreTiempos.setCellFactory(lista -> new FilaTorre());
        torreTiempos.setItems(clasificacionVisible);
        tablaEventos.setItems(eventosVisibles);

        selectorCircuito.valueProperty().addListener((o, a, b) -> actualizarMapaCircuito(b));
        selectorPiloto.valueProperty().addListener((o, a, b) -> actualizarTarjetasPilotos());
        // Silueta del monoplaza del panel de telemetría: es decorativa y fija,
        // así que se carga una vez desde la caché de imágenes.
        siluetaCoche.setImage(Imagenes.cargar("/resource-new-dashboard/telemetry-car.jpg", 420, 0));
        precargarUltimaConfiguracion();
        actualizarMapaCircuito(selectorCircuito.getValue());
        actualizarTarjetasPilotos();
        reiniciarBaldosasTelemetria();
        telemetryDetailPresenter.load(List.of());
    }

    /**
     * Abreviatura de tres letras del piloto (VER, HAM, LEC).
     *
     * La torre repinta sus veinte filas en cada segmento, así que la consulta
     * al servicio se cachea: el código de un piloto no cambia en toda la sesión.
     */
    private String codigoPiloto(LapResult resultado) {
        return codigosPiloto.computeIfAbsent(resultado.getPilotoId(), id ->
                pilotos.porId(id)
                        .map(Driver::getCodigo)
                        .filter(codigo -> codigo != null && !codigo.isBlank())
                        .orElse(resultado.getPiloto()));
    }

    /** Código de tres letras por identificador, para el mapa y la radio. */
    private String codigoDe(Integer pilotoId) {
        if (pilotoId == null) {
            return "—";
        }
        return codigosPiloto.computeIfAbsent(pilotoId, id ->
                pilotos.porId(id)
                        .map(Driver::getCodigo)
                        .filter(codigo -> codigo != null && !codigo.isBlank())
                        .orElse("P" + id));
    }

    /**
     * A quién escucha la radio: al piloto fijado si lo hay y, si no, al que
     * se configuró, que es del único del que el motor emite telemetría.
     */
    private int pilotoDeRadio() {
        if (pilotoFijado != null) {
            return pilotoFijado;
        }
        return pilotoSesionActual == null ? -1 : pilotoSesionActual;
    }

    /** Pone la cabecera de la radio con el código y el color del piloto seguido. */
    private void sintonizarRadio(Integer pilotoId) {
        String equipo = pilotoId == null ? null
                : pilotos.porId(pilotoId).map(Driver::getEquipo).orElse(null);
        radioPresenter.seguirA(codigoDe(pilotoId), TeamColors.hex(equipo));
    }

    private void actualizarMapaCircuito(String nombre) {
        if (nombre == null) {
            circuitoEnVivo.mostrarCircuito(null);
            lblMapaTitulo.setText("—");
            lblMapaVueltas.setText("—");
            return;
        }
        circuitos.porNombre(nombre).ifPresent(this::mostrarCircuitoEnMapa);
    }

    private void mostrarCircuitoEnMapa(Circuit circuito) {
        lblMapaTitulo.setText(circuito.getNombre().toUpperCase());
        lblMapaPista.setText("Pista: " + circuito.getLongitudKm() + " km");
        lblMapaVueltas.setText(circuito.getVueltas() + " vueltas");
        circuitoEnVivo.mostrarCircuito(circuito);
    }

    /**
     * Fila de la torre de tiempos: posición, franja del color de la escudería,
     * código de tres letras e intervalo, como la señal de televisión.
     *
     * El grafo se construye una sola vez por celda y {@code updateItem} solo
     * reescribe textos y clases; con virtualización eso es un puñado de nodos
     * aunque la sesión repinte veinte filas por segmento.
     */
    private final class FilaTorre extends ListCell<LapResult> {

        private final Label posicion = new Label();
        private final Region franja = new Region();
        private final Label codigo = new Label();
        private final Label intervalo = new Label();
        private final Region estado = new Region();
        private final HBox grafo = new HBox(8);

        private FilaTorre() {
            posicion.getStyleClass().add("tower-pos");
            franja.getStyleClass().add("tower-team-bar");
            codigo.getStyleClass().add("tower-code");
            intervalo.getStyleClass().add("tower-gap");
            estado.getStyleClass().add("tower-state");

            Region espaciador = new Region();
            HBox.setHgrow(espaciador, Priority.ALWAYS);

            grafo.setAlignment(Pos.CENTER_LEFT);
            grafo.getStyleClass().add("tower-row");
            grafo.getChildren().addAll(posicion, franja, codigo, espaciador, intervalo, estado);

            // La torre y el mapa son la misma información: señalar en una tiene
            // que señalar en la otra. El hover es tanteo; el clic fija.
            // Consultar no desfija: el ratón puede curiosear a otro piloto
            // mientras el fijado sigue marcado en el mapa y en los sectores.
            setOnMouseEntered(e -> {
                if (getItem() != null) {
                    circuitoEnVivo.resaltar(getItem().getPilotoId());
                }
            });
            setOnMouseExited(e -> circuitoEnVivo.resaltar(null));
            setOnMouseClicked(e -> {
                if (getItem() == null) {
                    return;
                }
                if (e.getClickCount() == 2) {
                    ExploreDriversController.abrirFicha(getItem().getPilotoId());
                    return;
                }
                seleccionarPiloto(getItem());
            });
        }

        @Override
        protected void updateItem(LapResult resultado, boolean vacia) {
            super.updateItem(resultado, vacia);
            grafo.getStyleClass().removeAll("lider", "invalida", "activo", "fijado");
            if (vacia || resultado == null) {
                setGraphic(null);
                return;
            }

            posicion.setText(String.valueOf(resultado.getPosicion()));
            franja.setStyle("-fx-background-color: " + TeamColors.hex(resultado.getEquipo()) + ";");
            codigo.setText(codigoPiloto(resultado));

            // La torre reserva este indicador al estado deportivo de la vuelta;
            // el compuesto vigente se presenta en su lectura propia del Dashboard.
            estado.getStyleClass().removeAll("valida", "invalidada", "fuera");
            estado.getStyleClass().add(switch (resultado.getEstadoVuelta()) {
                case VALID -> "valida";
                case INVALID -> "invalidada";
                case OUT -> "fuera";
            });

            if (!resultado.isVueltaValida()) {
                intervalo.setText("INVALID");
                grafo.getStyleClass().add("invalida");
            } else if (resultado.getPosicion() == 1) {
                // El líder no tiene intervalo contra nadie: se muestra su vuelta.
                intervalo.setText(FormatUtils.formatLapResult(resultado));
                grafo.getStyleClass().add("lider");
            } else {
                intervalo.setText(FormatUtils.formatGap(resultado.getGap()));
            }

            Integer resaltado = circuitoEnVivo == null ? null
                    : circuitoEnVivo.pilotoResaltadoProperty().get();
            if (resaltado != null && resaltado == resultado.getPilotoId()) {
                grafo.getStyleClass().add("activo");
            }
            // El fijado se marca aparte y manda sobre el consultado.
            if (pilotoFijado != null && pilotoFijado == resultado.getPilotoId()) {
                grafo.getStyleClass().add("fijado");
            }
            setGraphic(grafo);
        }
    }

    /**
     * Vuelca una sesión ya calculada en todas las pestañas.
     *
     * Está separado del arranque porque la sesión no siempre llega de la
     * tarea que lanza esta pantalla: cuando se disputa desde la vista en vivo
     * es esa la que la trae ya terminada.
     */
    public void mostrarSesion(QualifyingSession sesion) {
        mostrarSesion(sesion, false);
    }

    /**
     * @param manual si la sesión se cortó desde el botón Finalizar, en cuyo caso
     *               los coches se quedan donde estaban en vez de saltar a la meta
     */
    public void mostrarSesion(QualifyingSession sesion, boolean manual) {
        if (sesion == null) {
            return;
        }
        // El historial reproduce sesiones cuyo circuito puede no ser el que
        // marca el selector: se pintaría la parrilla correcta sobre la pista
        // equivocada.
        circuitos.porNombre(sesion.getCircuito()).ifPresent(this::mostrarCircuitoEnMapa);
        lblClima.setText(resumenClimatico(sesion));
        clasificacionVisible.setAll(sesion.getResultados());
        eventosVisibles.setAll(sesion.getEventos());
        btnVerEventos.setDisable(sesion.getEventos().isEmpty());
        feedEventos.getChildren().clear();
        sesion.getEventos().forEach(this::anadirAlFeed);
        sesion.getParadasBoxes().forEach(pitStopPresenter::addToFeed);
        sesion.getCambiosNeumaticos().forEach(tireChangePresenter::addToFeed);
        pilotoSesionActual = sesion.getConfig() == null
                ? null : sesion.getConfig().getPilotoId();
        tireChangePresenter.showSession(
                sesion.getCambiosNeumaticos(), pilotoSesionActual,
                sesion.getConfig() == null
                        ? TireCompound.MEDIUM
                        : sesion.getConfig().getCompuestoInicial());
        actualizarEtiquetasSector(sesion.getResultados());
        comparacionSectoresController.cargar(sesion.getResultados());
        mostrarPistaDeSesion(sesion);
        telemetryDetailPresenter.load(sesion.getEvolucionVuelta());
        LapResult pole = sesion.getPole();
        lblEstado.setText(pole == null ? "Sesión sin resultados"
                : "Pole: " + pole.getPiloto() + " — " + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
        lblDashboardMensaje.setText(pole == null ? "Sesión terminada sin vueltas válidas"
                : "Pole para " + pole.getPiloto() + " con "
                        + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
        int incidencias = sesion.getEventos().size() + sesion.getParadasBoxes().size()
                + sesion.getCambiosNeumaticos().size();
        lblDashboardEvento.setText(incidencias == 0
                ? "SIN EVENTOS" : incidencias + " EVENTOS");
        pitStopPresenter.showLatest(sesion.getParadasBoxes());
        resultadosFinalesController.load(sesion, pilotoFijado);

        // El piloto fijado sobrevive a la sesión: se vuelve a marcar sobre la
        // parrilla final y su rótulo se pone al día con el resultado.
        circuitoEnVivo.fijar(pilotoFijado);
        refrescarSeleccion(sesion.getResultados());
        radioPresenter.vaciarPendientes();
        radioPresenter.encolar(radio.cierreDeSesion(
                sesion.getResultados().stream()
                        .filter(r -> r.getPilotoId() == pilotoDeRadio())
                        .findFirst()
                        .orElse(null),
                MapaProgreso.TOTAL_SEGMENTOS));
        tabResultadosFinales.setDisable(false);
        lblPolePiloto.setText(pole == null ? "—" : pole.getPiloto());
        lblPoleTiempo.setText(pole == null ? "—"
                : FormatUtils.formatLapTime(pole.getTiempoSegundos()));
        if (pole != null) {
            lblMejorVueltaTiempo.setText(FormatUtils.formatLapTime(pole.getTiempoSegundos()));
            lblMejorVueltaPiloto.setText(codigoPiloto(pole) + " · " + pole.getEquipo());
        }
        // Al terminar la vuelta completa el mapa se lleva a la parrilla final,
        // para no quedarse con la foto del penúltimo segmento. Si el usuario
        // cortó a mano no: nadie cruzó la meta, y saltar allí lo aparentaría.
        if (!manual) {
            circuitoEnVivo.publicar(mapaProgreso.construir(
                    MapaProgreso.TOTAL_SEGMENTOS, sesion.getResultados()));
        }
        circuitoEnVivo.finalizar(manual);
        ShellController.estadoSesion(ShellController.Estado.TERMINADA);
        panelResultados.getSelectionModel().select(tabDashboard);
    }

    /**
     * La configuración se guarda automáticamente con cada sesión, así que
     * al abrir la pantalla se recupera la última empleada.
     */
    private void precargarUltimaConfiguracion() {
        // Lo que el usuario dejó preparado en la pantalla de configuración
        // manda sobre lo que se usó en la última sesión guardada.
        SimulationConfig ultima = configuracionPreparada.configuracionActual();
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
        compuestoInicial = TireCompound.MEDIUM;
        combustible = FuelStrategy.BALANCEADA;

        if (ultima == null) {
            versionConfiguracionAplicada = configuracionPreparada.versionConfiguracion();
            return;
        }
        precargarConfiguracion(ultima);
        versionConfiguracionAplicada = configuracionPreparada.versionConfiguracion();
        lblEstado.setText("Configuración recuperada");
    }

    /** Resume la puesta a punto vigente, que se ajusta en la otra pantalla. */
    private void mostrarPuestaAPunto() {
        lblPuestaAPunto.setText(modo.getEtiqueta() + " · " + aero.getEtiqueta()
                + " · " + presion.getEtiqueta() + " · " + compuestoInicial.getCodigo()
                + " · " + combustible.getEtiqueta());
        if (lblPaceUno != null) {
            lblPaceUno.setText(etiquetaPaceInicial());
            lblFuelModeUno.setText(etiquetaCombustible(combustible));
        }
    }

    private void configurarSelectorDuracion() {
        selectorDuracion.getItems().setAll(
                new OpcionDuracion(5, "5 segundos"),
                new OpcionDuracion(10, "10 segundos"),
                new OpcionDuracion(30, "30 segundos"),
                new OpcionDuracion(60, "60 segundos"),
                new OpcionDuracion(120, "2 minutos"),
                new OpcionDuracion(null, "Personalizada…"));
        selectorDuracion.valueProperty().addListener((o, anterior, seleccion) -> {
            if (actualizandoSelectorDuracion || seleccion == null) {
                return;
            }
            if (seleccion.segundos() == null) {
                solicitarDuracionPersonalizada(anterior);
            } else {
                duracionSegundos = seleccion.segundos();
                actualizarContadorEnReposo();
            }
        });
        seleccionarDuracion(duracionSegundos);
    }

    private void solicitarDuracionPersonalizada(OpcionDuracion anterior) {
        TextInputDialog dialogo = new TextInputDialog(String.valueOf(duracionSegundos));
        dialogo.setTitle("Duración personalizada");
        dialogo.setHeaderText("Duración de la simulación");
        dialogo.setContentText("Segundos (1 a 3600):");
        Optional<String> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty()) {
            seleccionarDuracion(anterior != null && anterior.segundos() != null
                    ? anterior.segundos() : duracionSegundos);
            return;
        }
        try {
            int segundos = Integer.parseInt(respuesta.get().trim());
            if (segundos < SimulationConfig.DURACION_MINIMA_SEGUNDOS
                    || segundos > SimulationConfig.DURACION_MAXIMA_SEGUNDOS) {
                throw new NumberFormatException();
            }
            duracionSegundos = segundos;
            seleccionarDuracion(segundos);
            actualizarContadorEnReposo();
        } catch (NumberFormatException e) {
            Navigator.aviso("Duración no válida",
                    "Introduce un número entre 1 y 3600 segundos.");
            seleccionarDuracion(anterior != null && anterior.segundos() != null
                    ? anterior.segundos() : duracionSegundos);
        }
    }

    private void seleccionarDuracion(int segundos) {
        selectorDuracion.getItems().removeIf(valor -> valor.segundos() != null
                && valor.segundos() != 5 && valor.segundos() != 10
                && valor.segundos() != 30 && valor.segundos() != 60
                && valor.segundos() != 120);
        OpcionDuracion opcion = selectorDuracion.getItems().stream()
                .filter(valor -> Integer.valueOf(segundos).equals(valor.segundos()))
                .findFirst()
                .orElseGet(() -> new OpcionDuracion(segundos,
                        formatoDuracion(segundos) + " (personalizada)"));
        if (!selectorDuracion.getItems().contains(opcion)) {
            selectorDuracion.getItems().add(selectorDuracion.getItems().size() - 1, opcion);
        }
        actualizandoSelectorDuracion = true;
        selectorDuracion.setValue(opcion);
        actualizandoSelectorDuracion = false;
        duracionSegundos = segundos;
        actualizarContadorEnReposo();
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
        config.setDuracionSegundos(duracionSegundos);
        config.setCompuestoInicial(compuestoInicial);
        configuracionPreparada.guardarConfiguracion(config);
        versionConfiguracionAplicada = configuracionPreparada.versionConfiguracion();
        finalizarSolicitado.set(false);
        pilotoSesionActual = config.getPilotoId();
        btnFinalizar.setText("Finalizar");
        pitStopPresenter.reset();
        tireChangePresenter.reset(compuestoInicial);
        // La radio arranca escuchando al piloto configurado; si luego se fija
        // otro desde la torre, el canal cambia con él.
        radio.reiniciar();
        radioPresenter.reiniciar();
        sintonizarRadio(config.getPilotoId());
        radioPresenter.encolar(radio.saludoInicial(codigoDe(config.getPilotoId())));
        iniciarContador();

        reiniciarEvolucion();
        reiniciarTelemetria();
        comparacionSectoresController.reiniciar();
        panelResultados.getSelectionModel().select(tabDashboard);
        tabResultadosFinales.setDisable(true);

        segmentoEnVivo = 0;
        mapaProgreso.reiniciar();
        circuitos.porNombre(config.getCircuito()).ifPresent(this::mostrarCircuitoEnMapa);
        circuitoEnVivo.iniciarSesion(config.getPilotoId(), duracionSegundos);
        lblMejorVueltaTiempo.setText("—");
        lblMejorVueltaPiloto.setText("Sesión en curso");

        // La cabecera de la aplicación acompaña a la sesión: evento, estado,
        // contador de segmento, clima y bandera.
        circuitos.porNombre(config.getCircuito()).ifPresent(
                c -> ShellController.evento(c.getNombre(), c.getPais()));
        ShellController.estadoSesion(ShellController.Estado.EN_CURSO);
        ShellController.bandera(null);
        Task<QualifyingSession> tarea = tareas.crearTarea(config,
                muestra -> Platform.runLater(() -> mostrarEvolucion(muestra)),
                muestra -> Platform.runLater(() -> {
                    mostrarTelemetria(muestra);
                    mostrarClimaResumen(muestra.clima());
                }),
                muestra -> Platform.runLater(() -> mostrarEvolucionPista(muestra)),
                fotograma -> Platform.runLater(() -> mostrarClasificacionEnVivo(fotograma)),
                evento -> Platform.runLater(() -> registrarEventoEnVivo(evento)),
                parada -> Platform.runLater(() -> mostrarPitStop(parada)),
                cambio -> Platform.runLater(() -> mostrarCambioNeumaticos(cambio)),
                finalizarSolicitado::get);

        // Enlazar en vez de asignar: el Task publica sus cambios en el hilo
        // de JavaFX, así que la interfaz se actualiza sola y sin bloquearse.
        progreso.progressProperty().bind(tarea.progressProperty());
        lblEstado.textProperty().bind(tarea.messageProperty());
        btnSimular.disableProperty().bind(tarea.runningProperty());
        btnFinalizar.disableProperty().bind(tarea.runningProperty().not());
        selectorDuracion.disableProperty().bind(tarea.runningProperty());
        clasificacionVisible.clear();
        eventosVisibles.clear();
        feedEventos.getChildren().clear();
        pilotoFijado = null;
        btnVerEventos.setDisable(true);
        lblClima.setText("");

        tarea.setOnSucceeded(e -> {
            boolean finalizadaManualmente = finalizarSolicitado.get();
            detenerContador(!finalizadaManualmente);
            desenlazar();
            // La sesión se lee una sola vez y aquí, en el hilo de JavaFX:
            // Task.getValue() lanza si se consulta desde otro hilo, y el
            // guardado de abajo corre en el pool.
            QualifyingSession resultado = tarea.getValue();
            mostrarSesion(resultado, finalizadaManualmente);
            panelResultados.getSelectionModel().select(tabResultadosFinales);
            if (finalizadaManualmente) {
                lblEstado.setText("Sesión finalizada manualmente · resultados guardados");
                lblDashboardEvento.setText("FINALIZADA");
            }
            // Guardar en segundo plano: la parrilla ya está en pantalla.
            Async.ejecutar(() -> sesiones.guardar(resultado));
        });

        tarea.setOnFailed(e -> {
            detenerContador(false);
            desenlazar();
            circuitoEnVivo.detener();
            progreso.setProgress(0);
            lblEstado.setText("La simulación falló");
            ShellController.estadoSesion(ShellController.Estado.REPOSO);
            Throwable causa = tarea.getException();
            Navigator.error("No se pudo completar la clasificación",
                    causa == null ? "Error desconocido" : String.valueOf(causa.getMessage()));
        });

        Async.ejecutar(tarea);
    }

    @FXML
    private void onFinalizar() {
        if (finalizarSolicitado.compareAndSet(false, true)) {
            btnFinalizar.setText("Finalizando…");
        }
    }

    private void iniciarContador() {
        if (relojInterfaz != null) {
            relojInterfaz.stop();
        }
        inicioInterfazNanos = System.nanoTime();
        actualizarContador(0);
        relojInterfaz = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            long transcurridos = Math.min(duracionSegundos,
                    (System.nanoTime() - inicioInterfazNanos) / 1_000_000_000L);
            actualizarContador((int) transcurridos);
        }));
        relojInterfaz.setCycleCount(Timeline.INDEFINITE);
        relojInterfaz.play();
    }

    private void detenerContador(boolean completar) {
        if (relojInterfaz != null) {
            relojInterfaz.stop();
        }
        int transcurridos = completar ? duracionSegundos : (int) Math.min(duracionSegundos,
                (System.nanoTime() - inicioInterfazNanos) / 1_000_000_000L);
        actualizarContador(transcurridos);
    }

    private void actualizarContadorEnReposo() {
        if (lblContador != null && (relojInterfaz == null
                || relojInterfaz.getStatus() != Timeline.Status.RUNNING)) {
            actualizarContador(0);
        }
    }

    private void actualizarContador(int transcurridos) {
        lblContador.setText(formatoDuracion(transcurridos) + " / "
                + formatoDuracion(duracionSegundos));
    }

    private String formatoDuracion(int totalSegundos) {
        return String.format("%02d:%02d", totalSegundos / 60, totalSegundos % 60);
    }

    /** Refresca la torre mientras los pilotos van completando sus vueltas. */
    private void mostrarClasificacionEnVivo(LiveClassificationFrame fotograma) {
        List<LapResult> resultados = fotograma.classification();
        segmentoEnVivo = fotograma.segment();

        clasificacionVisible.setAll(resultados);

        // El mapa se alimenta de esta misma lista: el marcador y la fila no son
        // dos cálculos que puedan discrepar, son el mismo dato.
        circuitoEnVivo.publicar(mapaProgreso.construir(fotograma.progress(), resultados));
        actualizarVueltaHud();
        actualizarEtiquetasSector(resultados);

        if (resultados.isEmpty()) {
            return;
        }
        radioPresenter.encolar(radio.desdeClasificacion(resultados, pilotoDeRadio(),
                segmentoEnVivo));
        // El rótulo del piloto fijado se refresca con la parrilla: si no, se
        // quedaría con la posición y el gap del instante en que se pulsó.
        refrescarSeleccion(resultados);

        LapResult lider = resultados.get(0);
        lblMejorVueltaTiempo.setText(FormatUtils.formatLapResult(lider));
        lblMejorVueltaPiloto.setText(codigoPiloto(lider) + " · " + lider.getEquipo());
        lblDashboardEvento.setText("EN VIVO");
        lblDashboardMensaje.setText("Líder provisional: " + lider.getPiloto()
                + " · " + FormatUtils.formatLapResult(lider)
                + " · " + resultados.size() + " clasificados");
    }

    /**
     * Alimenta los rótulos de sector del mapa.
     *
     * Con un piloto fijado se muestra su diferencia contra el mejor de ese
     * sector; sin selección, el mejor tiempo absoluto. Ambos salen de
     * {@code SectorTimes}, que el motor solo rellena en vueltas válidas.
     */
    private void actualizarEtiquetasSector(List<LapResult> resultados) {
        TrackSector[] sectores = {TrackSector.SECTOR_1, TrackSector.SECTOR_2, TrackSector.SECTOR_3};
        String[] valores = new String[3];
        for (int i = 0; i < sectores.length; i++) {
            Optional<LapResult> mejor = comparadorSectores.mejorEn(resultados, sectores[i]);
            if (mejor.isEmpty()) {
                continue;
            }
            double referencia = mejor.get().getSectorTimes().tiempoDe(sectores[i]);
            LapResult elegido = pilotoFijado == null ? null : resultados.stream()
                    .filter(r -> r.getPilotoId() == pilotoFijado)
                    .filter(r -> r.isVueltaValida() && r.hasSectorTimes())
                    .findFirst()
                    .orElse(null);
            valores[i] = elegido == null
                    ? String.format(java.util.Locale.ROOT, "%.3f", referencia)
                    : String.format(java.util.Locale.ROOT, "%+.3f",
                            elegido.getSectorTimes().tiempoDe(sectores[i]) - referencia);
        }
        circuitoEnVivo.setEtiquetasSector(valores);
    }

    private void actualizarVueltaHud() {
        lblHudVuelta.setText(segmentoEnVivo + " / " + MapaProgreso.TOTAL_SEGMENTOS);
        lblHudSector.setText(com.formula1.domain.model.TrackSector
                .desdeSegmento(Math.max(1, segmentoEnVivo), MapaProgreso.TOTAL_SEGMENTOS)
                .getEtiqueta().toUpperCase());
    }

    private void reiniciarEvolucion() {
        velocidadMaximaAlcanzada = 0;
        consumoVueltaAcumulado = 0;
        consumoVueltaTotal = 0;
        lblDashboardMensaje.setText("Preparando datos de la vuelta seleccionada");
        lblDashboardEvento.setText("SIN EVENTOS");
        eventosEnVivoRegistrados.clear();
        colaNotificaciones.clear();
        ocultarNotificacionEvento();
        pitStopPresenter.reset();
        reiniciarPistaYClima();
    }

    /** Actualiza exclusivamente controles JavaFX; el motor entrega datos inmutables. */
    private void mostrarEvolucion(SimulationSnapshot muestra) {
        consumoVueltaAcumulado = muestra.consumoAcumulado();
        consumoVueltaTotal = muestra.consumoTotal();
        actualizarVelocidadMaxima(muestra.velocidadKmh());
        ShellController.segmento(muestra.segmento(), muestra.totalSegmentos());
    }

    private void reiniciarTelemetria() {
        telemetriaSesionActual.clear();
        telemetryDetailPresenter.load(List.of());
        lblTelemetriaPiloto.setText("Esperando datos del vehículo seleccionado");
        lblEstadoPista.setText("Estado de pista —");
        lblVelocidadTelemetria.setText("0 km/h");
        lblVelocidadMaximaTelemetria.setText("Máxima alcanzada: 0 km/h");
        lblRpm.setText("0 rpm");
        lblCombustible.setText("0.00 kg/v · 0 % consumido");
        lblDesgasteTelemetria.setText("0.0 %");
        lblTempNeumaticos.setText("0.0 °C");
        lblTempMotor.setText("0.0 °C");
        lblSector.setText("S—");
        lblTiempoVuelta.setText("0:00.000");
        lblDelta.setText("±0.000");
        lblEventoTelemetria.setText("Sin evento");
        lblEstadoPiloto.setText("Vuelta válida");
        lblDelta.getStyleClass().removeAll("delta-faster", "delta-slower");
        lblDashboardCombustible.setText("100 %");
        lblDashboardDesgaste.setText("100 %");
        barraVelocidadTelemetria.setProgress(0);
        barraRpm.setProgress(0);
        barraCombustible.setProgress(0);
        barraDesgasteTelemetria.setProgress(0);
        barraTempNeumaticos.setProgress(0);
        barraTempMotor.setProgress(0);
        reiniciarTelemetriaTarjetas();
        reiniciarBaldosasTelemetria();
    }

    /** Representa una lectura ya calculada; no ejecuta lógica del motor en JavaFX. */
    private void mostrarTelemetria(TelemetrySnapshot muestra) {
        // La telemetría sí trae el número de segmento y llega en el mismo
        // fotograma que la clasificación: se aprovecha para resincronizar el
        // contador en vez de fiarlo todo a ir sumando uno.
        segmentoEnVivo = muestra.segmento();
        actualizarVueltaHud();

        telemetriaSesionActual.add(muestra);
        telemetryDetailPresenter.append(muestra);
        actualizarVelocidadMaxima(muestra.velocidadKmh());
        lblTelemetriaPiloto.setText(muestra.piloto() + " · " + muestra.vehiculo());
        String bandera = muestra.evento().impacto().bandera() == TrackFlag.GREEN
                ? "" : " · " + muestra.evento().impacto().bandera().getEtiqueta();
        lblEstadoPista.setText(muestra.estadoPista() + bandera);
        ShellController.bandera(muestra.evento().impacto().bandera());
        lblVelocidadTelemetria.setText(String.format("%.0f km/h", muestra.velocidadKmh()));
        lblRpm.setText(String.format("%,d rpm", muestra.rpm()));
        double porcentajeConsumido = consumoVueltaTotal <= 0 ? 0
                : Math.min(100, 100 * consumoVueltaAcumulado / consumoVueltaTotal);
        lblCombustible.setText(String.format("%.2f kg/v · %.0f %% consumido",
                consumoVueltaTotal, porcentajeConsumido));
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
            lblDashboardEvento.setText(muestra.evento().tipo().getEtiqueta().toUpperCase());
            lblDashboardMensaje.setText(muestra.evento().resumen());
        } else {
            lblDashboardEvento.setText("EN VIVO");
            lblDashboardMensaje.setText(muestra.piloto() + " · Sector "
                    + muestra.sectorActual() + " · "
                    + muestra.estadoVuelta().getEtiqueta());
        }

        double combustibleRestante = muestra.combustibleRestantePorcentaje();
        double vidaNeumatico = Math.max(0, 100 - muestra.desgasteNeumaticosPorcentaje());
        lblDashboardCombustible.setText(String.format("%.0f %%", combustibleRestante));
        lblDashboardDesgaste.setText(String.format("%.0f %%", vidaNeumatico));
        lblTempNeumaticoPanel.setText(String.format("%.0f °C", muestra.temperaturaNeumaticosC()));
        lblTempMotorPanel.setText(String.format("%.0f °C", muestra.temperaturaMotorC()));
        // Verde mientras hay holgura, ámbar al acercarse al límite, rojo al pasarlo.
        estadoBaldosa(lblDashboardDesgaste, vidaNeumatico, 40, 20, false);
        estadoBaldosa(lblDashboardCombustible, combustibleRestante, 25, 10, false);
        estadoBaldosa(lblTempNeumaticoPanel, muestra.temperaturaNeumaticosC(), 100, 115, true);
        estadoBaldosa(lblTempMotorPanel, muestra.temperaturaMotorC(), 105, 120, true);

        lblDelta.getStyleClass().removeAll("delta-faster", "delta-slower");
        lblDelta.getStyleClass().add(muestra.estadoVuelta() != LapStatus.VALID
                || muestra.deltaSegundos() > 0 ? "delta-slower" : "delta-faster");
        barraVelocidadTelemetria.setProgress(muestra.velocidadRelativa());
        barraRpm.setProgress(muestra.rpmRelativas());
        barraCombustible.setProgress(porcentajeConsumido / 100);
        barraDesgasteTelemetria.setProgress(muestra.desgasteNeumaticosPorcentaje() / 100);
        barraTempNeumaticos.setProgress(muestra.temperaturaNeumaticosC() / 125);
        barraTempMotor.setProgress(muestra.temperaturaMotorC() / 125);
        actualizarTelemetriaTarjetas(muestra);
        // La telemetría es el reloj regular de la sesión (una muestra por
        // segmento): es de donde salen los avisos de coche y bandera.
        radioPresenter.encolar(radio.desdeTelemetria(muestra));
    }

    /** Precarga una configuración elegida en Historial sin iniciar ni borrar la sesión actual. */
    public void precargarConfiguracion(SimulationConfig config) {
        if (config == null) {
            return;
        }
        if (config.getCircuito() != null && selectorCircuito.getItems().contains(config.getCircuito())) {
            selectorCircuito.setValue(config.getCircuito());
        }
        if (config.getVehiculo() != null && selectorVehiculo.getItems().contains(config.getVehiculo())) {
            selectorVehiculo.setValue(config.getVehiculo());
        }
        seleccionarPiloto(config.getPilotoId());
        if (config.getModo() != null) modo = config.getModo();
        if (config.getAerodinamica() != null) aero = config.getAerodinamica();
        if (config.getPresion() != null) presion = config.getPresion();
        compuestoInicial = config.getCompuestoInicial();
        if (config.getCombustible() != null) combustible = config.getCombustible();
        seleccionarDuracion(config.getDuracionSegundos());
        if (!lblEstado.textProperty().isBound()) {
            lblEstado.setText("Configuración preparada para la próxima sesión");
        }
        mostrarPuestaAPunto();
    }

    /** Aplica un guardado nuevo sin tocar Carrera cuando no hay cambios pendientes. */
    public void aplicarConfiguracionGuardadaPendiente() {
        long version = configuracionPreparada.versionConfiguracion();
        if (version == versionConfiguracionAplicada) {
            return;
        }
        precargarConfiguracion(configuracionPreparada.configuracionActual());
        versionConfiguracionAplicada = version;
    }

    private void actualizarVelocidadMaxima(double velocidadActual) {
        velocidadMaximaAlcanzada = Math.max(velocidadMaximaAlcanzada, velocidadActual);
        String maxima = String.format("Máxima alcanzada: %.0f km/h", velocidadMaximaAlcanzada);
        lblVelocidadMaximaTelemetria.setText(maxima);
    }

    /** Deja las baldosas del panel derecho en su estado de reposo. */
    private void reiniciarBaldosasTelemetria() {
        lblDashboardDesgaste.setText("100 %");
        lblDashboardCombustible.setText("100 %");
        lblTempNeumaticoPanel.setText("— °C");
        lblTempMotorPanel.setText("— °C");
        for (Label baldosa : new Label[]{lblDashboardDesgaste, lblDashboardCombustible,
                lblTempNeumaticoPanel, lblTempMotorPanel}) {
            baldosa.getStyleClass().removeAll("ok", "warn", "crit");
        }
    }

    /**
     * Colorea una baldosa según su holgura. Los umbrales van en la unidad del
     * dato, y {@code masEsPeor} distingue "queda poco" de "se calienta".
     */
    private void estadoBaldosa(Label baldosa, double valor, double aviso, double critico,
                               boolean masEsPeor) {
        baldosa.getStyleClass().removeAll("ok", "warn", "crit");
        boolean critica = masEsPeor ? valor >= critico : valor <= critico;
        boolean avisa = masEsPeor ? valor >= aviso : valor <= aviso;
        baldosa.getStyleClass().add(critica ? "crit" : avisa ? "warn" : "ok");
    }

    /** Abre el análisis sin abandonar la sesión ni crear otra pantalla. */
    @FXML
    private void onVerDetalles() {
        if (telemetryDetailPresenter.hasData()) {
            panelResultados.getSelectionModel().select(tabTelemetria);
        }
    }

    /** Regresa al panel operativo conservando la vuelta elegida. */
    @FXML
    private void onVolverDashboard() {
        panelResultados.getSelectionModel().select(tabDashboard);
    }

    /** Abre el historial completo desde el resumen compacto del Dashboard. */
    @FXML
    private void onVerEventos() {
        panelResultados.getSelectionModel().select(tabEventos);
    }


    /**
     * Vuelca la muestra climatica en la tarjeta del panel y en la cabecera.
     *
     * De las once magnitudes que produce el motor aqui solo se enseñan las que
     * cambian una decision: que tiempo hace, como esta el asfalto y con que
     * neumatico se sale. El resto sigue calculandose porque el tiempo de vuelta
     * depende de ello, pero no necesita pantalla.
     */
    private void mostrarClimaResumen(WeatherSnapshot muestra) {
        // Unica llamada del proyecto: sin ella la cabecera se queda sin clima.
        ShellController.clima(muestra);
        lblTempClimaEvolucion.setText(String.format("%.0f °C · Asfalto %.0f °C",
                muestra.temperaturaC(), muestra.temperaturaPistaC()));
        lblEstadoClimaEvolucion.setText(
                muestra.estado().getEtiqueta() + " · " + muestra.estadoPista());
        lblLluviaEvolucion.setText(String.format("Lluvia %.0f %% · Humedad %.0f %%",
                muestra.intensidadLluviaPorcentaje(), muestra.humedadPorcentaje()));
        lblNeumaticoEvolucion.setText(muestra.neumaticoRecomendado());
        lblEstrategiaEvolucion.setText(muestra.estrategiaRecomendada());
        // El panel de clima del deck se alimenta de la misma muestra.
        lblHudClima.setText(String.format("%.0f °C", muestra.temperaturaC()));
        lblHudClimaDetalle.setText(muestra.estado().getEtiqueta().toUpperCase()
                + " · PISTA " + muestra.estadoPista().toUpperCase());
        lblNeumaticoRecomendado.setText("NEUMÁTICO " + muestra.neumaticoRecomendado().toUpperCase()
                + " · HUMEDAD " + String.format("%.0f %%", muestra.humedadPorcentaje()));
        // Mismos glifos que la cabecera: los emojis de clima no los dibuja JavaFX.
        lblIconoClimaPanel.setText(switch (muestra.estado()) {
            case SECO -> "☀";
            case NUBLADO -> "░";
            case LLUVIA_LIGERA -> "▒";
            case LLUVIA -> "▓";
            case LLUVIA_INTENSA -> "█";
        });
        lblMapaPista.setText("Pista: " + muestra.estadoPista());
    }

    /**
     * Pinta el grip mientras rueda el piloto elegido.
     *
     * El motor emite una lectura de pista por participante y el seleccionado
     * sale el primero, asi que su muestra es la de vuelta 1 y llega antes que
     * la telemetria: el panel se rellena nada mas arrancar la simulacion. Las
     * lecturas de los rivales se ignoran aqui y se resumen al cerrar la sesion.
     */
    private void mostrarEvolucionPista(TrackEvolutionSnapshot muestra) {
        if (muestra.vuelta() != 1) {
            return;
        }
        pintarGrip(muestra.gripInicialPorcentaje(), muestra.gripFinalPorcentaje(),
                muestra.tendencia(),
                String.format("Goma %.2f → %.2f %%",
                        muestra.gomaInicialPorcentaje(), muestra.gomaFinalPorcentaje()));
    }

    /** Al terminar, el grip pasa a contar la sesion entera, no una sola vuelta. */
    private void mostrarPistaDeSesion(QualifyingSession sesion) {
        List<TrackEvolutionSnapshot> pista = sesion.getEvolucionPista();
        if (pista.isEmpty()) {
            return;
        }
        TrackEvolutionSnapshot primera = pista.get(0);
        TrackEvolutionSnapshot ultima = pista.get(pista.size() - 1);
        pintarGrip(primera.gripInicialPorcentaje(), ultima.gripFinalPorcentaje(),
                ultima.tendencia() + " tras " + pista.size() + " vueltas",
                String.format("Goma %.2f → %.2f %%",
                        primera.gomaInicialPorcentaje(), ultima.gomaFinalPorcentaje()));
    }

    private void pintarGrip(double inicial, double finalGrip, String tendencia, String goma) {
        lblGripPista.setText(String.format("%.2f %% → %.2f %%", inicial, finalGrip));
        lblCambioGrip.setText(String.format("%+.2f pts · %s", finalGrip - inicial, tendencia));
        lblGomaPista.setText(goma);
        barraGripPista.setProgress(finalGrip / 100);
        lblMapaGrip.setText(String.format("Grip: %.0f %%", finalGrip));
    }

    private void reiniciarPistaYClima() {
        lblGripPista.setText("—");
        lblCambioGrip.setText("Sin datos de pista");
        lblGomaPista.setText("—");
        barraGripPista.setProgress(0);
        lblTempClimaEvolucion.setText("—");
        lblEstadoClimaEvolucion.setText("Sin datos de sesión");
        lblLluviaEvolucion.setText("—");
        lblNeumaticoEvolucion.setText("—");
        lblEstrategiaEvolucion.setText("—");
        lblMapaGrip.setText("Grip: —");
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

    /** Registra toda incidencia, pero solo interrumpe visualmente si es importante. */
    private void registrarEventoEnVivo(EventOccurrence evento) {
        String clave = evento.tipo() + ":" + evento.pilotoId() + ":"
                + evento.vuelta() + ":" + evento.sector();
        if (!eventosEnVivoRegistrados.add(clave)) {
            return;
        }
        tablaEventos.getItems().add(evento);
        btnVerEventos.setDisable(false);
        anadirAlFeed(evento);
        radio.desdeEvento(evento, pilotoDeRadio()).ifPresent(radioPresenter::encolar);
        if (esEventoImportante(evento.categoria())) {
            colaNotificaciones.add(evento);
            mostrarSiguienteNotificacion();
        }
    }

    /** Presenta un cambio de fase sin trasladar reglas de boxes a JavaFX. */
    private void mostrarPitStop(PitStopRecord parada) {
        String clave = "PIT:" + parada.id() + ":" + parada.fase();
        if (!eventosEnVivoRegistrados.add(clave)) {
            return;
        }
        pitStopPresenter.present(parada);
        radioPresenter.encolar(radio.desdeParada(parada, pilotoDeRadio()));
    }

    private void mostrarCambioNeumaticos(TireChangeRecord cambio) {
        String clave = "TIRE:" + cambio.pitStopId();
        if (eventosEnVivoRegistrados.add(clave)) {
            tireChangePresenter.present(cambio, pilotoSesionActual);
            radio.desdeCambioNeumaticos(cambio, pilotoDeRadio())
                    .ifPresent(radioPresenter::encolar);
        }
    }

    /**
     * Añade una incidencia al feed del panel derecho, la más reciente arriba.
     *
     * Los eventos no llevan marca de tiempo —la clasificación es de una sola
     * vuelta—, así que a la derecha va el sector, que sí es dato real y sitúa
     * la incidencia igual de bien.
     */
    private void anadirAlFeed(EventOccurrence evento) {
        if (feedEventos == null || !evento.ocurrio()) {
            return;
        }
        Region punto = new Region();
        punto.getStyleClass().addAll("feed-dot", switch (evento.categoria()) {
            case POSITIVE -> "positivo";
            case MAJOR_NEGATIVE, EXCEPTIONAL -> "grave";
            case WEATHER_TRACK -> "clima";
            default -> "leve";
        });

        Label texto = new Label(evento.tipo().getEtiqueta());
        texto.getStyleClass().add("feed-text");
        Label piloto = new Label(evento.piloto());
        piloto.getStyleClass().add("feed-driver");
        Label sector = new Label(evento.sector() == TrackSector.NONE ? "—"
                : "S" + evento.sector().name().substring(evento.sector().name().length() - 1));
        sector.getStyleClass().add("feed-time");

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        VBox cuerpo = new VBox(texto, piloto);
        HBox fila = new HBox(8, punto, cuerpo, espaciador, sector);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("feed-row");

        feedEventos.getChildren().add(0, fila);
        // El feed es una vista, no un registro: la tabla de Eventos guarda todo.
        while (feedEventos.getChildren().size() > 14) {
            feedEventos.getChildren().remove(feedEventos.getChildren().size() - 1);
        }

        FadeTransition entrada = new FadeTransition(Duration.millis(200), fila);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.setInterpolator(Interpolator.EASE_BOTH);
        entrada.play();
    }

    /** Los incidentes leves y positivos quedan en el historial sin generar toast. */
    static boolean esEventoImportante(EventCategory categoria) {
        return categoria == EventCategory.MAJOR_NEGATIVE
                || categoria == EventCategory.WEATHER_TRACK
                || categoria == EventCategory.EXCEPTIONAL;
    }

    /** Presenta cada aviso importante por turnos para que ninguno tape a otro. */
    private void mostrarSiguienteNotificacion() {
        if (notificacionEvento.isVisible() || colaNotificaciones.isEmpty()) {
            return;
        }
        EventOccurrence evento = colaNotificaciones.removeFirst();
        lblNotificacionTipo.setText(evento.tipo().getEtiqueta());
        lblNotificacionCategoria.setText(evento.categoria().getEtiqueta().toUpperCase());
        lblNotificacionPiloto.setText(evento.piloto());
        lblNotificacionSector.setText(evento.sector().getEtiqueta().toUpperCase());
        lblNotificacionImpacto.setText("Impacto · " + describirImpacto(evento));
        notificacionEvento.getStyleClass().removeAll(
                "event-toast-major", "event-toast-weather", "event-toast-exceptional");
        notificacionEvento.getStyleClass().add(switch (evento.categoria()) {
            case WEATHER_TRACK -> "event-toast-weather";
            case EXCEPTIONAL -> "event-toast-exceptional";
            default -> "event-toast-major";
        });

        if (temporizadorNotificacionEvento != null) {
            temporizadorNotificacionEvento.stop();
        }
        notificacionEvento.setOpacity(0);
        notificacionEvento.setVisible(true);
        FadeTransition entrada = new FadeTransition(Duration.millis(160), notificacionEvento);
        entrada.setToValue(1);
        entrada.play();

        temporizadorNotificacionEvento = new PauseTransition(Duration.seconds(4));
        temporizadorNotificacionEvento.setOnFinished(e -> ocultarNotificacionEvento());
        temporizadorNotificacionEvento.play();
    }

    private void ocultarNotificacionEvento() {
        if (temporizadorNotificacionEvento != null) {
            temporizadorNotificacionEvento.stop();
        }
        if (!notificacionEvento.isVisible()) {
            return;
        }
        FadeTransition salida = new FadeTransition(Duration.millis(220), notificacionEvento);
        salida.setToValue(0);
        salida.setOnFinished(e -> {
            notificacionEvento.setVisible(false);
            mostrarSiguienteNotificacion();
        });
        salida.play();
    }

    /** Permite retirar el aviso inmediatamente sin afectar su registro. */
    @FXML
    private void onCerrarNotificacion() {
        colaNotificaciones.clear();
        ocultarNotificacionEvento();
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

    /** Las dos tarjetas inferiores muestran al piloto elegido y a su compañero. */
    private void actualizarTarjetasPilotos() {
        List<Driver> equipo = new ArrayList<>(selectorPiloto.getItems());
        Driver seleccionado = selectorPiloto.getValue() != null
                ? selectorPiloto.getValue()
                : equipo.stream().findFirst().orElse(null);
        Driver companero = seleccionado == null ? null : equipo.stream()
                .filter(piloto -> !piloto.equals(seleccionado))
                .findFirst()
                .orElse(null);

        pintarTarjetaPiloto(seleccionado, lblPilotoUno, lblEquipoUno,
                lblVehiculoUno, lblExperienciaUno);
        pintarRetrato(seleccionado);
        pintarTarjetaPiloto(companero, lblPilotoDos, lblEquipoDos,
                lblVehiculoDos, lblExperienciaDos);
        lblFuelModeUno.setText(etiquetaCombustible(combustible));
        lblFuelModeDos.setText("BALANCEADA");
    }

    private void pintarTarjetaPiloto(Driver piloto, Label nombre, Label equipo,
                                     Label vehiculo, Label dorsal) {
        if (piloto == null) {
            nombre.setText("—");
            equipo.setText("—");
            vehiculo.setText(selectorVehiculo.getValue() == null
                    ? "—" : selectorVehiculo.getValue());
            dorsal.setText("—");
            return;
        }

        nombre.setText(piloto.getNombre());
        equipo.setText(piloto.getEquipo());
        vehiculo.setText(selectorVehiculo.getValue());
        dorsal.setText("#" + piloto.getNumero());
    }

    /**
     * Fija o suelta el piloto elegido en la torre.
     *
     * Fijar lo mantiene resaltado en el mapa aunque el ratón se vaya, y cambia
     * los rótulos de sector a la diferencia de ese piloto contra el mejor. Las
     * lecturas del coche no cambian: el motor solo emite telemetría del piloto
     * configurado, así que atribuírsela a otro sería inventarla.
     */
    private void seleccionarPiloto(LapResult resultado) {
        boolean mismo = pilotoFijado != null && pilotoFijado == resultado.getPilotoId();
        pilotoFijado = mismo ? null : resultado.getPilotoId();
        // Fijar y consultar son estados distintos: el mapa mantiene el fijado
        // aunque el ratón se vaya a curiosear otro coche.
        circuitoEnVivo.fijar(pilotoFijado);
        torreTiempos.refresh();

        // La radio cambia de canal con el piloto fijado; sin fijado, vuelve al
        // configurado, que es del único del que hay telemetría.
        sintonizarRadio(pilotoFijado != null ? pilotoFijado : pilotoSesionActual);

        refrescarSeleccion(torreTiempos.getItems());
        FadeTransition destello = new FadeTransition(Duration.millis(150), lblSeleccion);
        destello.setFromValue(0.2);
        destello.setToValue(1);
        destello.setInterpolator(Interpolator.EASE_BOTH);
        destello.play();

        actualizarEtiquetasSector(torreTiempos.getItems());
    }

    /**
     * Reescribe el rótulo del piloto fijado con su posición y gap vigentes.
     *
     * Se llama en cada actualización de la parrilla, no solo al pulsar: de otro
     * modo el rótulo congelaría el momento del clic y acabaría mintiendo.
     */
    private void refrescarSeleccion(List<LapResult> clasificacion) {
        if (pilotoFijado == null) {
            lblSeleccion.setText("Sin piloto fijado");
            return;
        }
        clasificacion.stream()
                .filter(r -> r.getPilotoId() == pilotoFijado)
                .findFirst()
                .ifPresentOrElse(actual -> lblSeleccion.setText(
                        "SIGUIENDO · " + codigoPiloto(actual) + " · P" + actual.getPosicion()
                                + " · " + (actual.getPosicion() == 1
                                        ? FormatUtils.formatLapResult(actual)
                                        : FormatUtils.formatGap(actual.getGap()))),
                        () -> lblSeleccion.setText("SIGUIENDO · " + codigoDe(pilotoFijado)));
    }

    /** Retrato del HUD inferior. El render oficial existe para los veinte códigos. */
    private void pintarRetrato(Driver piloto) {
        if (fotoPilotoUno == null) {
            return;
        }
        fotoPilotoUno.setImage(piloto == null ? null
                : Imagenes.cargar(F1Assets.render(piloto.getCodigo()), 140, 0));
    }

    private void reiniciarTelemetriaTarjetas() {
        ersPiloto = 100;
        ersCompanero = 100;
        pintarLecturaTarjeta(lblLlantasUno, lblTempLlantasUno, lblFuelUno,
                lblTempMotorUno, lblMarchaUno, lblRpmUno, lblPaceUno,
                lblErsUno, barraErsUno, 100, 0, 100, 0, "N", 0,
                etiquetaPaceInicial(), ersPiloto);
        pintarLecturaTarjeta(lblLlantasDos, lblTempLlantasDos, lblFuelDos,
                lblTempMotorDos, lblMarchaDos, lblRpmDos, lblPaceDos,
                lblErsDos, barraErsDos, 100, 0, 100, 0, "N", 0,
                "NORMAL", ersCompanero);
        barraLlantasUno.setProgress(1);
        barraFuelUno.setProgress(1);
        lblFuelModeUno.setText(etiquetaCombustible(combustible));
        lblFuelModeDos.setText("BALANCEADA");
    }

    /**
     * La primera tarjeta usa la lectura real emitida por el motor. La segunda
     * conserva el mismo estado de pista con una variación pequeña y estable,
     * representando el coche gemelo sin inventar saltos aleatorios en pantalla.
     */
    private void actualizarTelemetriaTarjetas(TelemetrySnapshot muestra) {
        double cargaErs = muestra.velocidadRelativa() < 0.52 ? 1.8
                : muestra.velocidadRelativa() > 0.76 ? -2.8 : -0.6;
        if (muestra.evento().impacto().bandera() != TrackFlag.GREEN) {
            cargaErs = 2.2;
        }
        ersPiloto = limitar(ersPiloto + cargaErs, 20, 100);

        double oscilacion = Math.sin(muestra.segmento() * 0.72);
        double velocidadCompanero = limitar(
                muestra.velocidadKmh() * (0.975 + 0.012 * oscilacion),
                0, muestra.velocidadMaximaKmh());
        double velocidadRelativaCompanero = velocidadCompanero / muestra.velocidadMaximaKmh();
        double cargaErsCompanero = velocidadRelativaCompanero < 0.50 ? 1.6
                : velocidadRelativaCompanero > 0.75 ? -2.6 : -0.5;
        ersCompanero = limitar(ersCompanero + cargaErsCompanero, 20, 100);

        pintarLecturaTarjeta(lblLlantasUno, lblTempLlantasUno, lblFuelUno,
                lblTempMotorUno, lblMarchaUno, lblRpmUno, lblPaceUno,
                lblErsUno, barraErsUno,
                100 - muestra.desgasteNeumaticosPorcentaje(),
                muestra.temperaturaNeumaticosC(),
                combustibleVisible(muestra.combustibleRestantePorcentaje()),
                muestra.temperaturaMotorC(),
                marchaPara(muestra.velocidadKmh()), muestra.rpm(), pacePara(muestra),
                ersPiloto);
        barraLlantasUno.setProgress(
                Math.max(0, 100 - muestra.desgasteNeumaticosPorcentaje()) / 100);
        barraFuelUno.setProgress(
                combustibleVisible(muestra.combustibleRestantePorcentaje()) / 100);

        pintarLecturaTarjeta(lblLlantasDos, lblTempLlantasDos, lblFuelDos,
                lblTempMotorDos, lblMarchaDos, lblRpmDos, lblPaceDos,
                lblErsDos, barraErsDos,
                limitar(100 - muestra.desgasteNeumaticosPorcentaje() * 1.06, 0, 100),
                limitar(muestra.temperaturaNeumaticosC() + 1.7 * oscilacion, 0, 150),
                limitar(combustibleVisible(muestra.combustibleRestantePorcentaje())
                        - 0.5 + 0.3 * oscilacion, 0, 100),
                limitar(muestra.temperaturaMotorC() + 1.2 * oscilacion, 0, 160),
                marchaPara(velocidadCompanero),
                (int) Math.round(limitar(muestra.rpm() * (0.98 + 0.01 * oscilacion), 0, 20_000)),
                paceCompanero(muestra, velocidadRelativaCompanero), ersCompanero);
    }

    private void pintarLecturaTarjeta(Label llantas, Label tempLlantas, Label fuel,
                                      Label tempMotor, Label marcha, Label rpm,
                                      Label pace, Label ers, ProgressBar barraErs,
                                      double vidaLlantas, double temperaturaLlantas,
                                      double combustibleRestante, double temperaturaMotor,
                                      String marchaActual, int rpmActual, String paceActual,
                                      double cargaErs) {
        llantas.setText(String.format("%.0f %%", limitar(vidaLlantas, 0, 100)));
        tempLlantas.setText(temperaturaLlantas <= 0
                ? "— °C" : String.format("%.0f °C", temperaturaLlantas));
        fuel.setText(String.format("%.0f %%", limitar(combustibleRestante, 0, 100)));
        tempMotor.setText(temperaturaMotor <= 0
                ? "— °C motor" : String.format("%.0f °C motor", temperaturaMotor));
        marcha.setText(marchaActual);
        rpm.setText(String.format("%,d RPM", rpmActual));
        pace.setText(paceActual);
        ers.setText(String.format("%.0f %%", cargaErs));
        barraErs.setProgress(cargaErs / 100);
    }

    private String marchaPara(double velocidadKmh) {
        if (velocidadKmh < 1) return "N";
        if (velocidadKmh < 82) return "2";
        if (velocidadKmh < 125) return "3";
        if (velocidadKmh < 168) return "4";
        if (velocidadKmh < 212) return "5";
        if (velocidadKmh < 255) return "6";
        if (velocidadKmh < 295) return "7";
        return "8";
    }

    /** El snapshot expresa el combustible asignado a la vuelta; la tarjeta lo
     * traduce al porcentaje del depósito, cuya caída en una vuelta ronda 12 %. */
    private double combustibleVisible(double porcentajeAsignadoRestante) {
        return 88 + 0.12 * porcentajeAsignadoRestante;
    }

    private String pacePara(TelemetrySnapshot muestra) {
        if (muestra.estadoVuelta() != LapStatus.VALID) return "BOX";
        if (muestra.evento().impacto().bandera() != TrackFlag.GREEN) return "CAUTION";
        if (modo == DrivingMode.AHORRO) return "SAVE";
        if (modo == DrivingMode.AGRESIVA || muestra.deltaSegundos() < -0.15) return "PUSH";
        if (muestra.velocidadRelativa() < 0.48) return "BUILD";
        return "NORMAL";
    }

    private String paceCompanero(TelemetrySnapshot muestra, double velocidadRelativa) {
        if (muestra.estadoVuelta() != LapStatus.VALID) return "BOX";
        if (muestra.evento().impacto().bandera() != TrackFlag.GREEN) return "CAUTION";
        if (velocidadRelativa > 0.82) return "PUSH";
        if (velocidadRelativa < 0.46) return "BUILD";
        return "NORMAL";
    }

    private String etiquetaPaceInicial() {
        return switch (modo) {
            case AGRESIVA -> "PUSH";
            case NORMAL -> "NORMAL";
            case AHORRO -> "SAVE";
        };
    }

    private String etiquetaCombustible(FuelStrategy estrategia) {
        return switch (estrategia) {
            case AGRESIVA -> "PUSH";
            case BALANCEADA -> "BALANCEADA";
            case AHORRO -> "AHORRO";
        };
    }

    private double limitar(double valor, double minimo, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
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
        btnFinalizar.disableProperty().unbind();
        btnFinalizar.setDisable(true);
        selectorDuracion.disableProperty().unbind();
        selectorDuracion.setDisable(false);
    }

    private record OpcionDuracion(Integer segundos, String etiqueta) {
        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
