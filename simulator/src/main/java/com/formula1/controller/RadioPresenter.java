package com.formula1.controller;

import com.formula1.model.RadioMessage;
import com.formula1.util.AudioManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * Pone en pantalla la radio del equipo.
 *
 * <p>Dos salidas para el mismo mensaje: el <b>hilo lateral</b>, que conserva la
 * conversación entera, y el <b>rótulo sobre el trazado</b>, que solo aparece
 * para lo que de verdad interrumpe, como en la retransmisión.
 *
 * <p>La cola con ritmo no es un adorno. El motor no emite las incidencias
 * goteando: las suelta en tres ráfagas, al cambiar de sector, así que sin
 * regular la salida la radio escupiría diez frases de golpe y no se leería
 * ninguna. Se sirve una cada {@link #RITMO}, que es más o menos lo que tarda
 * una frase real de muro de boxes.
 */
final class RadioPresenter {

    private static final Duration RITMO = Duration.millis(900);
    private static final Duration ENTRADA = Duration.millis(200);
    private static final Duration PERMANENCIA = Duration.seconds(3.5);
    private static final Duration SALIDA = Duration.millis(250);

    /** El hilo es una vista, no un registro: la sesión guarda los eventos. */
    private static final int MAX_MENSAJES = 20;

    /** El mismo golpe del menú, atenuado. No hace falta añadir audio nuevo. */
    private static final String AVISO = "/audio/sound2.mp3";
    private static final double VOLUMEN_AVISO = 0.35;

    private final VBox hilo;
    private final Label cabeceraPiloto;
    private final Region barraEquipo;
    private final Node rotulo;
    private final Label rotuloPiloto;
    private final Label rotuloTexto;

    private final Deque<RadioMessage> cola = new ArrayDeque<>();
    private PauseTransition ritmo;
    private PauseTransition permanencia;

    private String codigoPiloto = "—";
    private String colorEquipo = "#E10600";

    RadioPresenter(VBox hilo, Label cabeceraPiloto, Region barraEquipo,
                   Node rotulo, Label rotuloPiloto, Label rotuloTexto) {
        this.hilo = hilo;
        this.cabeceraPiloto = cabeceraPiloto;
        this.barraEquipo = barraEquipo;
        this.rotulo = rotulo;
        this.rotuloPiloto = rotuloPiloto;
        this.rotuloTexto = rotuloTexto;
    }

    /** Vacía la conversación y corta lo que estuviera sonando. */
    void reiniciar() {
        cola.clear();
        if (ritmo != null) {
            ritmo.stop();
        }
        hilo.getChildren().clear();
        ocultarRotulo();
    }

    /** Dice a quién se está escuchando; tiñe la cabecera con su escudería. */
    void seguirA(String codigo, String color) {
        this.codigoPiloto = codigo == null || codigo.isBlank() ? "—" : codigo;
        this.colorEquipo = color == null ? "#E10600" : color;
        cabeceraPiloto.setText(this.codigoPiloto);
        barraEquipo.setStyle("-fx-background-color: " + this.colorEquipo + ";");
    }

    void encolar(Collection<RadioMessage> mensajes) {
        if (mensajes == null || mensajes.isEmpty()) {
            return;
        }
        cola.addAll(mensajes);
        arrancarRitmo();
    }

    void encolar(RadioMessage mensaje) {
        if (mensaje == null) {
            return;
        }
        cola.add(mensaje);
        arrancarRitmo();
    }

    private void arrancarRitmo() {
        if (ritmo != null && ritmo.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            return;
        }
        soltarSiguiente();
    }

    private void soltarSiguiente() {
        RadioMessage mensaje = cola.poll();
        if (mensaje == null) {
            return;
        }
        pintar(mensaje);
        if (mensaje.interrumpe()) {
            mostrarRotulo(mensaje);
            AudioManager.reproducirSfx(AVISO, VOLUMEN_AVISO);
        }
        ritmo = new PauseTransition(RITMO);
        ritmo.setOnFinished(e -> soltarSiguiente());
        ritmo.play();
    }

    /** Una línea de la conversación: ingeniero a la izquierda, piloto a la derecha. */
    private void pintar(RadioMessage mensaje) {
        boolean esIngeniero = mensaje.emisor() == RadioMessage.Emisor.INGENIERO;

        Label quien = new Label(esIngeniero ? "INGENIERO" : codigoPiloto);
        quien.getStyleClass().add("radio-quien");
        Label segmento = new Label("S" + mensaje.segmento());
        segmento.getStyleClass().add("radio-segmento");
        Region separador = new Region();
        HBox.setHgrow(separador, Priority.ALWAYS);
        HBox encabezado = new HBox(6, onda(esIngeniero), quien, separador, segmento);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        Label texto = new Label(mensaje.texto());
        texto.getStyleClass().add("radio-texto");
        texto.setWrapText(true);

        VBox burbuja = new VBox(2, encabezado, texto);
        burbuja.getStyleClass().addAll("radio-burbuja",
                esIngeniero ? "ingeniero" : "piloto");
        if (esIngeniero) {
            // El filete lleva el color del equipo: identifica el canal de un vistazo.
            burbuja.setStyle("-fx-border-color: transparent transparent transparent "
                    + colorEquipo + ";");
        }
        if (mensaje.prioridad() == RadioMessage.Prioridad.CRITICA) {
            burbuja.getStyleClass().add("critica");
        }

        hilo.getChildren().add(burbuja);
        while (hilo.getChildren().size() > MAX_MENSAJES) {
            hilo.getChildren().remove(0);
        }

        FadeTransition entrada = new FadeTransition(ENTRADA, burbuja);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.setInterpolator(Interpolator.EASE_BOTH);
        entrada.play();
    }

    /** Tres barras que sugieren nivel de audio; el canal abierto se ve, no se oye. */
    private HBox onda(boolean esIngeniero) {
        HBox onda = new HBox(1);
        onda.setAlignment(Pos.CENTER);
        onda.getStyleClass().add("radio-onda");
        double[] alturas = {5, 9, 6};
        for (double alto : alturas) {
            Region barra = new Region();
            barra.getStyleClass().add("radio-onda-barra");
            barra.setMinSize(2, alto);
            barra.setPrefSize(2, alto);
            barra.setMaxSize(2, alto);
            if (esIngeniero) {
                barra.setStyle("-fx-background-color: " + colorEquipo + ";");
            }
            onda.getChildren().add(barra);
        }
        return onda;
    }

    /** Banda inferior sobre el trazado, al estilo de la señal de televisión. */
    private void mostrarRotulo(RadioMessage mensaje) {
        if (permanencia != null) {
            permanencia.stop();
        }
        rotuloPiloto.setText(codigoPiloto);
        rotuloPiloto.setStyle("-fx-text-fill: " + colorEquipo + ";");
        rotuloTexto.setText(mensaje.texto());
        rotulo.setVisible(true);
        rotulo.setManaged(true);
        rotulo.setOpacity(0);

        FadeTransition entrada = new FadeTransition(ENTRADA, rotulo);
        entrada.setToValue(1);
        entrada.setInterpolator(Interpolator.EASE_BOTH);
        entrada.play();

        permanencia = new PauseTransition(PERMANENCIA);
        permanencia.setOnFinished(e -> ocultarRotulo());
        permanencia.play();
    }

    private void ocultarRotulo() {
        if (!rotulo.isVisible()) {
            return;
        }
        FadeTransition salida = new FadeTransition(SALIDA, rotulo);
        salida.setToValue(0);
        salida.setOnFinished(e -> {
            rotulo.setVisible(false);
            rotulo.setManaged(false);
        });
        salida.play();
    }
}
