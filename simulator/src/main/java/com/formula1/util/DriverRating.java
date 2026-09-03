package com.formula1.util;

import com.formula1.model.Driver;

/**
 * Valoración general (OVR) del piloto, al estilo de los videojuegos de manager.
 *
 * No es un dato de modelo ni se persiste: se deriva en el momento de pintar
 * la interfaz a partir de las habilidades que el piloto ya tiene, para no
 * duplicar información ni añadir un campo que habría que mantener sincronizado.
 */
public final class DriverRating {

    private static final double PESO_VELOCIDAD = 0.40;
    private static final double PESO_CONSISTENCIA = 0.35;
    private static final double PESO_LLUVIA = 0.25;

    private DriverRating() {
    }

    /** Media ponderada de velocidad, consistencia y lluvia, redondeada al entero más cercano. */
    public static int ovr(Driver piloto) {
        double valor = piloto.getHabilidad(Driver.HABILIDAD_VELOCIDAD) * PESO_VELOCIDAD
                + piloto.getHabilidad(Driver.HABILIDAD_CONSISTENCIA) * PESO_CONSISTENCIA
                + piloto.getHabilidad(Driver.HABILIDAD_LLUVIA) * PESO_LLUVIA;
        return (int) Math.round(valor);
    }
}
