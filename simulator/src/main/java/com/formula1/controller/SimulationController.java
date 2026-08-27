package com.formula1.controller;

import com.formula1.model.AerodynamicLoad;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.EventOccurrence;
import com.formula1.model.FuelStrategy;
import com.formula1.model.LapResult;
import com.formula1.model.LapStatus;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.SimulationSnapshot;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.TirePressure;
import com.formula1.model.TrackEvolutionSnapshot;
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
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    @FXML private Tab tabTelemetria;
    @FXML private TableView<LapResult> tablaDashboard;
    @FXML private TableColumn<LapResult, Number> colDashboardPosicion;
    @FXML private TableColumn<LapResult, String> colDashboardPiloto;
    @FXML private TableColumn<LapResult, String> colDashboardGap;
    @FXML private TableColumn<LapResult, String> colDashboardTiempo;
    @FXML private Label lblMapaTitulo;
    @FXML private Label lblMapaPista;
    @FXML private Label lblMapaGrip;
    @FXML private ImageView imagenMapaCircuito;
    @FXML private Label lblDashboardEvento;
    @FXML private Label lblDashboardMensaje;
    @FXML private Label lblDashboardCombustible;
    @FXML private Label lblDashboardDesgaste;
    @FXML private LineChart<Number, Number> graficaDesgasteDashboard;
    @FXML private LineChart<Number, Number> graficaCombustibleDashboard;
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
    @FXML private SectorComparisonController comparacionSectoresController;
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
    private double velocidadMaximaAlcanzada;

    // Puesta a punto elegida en CONFIG. & HISTORIAL; aquí solo se consulta.
    private DrivingMode modo = DrivingMode.NORMAL;
    private AerodynamicLoad aero = AerodynamicLoad.MEDIA;
    private TirePressure presion = TirePressure.ESTANDAR;
    private FuelStrategy combustible = FuelStrategy.BALANCEADA;
    private long versionConfiguracionAplicada = -1;
    private double consumoVueltaAcumulado;
    private double consumoVueltaTotal;
    private double ersPiloto = 100;
    private double ersCompanero = 100;
    private final XYChart.Series<Number, Number> serieDesgasteDashboard = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> serieCombustibleDashboard = new XYChart.Series<>();

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

        // El vehículo delimita los pilotos válidos y evita combinaciones de
        // escuderías distintas antes de que lleguen a la capa de servicio.
        selectorVehiculo.valueProperty().addListener((obs, anterior, actual) -> {
            cargarPilotosDelVehiculo(actual);
            actualizarTarjetasPilotos();
        });

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

        // Timing tower del nuevo Dashboard. Es una vista resumida del mismo
        // ObservableList; la clasificación original conserva todas sus columnas.
        colDashboardPosicion.setCellValueFactory(f ->
                new SimpleIntegerProperty(f.getValue().getPosicion()));
        colDashboardPiloto.setCellValueFactory(f ->
                new SimpleStringProperty(codigoPiloto(f.getValue())));
        colDashboardGap.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().isVueltaValida()
                        ? FormatUtils.formatGap(f.getValue().getGap()) : "—"));
        colDashboardTiempo.setCellValueFactory(f -> new SimpleStringProperty(
                FormatUtils.formatLapResult(f.getValue())));
        colDashboardGap.getStyleClass().add("mono-col");
        colDashboardTiempo.getStyleClass().add("mono-col");

        // La pole se resalta con la clase .pole-row de la hoja de estilos.
        // Cada fila lleva a la izquierda la franja del color de su escudería,
        // igual que la tabla de tiempos del diseño.
        tabla.setRowFactory(t -> {
            TableRow<LapResult> fila = filaConColorDeEquipo();
            // Doble clic sobre un piloto abre su ficha, como en cualquier
            // tabla de resultados: la fila es el acceso natural al detalle.
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty() && fila.getItem() != null) {
                    ExploreDriversController.abrirFicha(fila.getItem().getPilotoId());
                }
            });
            return fila;
        });
        tablaDashboard.setRowFactory(t -> {
            TableRow<LapResult> fila = filaConColorDeEquipo();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty() && fila.getItem() != null) {
                    ExploreDriversController.abrirFicha(fila.getItem().getPilotoId());
                }
            });
            return fila;
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

        selectorCircuito.valueProperty().addListener((o, a, b) -> actualizarMapaCircuito(b));
        selectorPiloto.valueProperty().addListener((o, a, b) -> actualizarTarjetasPilotos());
        graficaDesgasteDashboard.getData().add(serieDesgasteDashboard);
        graficaCombustibleDashboard.getData().add(serieCombustibleDashboard);

        precargarUltimaConfiguracion();
        actualizarMapaCircuito(selectorCircuito.getValue());
        actualizarTarjetasPilotos();
        reiniciarGraficasDashboard();
    }

    private String codigoPiloto(LapResult resultado) {
        return pilotos.porId(resultado.getPilotoId())
                .map(Driver::getCodigo)
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .orElse(resultado.getPiloto());
    }

    private void actualizarMapaCircuito(String nombre) {
        if (nombre == null) {
            imagenMapaCircuito.setImage(null);
            lblMapaTitulo.setText("MAPA DEL CIRCUITO");
            return;
        }
        circuitos.porNombre(nombre).ifPresent(circuito -> {
            lblMapaTitulo.setText(circuito.getNombre().toUpperCase() + " · MAPA");
            lblMapaPista.setText("Pista: " + circuito.getLongitudKm() + " km");
            imagenMapaCircuito.setImage(cargarImagenCircuito(circuito));
        });
    }

    private Image cargarImagenCircuito(Circuit circuito) {
        if (circuito.getImagen() == null || circuito.getImagen().isBlank()) {
            return null;
        }
        var recurso = getClass().getResource(circuito.getImagen());
        return recurso == null ? null : new Image(recurso.toExternalForm());
    }

    /**
     * Vuelca una sesión ya calculada en todas las pestañas.
     *
     * Está separado del arranque porque la sesión no siempre llega de la
     * tarea que lanza esta pantalla: cuando se disputa desde la vista en vivo
     * es esa la que la trae ya terminada.
     */
    public void mostrarSesion(QualifyingSession sesion) {
        if (sesion == null) {
            return;
        }
        lblClima.setText(resumenClimatico(sesion));
        ObservableList<LapResult> resultados =
                FXCollections.observableArrayList(sesion.getResultados());
        tabla.setItems(resultados);
        tablaDashboard.setItems(resultados);
        tablaEventos.setItems(FXCollections.observableArrayList(sesion.getEventos()));
        comparacionSectoresController.cargar(sesion.getResultados());
        mostrarPistaDeSesion(sesion);
        LapResult pole = sesion.getPole();
        lblEstado.setText(pole == null ? "Sesión sin resultados"
                : "Pole: " + pole.getPiloto() + " — " + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
        lblDashboardMensaje.setText(pole == null ? "Sesión terminada sin vueltas válidas"
                : "Pole para " + pole.getPiloto() + " con "
                        + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
        lblDashboardEvento.setText(sesion.getEventos().isEmpty()
                ? "SIN EVENTOS" : sesion.getEventos().size() + " EVENTOS");
        ShellController.estadoSesion(ShellController.Estado.TERMINADA);
        panelResultados.getSelectionModel().select(tabDashboard);
    }

    /**
     * Fila con la franja del color de su escudería a la izquierda y la pole
     * destacada, igual que la tabla de tiempos del diseño.
     */
    private TableRow<LapResult> filaConColorDeEquipo() {
        return new TableRow<>() {
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
        };
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
            versionConfiguracionAplicada = com.formula1.data.DataStore.getInstance().versionConfiguracion();
            return;
        }
        precargarConfiguracion(ultima);
        versionConfiguracionAplicada = com.formula1.data.DataStore.getInstance().versionConfiguracion();
        lblEstado.setText("Configuración recuperada");
    }

    /** Resume la puesta a punto vigente, que se ajusta en la otra pantalla. */
    private void mostrarPuestaAPunto() {
        lblPuestaAPunto.setText(modo.getEtiqueta() + " · " + aero.getEtiqueta()
                + " · " + presion.getEtiqueta() + " · " + combustible.getEtiqueta());
        if (lblPaceUno != null) {
            lblPaceUno.setText(etiquetaPaceInicial());
            lblFuelModeUno.setText(etiquetaCombustible(combustible));
        }
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
        reiniciarTelemetria();
        comparacionSectoresController.reiniciar();
        panelResultados.getSelectionModel().select(tabDashboard);

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
                    mostrarClimaResumen(muestra.clima());
                }),
                muestra -> Platform.runLater(() -> mostrarEvolucionPista(muestra)));

        // Enlazar en vez de asignar: el Task publica sus cambios en el hilo
        // de JavaFX, así que la interfaz se actualiza sola y sin bloquearse.
        progreso.progressProperty().bind(tarea.progressProperty());
        lblEstado.textProperty().bind(tarea.messageProperty());
        btnSimular.disableProperty().bind(tarea.runningProperty());
        tabla.getItems().clear();
        tablaDashboard.getItems().clear();
        tablaEventos.getItems().clear();
        lblClima.setText("");

        tarea.setOnSucceeded(e -> {
            desenlazar();
            mostrarSesion(tarea.getValue());
            // Guardar en segundo plano: la parrilla ya está en pantalla.
            Async.ejecutar(() -> sesiones.guardar(tarea.getValue()));
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
        consumoVueltaAcumulado = 0;
        consumoVueltaTotal = 0;
        lblDashboardMensaje.setText("Preparando datos de la vuelta seleccionada");
        lblDashboardEvento.setText("SIN EVENTOS");
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
        reiniciarGraficasDashboard();
    }

    /** Representa una lectura ya calculada; no ejecuta lógica del motor en JavaFX. */
    private void mostrarTelemetria(TelemetrySnapshot muestra) {
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

        lblDashboardCombustible.setText(String.format("%.0f %%",
                muestra.combustibleRestantePorcentaje()));
        double vidaNeumatico = Math.max(0, 100 - muestra.desgasteNeumaticosPorcentaje());
        lblDashboardDesgaste.setText(String.format("%.0f %%", vidaNeumatico));
        actualizarGraficasDashboard(muestra.segmento(), vidaNeumatico,
                muestra.combustibleRestantePorcentaje());

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
        if (config.getCombustible() != null) combustible = config.getCombustible();
        if (!lblEstado.textProperty().isBound()) {
            lblEstado.setText("Configuración preparada para la próxima sesión");
        }
        mostrarPuestaAPunto();
    }

    /** Aplica un guardado nuevo sin tocar Carrera cuando no hay cambios pendientes. */
    public void aplicarConfiguracionGuardadaPendiente() {
        com.formula1.data.DataStore datos = com.formula1.data.DataStore.getInstance();
        long version = datos.versionConfiguracion();
        if (version == versionConfiguracionAplicada) {
            return;
        }
        precargarConfiguracion(datos.configuracionActual());
        versionConfiguracionAplicada = version;
    }

    private void actualizarVelocidadMaxima(double velocidadActual) {
        velocidadMaximaAlcanzada = Math.max(velocidadMaximaAlcanzada, velocidadActual);
        String maxima = String.format("Máxima alcanzada: %.0f km/h", velocidadMaximaAlcanzada);
        lblVelocidadMaximaTelemetria.setText(maxima);
    }

    private void reiniciarGraficasDashboard() {
        serieDesgasteDashboard.getData().clear();
        serieCombustibleDashboard.getData().clear();
        serieDesgasteDashboard.getData().add(new XYChart.Data<>(0, 100));
        serieCombustibleDashboard.getData().add(new XYChart.Data<>(0, 100));
        lblDashboardDesgaste.setText("100 %");
        lblDashboardCombustible.setText("100 %");
    }

    private void actualizarGraficasDashboard(int segmento, double vidaNeumatico,
                                              double combustibleRestante) {
        serieDesgasteDashboard.getData().add(
                new XYChart.Data<>(segmento, vidaNeumatico));
        serieCombustibleDashboard.getData().add(
                new XYChart.Data<>(segmento, combustibleRestante));
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
    }
}
