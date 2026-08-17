package com.formula1.util;

import java.util.Map;

/** Color de acento por escudería, para bordes de tarjeta en Explorar. */
public final class TeamColors {

    private static final String POR_DEFECTO = "#6c6c80";

    // Paleta oficial de la parrilla, tomada pixel a pixel de las capturas del
    // mockup (docs/assets/F1_Recursos_Multimedia/Mockup_Design/).
    private static final Map<String, String> COLORES = Map.ofEntries(
            Map.entry("Red Bull Racing", "#3671C6"),
            Map.entry("Ferrari", "#E8002D"),
            Map.entry("Mercedes-AMG Petronas", "#27F4D2"),
            Map.entry("McLaren", "#FF8000"),
            Map.entry("Aston Martin", "#229971"),
            Map.entry("Alpine", "#FF87BC"),
            Map.entry("Alfa Romeo", "#C92D4B"),
            Map.entry("Haas", "#B6BABD"),
            Map.entry("AlphaTauri", "#6692FF"),
            Map.entry("Williams", "#64C4FF"));

    private TeamColors() {
    }

    /** Hex del color de acento del equipo, o un gris neutro si no está mapeado. */
    public static String hex(String equipo) {
        return equipo == null ? POR_DEFECTO : COLORES.getOrDefault(equipo, POR_DEFECTO);
    }
}
