package com.formula1.util;

import java.util.Map;

/**
 * Color de escudería en sus dos variantes, como hace la web oficial de la F1.
 *
 * Formula1.com pinta cada tarjeta de piloto con dos variables CSS:
 * {@code --f1-team-colour}, el color vivo de la marca, para acentos y texturas;
 * y {@code --f1-accessible-colour}, el mismo color oscurecido, que se usa de
 * fondo para que el texto blanco encima contraste. Sin esa segunda variante el
 * amarillo de Renault o el gris de Haas dejan el nombre ilegible.
 */
public final class TeamColors {

    private static final String POR_DEFECTO = "#6c6c80";
    private static final String POR_DEFECTO_ACCESIBLE = "#2B2B38";

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

    // Los seis primeros son los valores literales de --f1-accessible-colour que
    // publica formula1.com para esas escuderías. Los cuatro restantes ya no
    // existen con ese nombre en la parrilla actual, así que se derivan con el
    // mismo criterio: oscurecer hasta pasar de 7:1 de contraste contra blanco.
    private static final Map<String, String> ACCESIBLES = Map.ofEntries(
            Map.entry("Red Bull Racing", "#142948"),
            Map.entry("Ferrari", "#5C0012"),
            Map.entry("Mercedes-AMG Petronas", "#067E6A"),
            Map.entry("McLaren", "#804000"),
            Map.entry("Aston Martin", "#0F4331"),
            Map.entry("AlphaTauri", "#0038C2"),
            Map.entry("Alpine", "#7A2E4E"),
            Map.entry("Alfa Romeo", "#5A1424"),
            Map.entry("Haas", "#545759"),
            Map.entry("Williams", "#16456B"));

    private TeamColors() {
    }

    /** Hex del color vivo del equipo, o un gris neutro si no está mapeado. */
    public static String hex(String equipo) {
        return equipo == null ? POR_DEFECTO : COLORES.getOrDefault(equipo, POR_DEFECTO);
    }

    /** Hex oscurecido del equipo, apto como fondo bajo texto blanco. */
    public static String accesible(String equipo) {
        return equipo == null ? POR_DEFECTO_ACCESIBLE
                : ACCESIBLES.getOrDefault(equipo, POR_DEFECTO_ACCESIBLE);
    }
}
