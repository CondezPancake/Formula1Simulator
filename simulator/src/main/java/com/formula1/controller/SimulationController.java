package com.formula1.controller;

import com.formula1.model.AerodynamicLoad;
import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.FuelStrategy;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.SimulationSnapshot;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

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
        Task<QualifyingSession> tarea = sesiones.crearTarea(config,
                muestra -> Platform.runLater(() -> mostrarEvolucion(muestra)));

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
}
