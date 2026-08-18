package com.formula1.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Localiza las vistas disponibles de un monoplaza dentro del classpath.
 *
 * {@code tools/copiar_imagenes.py} deja cada modelo en su carpeta con nombres
 * fijos —{@code principal.jpg} y {@code auxiliar-N.jpg}—, así que basta con
 * probar rutas en orden en vez de listar el directorio, que no es fiable
 * cuando la aplicación se empaqueta en un jar.
 */
public final class VehicleImages {

    private static final String BASE = "/images/vehicles/";

    /** Tope de seguridad para no sondear indefinidamente. */
    private static final int MAX_AUXILIARES = 8;

    private VehicleImages() {
    }

    /**
     * Vistas del modelo, con la principal primero.
     *
     * @return lista vacía si el modelo no tiene ninguna imagen
     */
    public static List<String> de(String modelo) {
        List<String> vistas = new ArrayList<>();
        if (modelo == null || modelo.isBlank()) {
            return vistas;
        }
        String carpeta = BASE + modelo.trim() + "/";
        agregarSiExiste(vistas, carpeta + "principal.jpg");
        for (int i = 1; i <= MAX_AUXILIARES; i++) {
            String ruta = carpeta + "auxiliar-" + i + ".jpg";
            if (!agregarSiExiste(vistas, ruta)) {
                break;      // los auxiliares van numerados sin huecos
            }
        }
        return vistas;
    }

    /** Vistas distintas de la principal: las que abre la galería. */
    public static List<String> auxiliaresDe(String modelo) {
        List<String> todas = de(modelo);
        return todas.isEmpty() ? todas : todas.subList(1, todas.size());
    }

    public static boolean tieneGaleria(String modelo) {
        return !auxiliaresDe(modelo).isEmpty();
    }

    private static boolean agregarSiExiste(List<String> destino, String ruta) {
        if (VehicleImages.class.getResource(ruta) == null) {
            return false;
        }
        destino.add(ruta);
        return true;
    }
}
