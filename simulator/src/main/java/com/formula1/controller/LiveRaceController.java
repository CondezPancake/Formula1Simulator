package com.formula1.controller;

import com.formula1.model.EventOccurrence;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.service.CircuitService;
import com.formula1.service.QualifyingService;
import com.formula1.util.Async;
import com.formula1.util.FormatUtils;
import com.formula1.util.TeamColors;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Sesión de clasificación transcurriendo en pantalla.
 *
 * La sesión llega ya calculada y aquí se revela en el tiempo: los pilotos van
 * marcando su vuelta de uno en uno y la tabla se reordena, igual que una
 * clasificación real, donde la parrilla se va formando conforme cada coche
 * cruza la línea.
 *
 * Se reproduce con un {@link Timeline}, que corre en el hilo de JavaFX. Eso
 * hace que detenerla sea inmediato y sin sincronización, y evita ocupar
 * ninguno de los dos hilos del pool compartido.
 */
public class LiveRaceController {

    /** Tope de duración que se pidió para una sesión. */
    private static final double DURACION_MAXIMA_S = 30;

    @FXML private Label lblEvento;
    @FXML private Label lblCircuito;
    @FXML private Label lblReloj;
    @FXML private Label lblPiloto;
    @FXML private Label lblVelocidad;
    @FXML private Label lblTiempo;
    @FXML private Label lblDesgaste;
    @FXML private Label lblCompletados;
    @FXML private ProgressBar progresoSesion;
    @FXML private Button btnFinalizar;
    @FXML private VBox avisos;

    @FXML private TableView<LapResult> tabla;
    @FXML private TableColumn<LapResult, Number> colPosicion;
    @FXML private TableColumn<LapResult, String> colPiloto;
    @FXML private TableColumn<LapResult, String> colEquipo;
    @FXML private TableColumn<LapResult, String> colTiempo;
    @FXML private TableColumn<LapResult, String> colGap;
    @FXML private TableColumn<LapResult, String> colEstado;

    private final CircuitService circuitos;
    private final QualifyingService sesiones;

    private final ObservableList<LapResult> enPista = FXCollections.observableArrayList();
    private Timeline reproduccion;
    private QualifyingSession sesion;
    private List<LapResult> porLlegar = List.of();
    private List<TelemetrySnapshot> telemetria = List.of();
    private int revelados;

    public LiveRaceController() {
        this(new CircuitService(), new QualifyingService());
    }

    public LiveRaceController(CircuitService circuitos, QualifyingService sesiones) {
        this.circuitos = circuitos;
        this.sesiones = sesiones;
    }

