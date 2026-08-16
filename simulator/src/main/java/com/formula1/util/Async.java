package com.formula1.util;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pool de hilos compartido para todo el trabajo en segundo plano.
 *
 * Tener uno solo (y que sus hilos sean demonio) evita que cada controlador
 * cree el suyo y que la aplicación se quede colgada al cerrarse.
 */
public final class Async {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, tarea -> {
        Thread hilo = new Thread(tarea, "f1-async");
        hilo.setDaemon(true);
        return hilo;
    });

    private Async() {
    }

    public static void ejecutar(Runnable trabajo) {
        POOL.execute(trabajo);
    }

    public static void ejecutar(Task<?> tarea) {
        POOL.execute(tarea);
    }

    public static void cerrar() {
        POOL.shutdownNow();
    }
}
