package com.formula1.controller;

import com.formula1.model.Driver;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.Vehicle;
import com.formula1.service.CircuitService;
import com.formula1.service.DriverService;
import com.formula1.service.QualifyingService;
import com.formula1.service.VehicleService;
import com.formula1.util.Async;
import com.formula1.util.ImageCrop;
import com.formula1.util.TeamColors;
import com.formula1.util.VehicleImages;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Antesala de la sesión: parrilla de salida y semáforo.
 *
 * Mientras se ve la secuencia de luces, la clasificación se está calculando
 * en segundo plano. Los dos procesos duran algo parecido, así que la espera
 * técnica queda escondida detrás de la puesta en escena en lugar de ser una
 * barra de progreso.
 *
 * Es una pantalla de paso: no está en el menú y solo se llega pulsando
 * «iniciar clasificación».
 */
public class StartGridController {

    private static final double ANCHO_COCHE = 340;
    private static final double ALTO_COCHE = 200;
    private static final double ANCHO_COCHE_PROPIO = 420;
    private static final double ALTO_COCHE_PROPIO = 250;

    @FXML private Label lblEvento;
    @FXML private Label lblCircuito;
    @FXML private Label lblFase;
    @FXML private Label lblEstado;
    @FXML private HBox parrilla;
    @FXML private HBox contenedorSemaforo;
    @FXML private Button btnCancelar;

    private final QualifyingService sesiones;
    private final CircuitService circuitos;
    private final VehicleService vehiculos;
    private final DriverService pilotos;

    private final StartLights semaforo = new StartLights();

    private QualifyingSession resultado;
    private boolean lucesApagadas;
    private boolean cancelada;

    public StartGridController() {
        this(new QualifyingService(), new CircuitService(), new VehicleService(), new DriverService());
    }

    public StartGridController(QualifyingService sesiones, CircuitService circuitos,
                               VehicleService vehiculos, DriverService pilotos) {
        this.sesiones = sesiones;
        this.circuitos = circuitos;
        this.vehiculos = vehiculos;
        this.pilotos = pilotos;
    }

    @FXML
    public void initialize() {
        contenedorSemaforo.getChildren().add(semaforo);
    }

    /**
     * Monta la escena y arranca a la vez el cálculo y las luces.
     *
     * @param config ajustes ya validados por la pantalla de clasificación
     */
    public void preparar(SimulationConfig config) {
        cancelada = false;
        lucesApagadas = false;
        resultado = null;

        circuitos.porNombre(config.getCircuito()).ifPresent(c -> {
            lblEvento.setText(c.getNombre().toUpperCase(Locale.ROOT));
            lblCircuito.setText("FORMACIÓN · " + (c.getPais() == null ? "" : c.getPais().toUpperCase(Locale.ROOT)));
        });
        lblEstado.setText("Los monoplazas se colocan en la parrilla…");
        montarParrilla(config);

        lanzarCalculo(config);
        semaforo.arrancar(this::alApagarseLasLuces);
    }

    /**
     * Tu monoplaza al centro y dos rivales a los lados, como los tres planos
     * del diseño.
     */
    private void montarParrilla(SimulationConfig config) {
        parrilla.getChildren().clear();
        Optional<Vehicle> propio = vehiculos.porModelo(config.getVehiculo());
        List<Vehicle> rivales = new ArrayList<>(vehiculos.listar());
        propio.ifPresent(rivales::remove);

        Vehicle izquierda = rivales.isEmpty() ? null : rivales.get(0);
        Vehicle derecha = rivales.size() < 2 ? null : rivales.get(1);

        if (izquierda != null) {
            parrilla.getChildren().add(plano(izquierda, null, false));
        }
        propio.ifPresent(v -> parrilla.getChildren().add(
                plano(v, pilotos.porId(config.getPilotoId() == null ? -1 : config.getPilotoId()).orElse(null), true)));
        if (derecha != null) {
            parrilla.getChildren().add(plano(derecha, null, false));
        }
    }

    /** Recuadro con la foto del monoplaza y quién lo conduce. */
    private VBox plano(Vehicle vehiculo, Driver piloto, boolean esPropio) {
        String color = TeamColors.hex(vehiculo.getEquipo());
        double ancho = esPropio ? ANCHO_COCHE_PROPIO : ANCHO_COCHE;
        double alto = esPropio ? ALTO_COCHE_PROPIO : ALTO_COCHE;

        StackPane foto = new StackPane();
        foto.getStyleClass().add("grid-car-photo");
        foto.setPrefSize(ancho, alto);
        foto.setMinSize(ancho, alto);
        foto.setMaxSize(ancho, alto);
        foto.setStyle("-fx-border-color: " + color + ";");

        List<String> vistas = VehicleImages.de(vehiculo.getModelo());
        if (!vistas.isEmpty()) {
            ImageCrop.desdeClasspath(vistas.get(0), ancho, alto, ImageCrop.CENTRADO)
                    .ifPresent(foto.getChildren()::add);
        }

        Label modelo = new Label(vehiculo.getModelo());
        modelo.getStyleClass().add("grid-car-model");
        StackPane.setAlignment(modelo, Pos.TOP_LEFT);
        StackPane.setMargin(modelo, new Insets(10));
        foto.getChildren().add(modelo);

        Label quien = new Label(piloto == null
                ? vehiculo.getEquipo()
                : "#" + piloto.getNumero() + "  " + piloto.getNombre());
        quien.getStyleClass().add(esPropio ? "grid-car-driver-own" : "grid-car-driver");
        quien.setStyle("-fx-text-fill: " + color + ";");

        VBox plano = new VBox(8, foto, quien);
        plano.setAlignment(Pos.CENTER);
        return plano;
    }

    /** El cálculo va por detrás de la animación, no al revés. */
    private void lanzarCalculo(SimulationConfig config) {
        Task<QualifyingSession> tarea = sesiones.crearTarea(config);
        tarea.setOnSucceeded(e -> {
            resultado = tarea.getValue();
            continuarSiTodoListo();
        });
        tarea.setOnFailed(e -> {
            semaforo.detener();
            Throwable causa = tarea.getException();
            Navigator.error("No se pudo iniciar la clasificación",
                    causa == null ? "Error desconocido" : String.valueOf(causa.getMessage()));
            volverAClasificacion();
        });
        Async.ejecutar(tarea);
    }

    private void alApagarseLasLuces() {
        lucesApagadas = true;
        lblFase.setText("● EN PISTA");
        lblFase.getStyleClass().remove("off");
        lblEstado.setText(resultado == null ? "Ultimando la salida…" : "");
        continuarSiTodoListo();
    }

    /**
     * La salida necesita las dos cosas: luces apagadas y sesión calculada.
     * Normalmente termina antes el cálculo, pero si se retrasara la pantalla
     * espera en vez de saltar a una vista vacía.
     */
    private void continuarSiTodoListo() {
        if (cancelada || !lucesApagadas || resultado == null) {
            return;
        }
        QualifyingSession sesion = resultado;
        Platform.runLater(() -> {
            Navigator.ir("carrera-vivo");
            if (Navigator.ultimoControlador() instanceof LiveRaceController vivo) {
                vivo.reproducir(sesion);
            }
        });
    }

    @FXML
    private void onCancelar() {
        cancelada = true;
        semaforo.detener();
        volverAClasificacion();
    }

    private void volverAClasificacion() {
        ShellController.estadoSesion(ShellController.Estado.REPOSO);
        ShellController.irACarrera();
    }
}
