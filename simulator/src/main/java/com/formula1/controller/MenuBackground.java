package com.formula1.controller;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Capa de fondo animado del menú principal: brasas y polvo moviéndose
 * lentamente sobre un lienzo oscuro, detrás de toda la interfaz.
 *
 * <p>Se declara en {@code menu.fxml} como cualquier otro nodo —no lleva
 * {@code @FXML initialize()}: es un control, no una pantalla— y
 * {@code FXMLLoader} la instancia con este constructor sin argumentos.
 */
public class MenuBackground extends Region {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final AnimationTimer timer;
    private double timeAccumulator = 0;

    public MenuBackground() {
        setFocusTraversable(false);
        setCache(true);
        setMouseTransparent(true);

        canvas = new Canvas(1600, 940);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                render();
            }
        };
        timer.start();
    }

    private void update(long now) {
        // Acumulador de tiempo para el movimiento suave, en segundos.
        timeAccumulator += now / 1_000_000_000.0;
    }

    private void render() {
        // Fondo casi negro, como el paddock de noche.
        gc.setFill(Color.color(0.015, 0.015, 0.03));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // --- Capa 1: brasas cálidas moviéndose lentamente ---
        gc.save();
        double shiftX = Math.sin(timeAccumulator * 0.2) * 2.0;
        gc.translate(shiftX, 0);

        gc.setFill(Color.color(0.6, 0.3, 0.1));
        for (int i = 0; i < 15; i++) {
            double x = Math.random() * canvas.getWidth();
            double y = Math.random() * canvas.getHeight();
            double size = Math.random() * 4 + 1;
            double opacity = Math.random() * 0.4 + 0.3;

            gc.setGlobalAlpha(opacity);
            gc.fillOval(x, y, size, size);
        }
        gc.restore();

        // --- Capa 2: chispas ocasionales ---
        if (Math.random() < 0.02) {
            double x = Math.random() * canvas.getWidth();
            double y = Math.random() * canvas.getHeight() * 0.6;
            double size = Math.random() * 6 + 2;

            gc.setGlobalAlpha(0.9);
            gc.setFill(Color.WHITE);
            gc.fillOval(x - size / 2, y - size / 2, size, size);
            gc.setGlobalAlpha(1.0);
        }

        // --- Capa 3: polvo giratorio sutil ---
        gc.save();
        gc.rotate(timeAccumulator * 15);
        gc.setFill(Color.color(0.1, 0.1, 0.2, 0.03));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.restore();

        gc.setGlobalAlpha(1.0);
    }

    @Override
    protected void layoutChildren() {
        // Ajusta el lienzo al tamaño real del panel.
        double w = getWidth();
        double h = getHeight();
        if (w > 0 && h > 0 && canvas.getWidth() != w) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }
        super.layoutChildren();
    }
}