    @FXML
    public void initialize() {
        colPosicion.setCellValueFactory(f -> new SimpleIntegerProperty(
                enPista.indexOf(f.getValue()) + 1));
        colPiloto.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPiloto()));
        colEquipo.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getEquipo()));
        colTiempo.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().isVueltaValida()
                        ? FormatUtils.formatLapTime(f.getValue().getTiempoSegundos()) : "—"));
        colGap.setCellValueFactory(f -> new SimpleStringProperty(gapDe(f.getValue())));
        colEstado.setCellValueFactory(f -> new SimpleStringProperty(
                f.getValue().getEstadoVuelta().getEtiqueta()));
        colTiempo.getStyleClass().add("mono-col");
        colGap.getStyleClass().add("mono-col");

        tabla.setItems(enPista);
        tabla.setPlaceholder(new Label("Los monoplazas están saliendo a pista…"));
        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(LapResult resultado, boolean vacia) {
                super.updateItem(resultado, vacia);
                if (vacia || resultado == null) {
                    setStyle("");
                    return;
                }
                setStyle("-fx-border-color: transparent transparent #17171B "
                        + TeamColors.hex(resultado.getEquipo()) + "; -fx-border-width: 0 0 1 3;");
            }
        });
    }

    /** Diferencia con quien va primero en ese momento, no con la pole final. */
    private String gapDe(LapResult resultado) {
        if (!resultado.isVueltaValida() || enPista.isEmpty()) {
            return "—";
        }
        LapResult lider = enPista.get(0);
        if (resultado == lider) {
            return "LÍDER";
        }
        return FormatUtils.formatGap(resultado.getTiempoSegundos() - lider.getTiempoSegundos());
    }

    /**
     * Empieza a reproducir una sesión ya calculada.
     *
     * @param sesion resultado completo que entrega la parrilla de salida
     */
    public void reproducir(QualifyingSession sesion) {
        this.sesion = sesion;
        detener();
        enPista.clear();
        avisos.getChildren().clear();
        revelados = 0;

        circuitos.porNombre(sesion.getCircuito()).ifPresent(c -> {
            lblEvento.setText(c.getNombre().toUpperCase(Locale.ROOT));
            lblCircuito.setText(c.getPais() == null ? "" : c.getPais().toUpperCase(Locale.ROOT));
        });

        // Se revelan en el orden en que marcaron, no en el de la parrilla
        // final: si no, la tabla nunca cambiaría de orden.
        porLlegar = new ArrayList<>(sesion.getResultados());
        porLlegar.sort(Comparator.comparingInt(LapResult::getPilotoId));
        telemetria = sesion.getEvolucionVuelta() == null ? List.of() : sesion.getEvolucionVuelta();

        lblCompletados.setText("0 / " + porLlegar.size());
        ShellController.estadoSesion(ShellController.Estado.EN_CURSO);

        arrancarReproduccion();
    }

    private void arrancarReproduccion() {
        int total = Math.max(porLlegar.size(), 1);
        Duration paso = Duration.seconds(DURACION_MAXIMA_S / total);

        reproduccion = new Timeline(new KeyFrame(paso, e -> revelarSiguiente()));
        reproduccion.setCycleCount(total);
        reproduccion.setOnFinished(e -> terminar());
        reproduccion.play();
    }

    private void revelarSiguiente() {
        if (revelados >= porLlegar.size()) {
            return;
        }
        LapResult resultado = porLlegar.get(revelados);
        revelados++;

        enPista.add(resultado);
        // Reordenar tras cada llegada es lo que hace que se vea la lucha por
        // la pole: quien acaba de marcar puede saltar al primer puesto.
        enPista.sort(Comparator
                .comparing(LapResult::isVueltaValida).reversed()
                .thenComparingDouble(LapResult::getTiempoSegundos));
        tabla.refresh();

        lblCompletados.setText(revelados + " / " + porLlegar.size());
        progresoSesion.setProgress(revelados / (double) porLlegar.size());
        lblReloj.setText(String.format(Locale.ROOT, "%.0f s",
                DURACION_MAXIMA_S * revelados / porLlegar.size()));

        mostrarTelemetria();
        anunciarEventos(resultado);
    }

    /**
     * La telemetría solo existe para el piloto elegido, así que se recorre a
     * la par que las llegadas para que las cifras acompañen al avance.
     */
    private void mostrarTelemetria() {
        if (telemetria.isEmpty()) {
            return;
        }
        int indice = Math.min(revelados - 1, telemetria.size() - 1);
        TelemetrySnapshot muestra = telemetria.get(indice);
        lblPiloto.setText(muestra.piloto());
        lblVelocidad.setText(String.format(Locale.ROOT, "%.0f km/h", muestra.velocidadKmh()));
        lblTiempo.setText(FormatUtils.formatLapTime(muestra.tiempoVueltaSegundos()));
        lblDesgaste.setText(String.format(Locale.ROOT, "%.1f %%",
                muestra.desgasteNeumaticosPorcentaje()));
        ShellController.segmento(muestra.segmento(), muestra.totalSegmentos());
        ShellController.clima(muestra.clima());
    }

    /** Los incidentes del piloto que acaba de marcar aparecen como avisos. */
    private void anunciarEventos(LapResult resultado) {
        for (EventOccurrence evento : resultado.getEventos()) {
            if (!evento.ocurrio()) {
                continue;
            }
            avisos.getChildren().add(0, aviso(resultado, evento));
        }
        // El panel no crece sin fin: solo interesan las últimas.
        while (avisos.getChildren().size() > 8) {
            avisos.getChildren().remove(avisos.getChildren().size() - 1);
        }
    }

    private VBox aviso(LapResult resultado, EventOccurrence evento) {
        Label titulo = new Label(evento.tipo().getEtiqueta());
        titulo.getStyleClass().add("live-feed-title");
        Label quien = new Label(resultado.getPiloto());
        quien.getStyleClass().add("live-feed-driver");
        quien.setStyle("-fx-text-fill: " + TeamColors.hex(resultado.getEquipo()) + ";");

        VBox tarjeta = new VBox(2, titulo, quien);
        tarjeta.getStyleClass().add("live-feed-item");
        return tarjeta;
    }

    @FXML
    private void onFinalizar() {
        terminar();
    }

    /**
     * Cierra la sesión y devuelve el control a la clasificación con el
     * resultado completo, tanto si se acabó sola como si se cortó antes.
     */
    private void terminar() {
        if (sesion == null) {
            return;
        }
        detener();
        QualifyingSession terminada = sesion;
        sesion = null;

        Async.ejecutar(() -> sesiones.guardar(terminada));
        ShellController.irACarrera();
        if (Navigator.ultimoControlador() instanceof SimulationController clasificacion) {
            clasificacion.mostrarSesion(terminada);
        }
    }

    /** Corta la reproducción; también al abandonar la pantalla. */
    void detener() {
        if (reproduccion != null) {
            reproduccion.stop();
            reproduccion = null;
        }
    }

    boolean enMarcha() {
        return reproduccion != null && reproduccion.getStatus() == Animation.Status.RUNNING;
    }
}
