package com.formula1.util;

import java.util.Map;

/**
 * Rutas de classpath de los recursos oficiales de formula1.com.
 *
 * Los ficheros los deja en su sitio {@code tools/descargar_assets_f1.py}; aquí
 * solo se traduce el dato que ya trae el seed —el código de tres letras del
 * piloto, su nacionalidad, el nombre del equipo— a la ruta correspondiente. Se
 * resuelve por convención en vez de añadir campos al seed para no duplicar en
 * los datos algo que es una decisión de presentación.
 *
 * Todas las rutas pueden apuntar a un fichero que no exista (si no se ha
 * ejecutado el script); quien las use debe tolerar el fallo de carga, como ya
 * hace {@link ImageCrop#desdeClasspath}.
 */
public final class F1Assets {

    private static final String RENDERS = "/images/drivers/f1/";
    private static final String BANDERAS = "/images/flags/";
    private static final String LOGOS = "/images/teams/";
    private static final String TEXTURA_DRS = "/images/patterns/drs-mask.png";

    /**
     * Nacionalidad tal cual la escribe {@code seed.json} (sin acentos) al
     * nombre de fichero de la bandera en el CDN de la F1.
     */
    private static final Map<String, String> PAISES = Map.ofEntries(
            Map.entry("Neerlandes", "netherlands"),
            Map.entry("Mexicano", "mexico"),
            Map.entry("Britanico", "great-britain"),
            Map.entry("Monegasco", "monaco"),
            Map.entry("Espanol", "spain"),
            Map.entry("Australiano", "australia"),
            Map.entry("Canadiense", "canada"),
            Map.entry("Frances", "france"),
            Map.entry("Finlandes", "finland"),
            Map.entry("Chino", "china"),
            Map.entry("Danes", "denmark"),
            Map.entry("Aleman", "germany"),
            Map.entry("Japones", "japan"),
            Map.entry("Tailandes", "thailand"),
            Map.entry("Estadounidense", "united-states-of-america"));

    /**
     * Nombre de equipo del seed al identificador que usa el CDN. El seed lleva
     * la parrilla con los nombres de 2023, pero la F1 solo publica la carpeta
     * de 2024, donde Alfa Romeo ya es Kick Sauber y AlphaTauri es RB.
     */
    private static final Map<String, String> ESCUDERIAS = Map.ofEntries(
            Map.entry("Red Bull Racing", "redbullracing"),
            Map.entry("Mercedes-AMG Petronas", "mercedes"),
            Map.entry("Ferrari", "ferrari"),
            Map.entry("McLaren", "mclaren"),
            Map.entry("Aston Martin", "astonmartin"),
            Map.entry("Alpine", "alpine"),
            Map.entry("Alfa Romeo", "kicksauber"),
            Map.entry("Haas", "haas"),
            Map.entry("AlphaTauri", "rb"),
            Map.entry("Williams", "williams"));

    private F1Assets() {
    }

    /**
     * Render de cuerpo entero sobre fondo transparente, el que la F1 usa en sus
     * propias tarjetas.
     *
     * @param codigo código de tres letras del piloto (VER, HAM…)
     * @return ruta de classpath, o {@code null} si no hay código
     */
    public static String render(String codigo) {
        return vacio(codigo) ? null : RENDERS + codigo.trim().toUpperCase() + ".png";
    }

    /**
     * @param nacionalidad valor del campo homónimo del piloto
     * @return ruta de la bandera, o {@code null} si la nacionalidad no está mapeada
     */
    public static String bandera(String nacionalidad) {
        if (vacio(nacionalidad)) {
            return null;
        }
        String pais = PAISES.get(nacionalidad.trim());
        return pais == null ? null : BANDERAS + pais + ".jpg";
    }

    /**
     * @param equipo nombre del equipo tal cual aparece en el piloto
     * @return ruta del logo en blanco, o {@code null} si el equipo no está mapeado
     */
    public static String logo(String equipo) {
        if (vacio(equipo)) {
            return null;
        }
        String escuderia = ESCUDERIAS.get(equipo.trim());
        return escuderia == null ? null : LOGOS + escuderia + ".png";
    }

    /**
     * Máscara alfa del patrón de velocidad («DRS») que la F1 superpone al fondo
     * de cada tarjeta. Es blanca: se tiñe en tiempo de ejecución con el color
     * del equipo.
     */
    public static String texturaDrs() {
        return TEXTURA_DRS;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
