package com.formula1.controller;

import com.formula1.util.StartLightsSound;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Semáforo de salida de Fórmula 1.
 *
 * Reproduce la secuencia real: las cinco luces se encienden de una en una con
 * un segundo entre ellas y luego se apagan todas a la vez, que es el momento
 * de la salida. No hay cuenta atrás visible a propósito —en F1 tampoco la
 * hay— y la tensión está justo en no saber cuándo se apagan.
 */
final class StartLights extends HBox {

    private static final int LUCES = 5;
    private static final double RADIO = 26;
    private static final Duration INTERVALO = Duration.seconds(1);

    /** Espera antes de apagar; en la realidad es variable, aquí fija y breve. */
    private static final Duration ESPERA_APAGADO = Duration.seconds(1.2);

    private static final Color APAGADA = Color.web("#241012");
    private static final Color ENCENDIDA = Color.web("#FF1E0F");

    private final List<Circle> bombillas = new ArrayList<>();
    private Timeline secuencia;

    StartLights() {
        super(18);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(22, 34, 22, 34));
        getStyleClass().add("start-lights");

        for (int i = 0; i < LUCES; i++) {
            Circle bombilla = new Circle(RADIO, APAGADA);
            bombilla.setStroke(Color.web("#3A1418"));
            bombilla.setStrokeWidth(2);
            bombillas.add(bombilla);
            getChildren().add(bombilla);
        }
    }

    /**
     * Lanza la secuencia completa.
     *
     * @param alApagarse se ejecuta en el hilo de JavaFX cuando se apagan las
     *                   luces, es decir, cuando empieza la sesión
     */
    void arrancar(Runnable alApagarse) {
        detener();
        apagarTodas();

        secuencia = new Timeline();
        for (int i = 0; i < LUCES; i++) {
            int indice = i;
            secuencia.getKeyFrames().add(new KeyFrame(INTERVALO.multiply(i + 1D), e -> {
                encender(indice);
                StartLightsSound.luz();
            }));
        }
        secuencia.getKeyFrames().add(new KeyFrame(
                INTERVALO.multiply(LUCES).add(ESPERA_APAGADO), e -> {
                    apagarTodas();
                    StartLightsSound.salida();
                    alApagarse.run();
                }));
        secuencia.play();
    }

    /** Corta la secuencia; se llama al salir de la pantalla. */
    void detener() {
        if (secuencia != null) {
            secuencia.stop();
            secuencia = null;
        }
    }

    private void encender(int indice) {
        Circle bombilla = bombillas.get(indice);
        bombilla.setFill(ENCENDIDA);
        bombilla.setStroke(Color.web("#FF6B60"));
        // El resplandor es lo que hace que se lea como una luz y no como un
        // circulo rojo.
        bombilla.setStyle("-fx-effect: dropshadow(gaussian, #FF1E0F, 26, 0.55, 0, 0);");
    }

    private void apagarTodas() {
        for (Circle bombilla : bombillas) {
            bombilla.setFill(APAGADA);
            bombilla.setStroke(Color.web("#3A1418"));
            bombilla.setStyle("");
        }
    }
}
