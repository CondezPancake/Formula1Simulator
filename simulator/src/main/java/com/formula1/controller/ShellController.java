package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.model.TrackFlag;
import com.formula1.model.WeatherSnapshot;
import com.formula1.util.Async;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.application.Platform;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Marco de la aplicación: barra superior persistente, navegación de
 * secciones y contenido central.
 *
 * La barra superior reproduce la del diseño (evento, estado de sesión,
 * contador de segmento, clima, banderas y reloj). Como cualquier pantalla
 * puede necesitar actualizarla —la simulación sobre todo—, se expone un
 * puñado de métodos estáticos, igual que hace {@link Navigator} con la
 * navegación.
 */
public class ShellController {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static ShellController instancia;

    @FXML private StackPane contenido;
    @FXML private HBox navegacion;
    @FXML private Label lblEstado;
    @FXML private ProgressIndicator cargando;

    @FXML private Label lblEvento;
    @FXML private Label lblCircuitoCabecera;
    @FXML private Label lblEstadoSesion;
    @FXML private Label lblSegmentoActual;
    @FXML private Label lblSegmentoTotal;
    @FXML private Label lblIconoClima;
    @FXML private Label lblClimaTemp;
    @FXML private Label lblClimaEstado;
    @FXML private Label banderaVerde;
    @FXML private Label banderaAmarilla;
    @FXML private Label banderaRoja;
    @FXML private Label lblZonaHoraria;
    @FXML private Label lblReloj;

    @FXML private Button btnCarrera;
    @FXML private Button btnExplorar;
    @FXML private Button btnGestion;
    @FXML private Button btnConfig;

    @FXML
    public void initialize() {
        instancia = this;
        Navigator.registrar(contenido);
        navegacion.setDisable(true);
        cargando.setVisible(true);
        lblEstado.setText("Cargando datos…");
        arrancarReloj();

        Task<String> carga = new Task<>() {
            @Override
            protected String call() {
                return DataStore.getInstance().cargar();
            }
        };

        carga.setOnSucceeded(e -> {
            lblEstado.setText(carga.getValue() + "  ·  " + resumen());
            navegacion.setDisable(false);
            cargando.setVisible(false);
            onCarrera();
        });

        carga.setOnFailed(e -> {
            cargando.setVisible(false);
            lblEstado.setText("No se pudieron cargar los datos");
            Navigator.error("No se pudieron cargar los datos",
                    String.valueOf(carga.getException() != null ? carga.getException().getMessage() : ""));
        });

        Async.ejecutar(carga);
    }

    /** Reloj de la cabecera, con el huso horario real de la máquina. */
    private void arrancarReloj() {
        lblZonaHoraria.setText("UTC" + desplazamientoHorario());
        Timeline reloj = new Timeline(new KeyFrame(Duration.ZERO,
                e -> lblReloj.setText(LocalTime.now().format(RELOJ))),
                new KeyFrame(Duration.seconds(1)));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
    }

