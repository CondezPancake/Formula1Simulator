package com.formula1.util;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pool de hilos compartido para todo el trabajo en segundo plano.
 *
 * Tener uno solo (y que sus hilos sean demonio) evita que cada controlador
 * cree el suyo y que la aplicación se quede colgada al cerrarse.
 */
public final class Async {

    /**
     * Con dos hilos el pool se quedaba sin sitio: una sesión de simulación
     * ocupa uno de ellos de principio a fin y el guardado de la sesión pide
     * otro, así que cualquier otra tarea —cargar datos, abrir imágenes— se
     * quedaba esperando a que terminara la carrera.
     */
    private static final int HILOS = Math.max(4, Runtime.getRuntime().availableProcessors() - 1);

    private static final AtomicInteger CONTADOR = new AtomicInteger();

    private static final ExecutorService POOL = Executors.newFixedThreadPool(HILOS, tarea -> {
        // Numerados: antes los dos hilos compartían nombre y no se distinguían
        // en un volcado de pila.
        Thread hilo = new Thread(tarea, "f1-async-" + CONTADOR.incrementAndGet());
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
