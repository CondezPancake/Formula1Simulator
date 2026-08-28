package com.formula1.controller;

import com.formula1.util.AudioManager;
import com.formula1.util.Imagenes;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Animación de apertura: logo de F1 sobre un campo de brasas en fondo negro,
 * con un botón "SALTAR" que aparece al segundo. Dura más de 5s si nadie la
 * omite, y termina siempre en el mismo callback esté omitida o completa.
 */
public final class IntroController {

    private static final Duration ENTRADA = Duration.millis(1200);
    private static final Duration ESPERA_SALTAR = Duration.seconds(1);
    private static final Duration HOLD = Duration.seconds(3.3);
    private static final Duration SALIDA = Duration.millis(500);

    private static final int PARTICULAS = 90;

    /** Ancho al que se pinta el logo; se decodifica al doble por nitidez. */
    private static final double ANCHO_LOGO = 420;

    private IntroController() {
    }

    /** Construye la escena de intro y devuelve su raíz. {@code alTerminar} se llama una única vez. */
    public static Node crear(Runnable alTerminar) {
        AtomicBoolean terminado = new AtomicBoolean(false);

        StackPane raiz = new StackPane();
        raiz.getStyleClass().add("intro-root");
        raiz.setStyle("-fx-background-color: -fx-bg-0;");
        raiz.setFocusTraversable(true);

        ImageView logo = new ImageView();
        Image imagen = cargarLogo();
        if (imagen != null) {
            logo.setImage(imagen);
        }
        logo.setPreserveRatio(true);
        logo.setFitWidth(ANCHO_LOGO);
        logo.setOpacity(0);
        logo.setScaleX(0.85);
        logo.setScaleY(0.85);

        Text saltar = new Text("SALTAR ▸");
        saltar.getStyleClass().add("intro-skip");
        saltar.setOpacity(0);
        saltar.setFont(Font.font("Titillium Web", 14));
        StackPane.setAlignment(saltar, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(saltar, new javafx.geometry.Insets(0, 28, 24, 0));

        raiz.getChildren().addAll(logo, saltar);

        // Va en el indice 0: las brasas quedan por detras del logo.
        AnimationTimer particulas = montarParticulas(raiz);
        Runnable terminarUnaVez = () -> {
            if (terminado.compareAndSet(false, true)) {
                particulas.stop();
                AudioManager.detenerMusica();
                alTerminar.run();
            }
        };

        // Stinger corto + tema de fondo sin loop, sincronizados con el arranque.
        AudioManager.reproducirSfx("/audio/sound-intro.mp3");
        AudioManager.reproducirMusica("/audio/intro-f1.mp3", false);

        ParallelTransition entradaLogo = new ParallelTransition(
                fade(logo, 0, 1, ENTRADA),
                escalar(logo, 0.85, 1.0, ENTRADA));

        FadeTransition mostrarSaltar = fade(saltar, 0, 1, Duration.millis(400));
        mostrarSaltar.setDelay(ESPERA_SALTAR);

        PauseTransition hold = new PauseTransition(HOLD);

        FadeTransition desvanecerTodo = fade(raiz, 1, 0, SALIDA);

        SequentialTransition secuencia = new SequentialTransition(
                new PauseTransition(Duration.millis(200)),
                new ParallelTransition(entradaLogo, mostrarSaltar),
                hold,
                desvanecerTodo);
        secuencia.setOnFinished(e -> terminarUnaVez.run());

        raiz.setOnMouseClicked(e -> saltarIntro(secuencia, raiz, terminarUnaVez));
        raiz.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                saltarIntro(secuencia, raiz, terminarUnaVez);
            }
        });
        raiz.sceneProperty().addListener((obs, antigua, nueva) -> {
            if (nueva != null) {
                raiz.requestFocus();
            } else {
                particulas.stop();
            }
        });

        secuencia.play();

        return raiz;
    }

    /**
     * Campo de brasas que suben por detrás del logo.
     *
     * La estela se consigue velando el lienzo con negro semitransparente en
     * vez de borrarlo: sale gratis y da las chispas de velocidad.
     */
    private static AnimationTimer montarParticulas(StackPane raiz) {
        Canvas lienzo = new Canvas();
        lienzo.widthProperty().bind(raiz.widthProperty());
        lienzo.heightProperty().bind(raiz.heightProperty());
        lienzo.setMouseTransparent(true);   // el clic debe seguir saltando la intro
        raiz.getChildren().add(0, lienzo);

        GraphicsContext g = lienzo.getGraphicsContext2D();
        Random azar = new Random();
        double[] x = new double[PARTICULAS];
        double[] y = new double[PARTICULAS];
        double[] vx = new double[PARTICULAS];
        double[] vy = new double[PARTICULAS];
        double[] radio = new double[PARTICULAS];
        double[] alfa = new double[PARTICULAS];
        double[] fase = new double[PARTICULAS];
        Color[] tono = new Color[PARTICULAS];

        return new AnimationTimer() {
            private boolean sembrado = false;

            @Override
            public void handle(long ahora) {
                double w = lienzo.getWidth();
                double h = lienzo.getHeight();
                if (w <= 0 || h <= 0) {
                    return;
                }
                if (!sembrado) {
                    for (int i = 0; i < PARTICULAS; i++) {
                        sembrar(i, w, h, true);
                    }
                    sembrado = true;
                }

                g.setFill(Color.rgb(10, 10, 10, 0.22));
                g.fillRect(0, 0, w, h);

                for (int i = 0; i < PARTICULAS; i++) {
                    fase[i] += 0.017;
                    x[i] += vx[i] + Math.sin(fase[i]) * 0.28;
                    y[i] += vy[i];
                    if (y[i] < -6 || x[i] < -6 || x[i] > w + 6) {
                        sembrar(i, w, h, false);
                    }
                    g.setGlobalAlpha(alfa[i]);
                    g.setFill(tono[i]);
                    g.fillOval(x[i] - radio[i], y[i] - radio[i], radio[i] * 2, radio[i] * 2);
                }
                g.setGlobalAlpha(1);
            }

            private void sembrar(int i, double w, double h, boolean inicial) {
                x[i] = azar.nextDouble() * w;
                y[i] = inicial ? azar.nextDouble() * h : h + azar.nextDouble() * 40;
                vx[i] = (azar.nextDouble() - 0.35) * 0.45;
                vy[i] = -(0.25 + azar.nextDouble() * 0.75);
                radio[i] = 0.8 + azar.nextDouble() * 1.9;
                alfa[i] = 0.10 + azar.nextDouble() * 0.45;
                fase[i] = azar.nextDouble() * Math.PI * 2;
                double dado = azar.nextDouble();
                tono[i] = dado < 0.62 ? Color.web("#E10600")
                        : dado < 0.80 ? Color.web("#FF3B2F")
                        : Color.WHITE;
            }
        };
    }

    private static void saltarIntro(SequentialTransition secuencia, StackPane raiz, Runnable terminarUnaVez) {
        secuencia.stop();
        FadeTransition salidaRapida = fade(raiz, raiz.getOpacity(), 0, Duration.millis(250));
        salidaRapida.setOnFinished(e -> terminarUnaVez.run());
        salidaRapida.play();
    }

    private static Image cargarLogo() {
        // LogoF1.png son 920x800 y aquí se pinta a 420 de ancho. Pasa por
        // Imagenes para decodificarlo a medida y compartirlo con el menú, que
        // hasta ahora lo abría por segunda vez a resolución nativa.
        return Imagenes.cargar("/images/LogoF1.png", ANCHO_LOGO * 2, 0);
    }

    private static FadeTransition fade(Node nodo, double desde, double hasta, Duration duracion) {
        FadeTransition transicion = new FadeTransition(duracion, nodo);
        transicion.setFromValue(desde);
        transicion.setToValue(hasta);
        return transicion;
    }

    private static ScaleTransition escalar(Node nodo, double desde, double hasta, Duration duracion) {
        ScaleTransition transicion = new ScaleTransition(duracion, nodo);
        transicion.setFromX(desde);
        transicion.setFromY(desde);
        transicion.setToX(hasta);
        transicion.setToY(hasta);
        transicion.setInterpolator(Interpolator.EASE_BOTH);
        return transicion;
    }
}