    private String desplazamientoHorario() {
        String id = ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now()).getId();
        return "Z".equals(id) ? "+0" : id.substring(0, 3).replace("+0", "+").replace("-0", "-");
    }

    private String resumen() {
        DataStore datos = DataStore.getInstance();
        return datos.pilotos().size() + " pilotos · "
                + datos.equipos().size() + " equipos · "
                + datos.vehiculos().size() + " vehículos · "
                + datos.circuitos().size() + " circuitos";
    }

    private void marcarActivo(Button activo) {
        for (Button boton : new Button[]{btnCarrera, btnExplorar, btnGestion, btnConfig}) {
            boton.getStyleClass().remove("active");
        }
        activo.getStyleClass().add("active");
    }

    @FXML
    private void onCarrera() {
        // La sesión ya trae su propia barra de sub-tabs (el TabPane de
        // simulation.fxml), así que es directamente la vista de la sección.
        Navigator.ir("simulation");
        marcarActivo(btnCarrera);
    }

    /** Navega a Carrera pasando por el shell para mantener sincronizado el menú activo. */
    public static void irACarrera() {
        if (instancia != null) {
            instancia.onCarrera();
        } else {
            Navigator.ir("simulation");
        }
    }

    @FXML
    private void onExplorar() {
        Navigator.ir("explorar");
        marcarActivo(btnExplorar);
    }

    @FXML
    private void onGestion() {
        Navigator.ir("gestion");
        marcarActivo(btnGestion);
    }

    @FXML
    private void onConfigHistorial() {
        Navigator.ir("config-historial");
        marcarActivo(btnConfig);
    }

    @FXML
    private void onSalir() {
        if (Navigator.confirmar("¿Quieres salir del simulador?")) {
            Platform.exit();
        }
    }

    // --- API para que la sesión alimente la cabecera ---------------------

    /** Título del evento: circuito y país, como en el diseño. */
    public static void evento(String circuito, String pais) {
        if (instancia == null) {
            return;
        }
        instancia.lblEvento.setText(circuito == null ? "SIMULADOR DE CLASIFICACIÓN"
                : circuito.toUpperCase(Locale.ROOT));
        instancia.lblCircuitoCabecera.setText(pais == null ? "" : pais.toUpperCase(Locale.ROOT));
    }

    /** Estados posibles: en curso, terminada o en reposo. */
    public static void estadoSesion(Estado estado) {
        if (instancia == null) {
            return;
        }
        Label badge = instancia.lblEstadoSesion;
        badge.getStyleClass().removeAll("off", "done");
        switch (estado) {
            case EN_CURSO -> badge.setText("● LIVE");
            case TERMINADA -> {
                badge.setText("● FINALIZADA");
                badge.getStyleClass().add("done");
            }
            case REPOSO -> {
                badge.setText("● LISTO");
                badge.getStyleClass().add("off");
                instancia.lblSegmentoActual.setText("—");
                bandera(null);
            }
        }
    }

    /** Contador de segmento de vuelta (el equivalente honesto al de vuelta). */
    public static void segmento(int actual, int total) {
        if (instancia == null) {
            return;
        }
        instancia.lblSegmentoActual.setText(String.valueOf(actual));
        instancia.lblSegmentoTotal.setText("/" + total);
    }

    public static void clima(WeatherSnapshot clima) {
        if (instancia == null || clima == null) {
            return;
        }
        instancia.lblClimaTemp.setText(String.format(Locale.ROOT, "%.0f°C · ASFALTO %.0f°C",
                clima.temperaturaC(), clima.temperaturaPistaC()));
        instancia.lblClimaEstado.setText(String.format(Locale.ROOT, "%s · HUMEDAD %.0f%%",
                clima.estado().getEtiqueta().toUpperCase(Locale.ROOT), clima.humedadPorcentaje()));
        // Solo se usan glifos con cobertura real en las fuentes del sistema:
        // los emojis de clima solo existen en Noto Color Emoji, que JavaFX no
        // dibuja. Los bloques de sombreado transmiten bien la intensidad.
        instancia.lblIconoClima.setText(switch (clima.estado()) {
            case SECO -> "☀";
            case NUBLADO -> "░";
            case LLUVIA_LIGERA -> "▒";
            case LLUVIA -> "▓";
            case LLUVIA_INTENSA -> "█";
        });
    }

    /** Enciende la bandera correspondiente; {@code null} las apaga todas. */
    public static void bandera(TrackFlag bandera) {
        if (instancia == null) {
            return;
        }
        instancia.banderaVerde.getStyleClass().removeAll("on-green");
        instancia.banderaAmarilla.getStyleClass().removeAll("on-yellow");
        instancia.banderaRoja.getStyleClass().removeAll("on-red");
        if (bandera == null) {
            return;
        }
        switch (bandera) {
            case GREEN -> instancia.banderaVerde.getStyleClass().add("on-green");
            case YELLOW, LOCAL_YELLOW -> instancia.banderaAmarilla.getStyleClass().add("on-yellow");
            case RED -> instancia.banderaRoja.getStyleClass().add("on-red");
        }
    }

    public enum Estado {
        REPOSO, EN_CURSO, TERMINADA
    }
}
