package com.formula1.util;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Punto único de carga de imágenes, con caché y tamaño de decodificación.
 *
 * Los dos problemas que resuelve venían de construir {@code new Image(...)}
 * suelto por los controladores:
 *
 * <ul>
 *   <li><b>Sin tamaño</b>: JavaFX decodifica a resolución nativa. En este
 *       proyecto eso significaba abrir una foto de 3444x4429 —unos 61 MB ya
 *       descomprimida— para pintarla en una caja de 210x240, o una bandera de
 *       1000x563 para dibujarla a 34 px, veinte veces por visita al catálogo.</li>
 *   <li><b>Sin caché</b>: cada reconstrucción de una rejilla volvía a
 *       decodificar los mismos ficheros desde cero.</li>
 * </ul>
 *
 * Ojo con lo que <i>no</i> hace: pedir un tamaño reduce la memoria del mapa de
 * píxeles resultante, pero JavaFX no submuestrea al decodificar, así que el
 * coste de abrir el fichero sigue siendo proporcional a su tamaño real. Por
 * eso existe además {@link #cargarDiferida}, que saca ese trabajo del hilo de
 * la interfaz.
 */
public final class Imagenes {

    /**
     * La clave lleva el tamaño pedido: la misma ruta a 34 px y a 300 px son
     * dos mapas de píxeles distintos y cachear solo por ruta devolvería el
     * primero que se hubiera pedido.
     */
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private Imagenes() {
    }

    /**
     * Carga sincrónicamente y cachea. Úsese cuando hay que decidir algo en el
     * acto a partir de la imagen (por ejemplo, si existe, para elegir entre la
     * foto y un dibujo de respaldo).
     *
     * @param ruta  ruta de classpath, empezando por {@code /}
     * @param ancho ancho de decodificación; 0 conserva la proporción
     * @param alto  alto de decodificación; 0 conserva la proporción
     * @return la imagen, o {@code null} si no existe o no se pudo leer
     */
    public static Image cargar(String ruta, double ancho, double alto) {
        return obtener(ruta, ancho, alto, false);
    }

    /**
     * Como {@link #cargar}, pero la decodificación ocurre en un hilo de
     * JavaFX y la vista se refresca sola al terminar. Para rejillas con
     * muchas imágenes: evita bloquear la interfaz mientras se abren.
     *
     * <p>La imagen vuelve inmediatamente, todavía vacía, así que quien la use
     * no puede consultar su tamaño ni {@code isError()} acto seguido.
     */
    public static Image cargarDiferida(String ruta, double ancho, double alto) {
        return obtener(ruta, ancho, alto, true);
    }

    private static Image obtener(String ruta, double ancho, double alto, boolean diferida) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        String clave = ruta + "|" + (int) ancho + "x" + (int) alto + (diferida ? "|d" : "");
        Image cacheada = CACHE.get(clave);
        if (cacheada != null) {
            return cacheada;
        }
        URL recurso = Imagenes.class.getResource(ruta);
        if (recurso == null) {
            return null;
        }
        try {
            Image imagen = new Image(recurso.toExternalForm(), ancho, alto, true, true, diferida);
            // Una imagen diferida aún no sabe si va a fallar, así que solo se
            // descarta aquí la que ya se sabe rota.
            if (!diferida && (imagen.isError() || imagen.getWidth() <= 0)) {
                return null;
            }
            CACHE.put(clave, imagen);
            return imagen;
        } catch (RuntimeException ignorado) {
            return null;
        }
    }

    /** Vacía la caché. Existe para las pruebas; la aplicación no la necesita. */
    public static void vaciar() {
        CACHE.clear();
    }
}
