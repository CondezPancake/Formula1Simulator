package com.formula1.util;

import java.util.Locale;

/**
 * Iniciales de respaldo cuando no hay foto ni logo que mostrar en una ficha.
 *
 * Para un nombre de persona ("Max Verstappen") usa la primera letra de pila
 * y la primera de apellido; para un nombre de una sola palabra (un equipo,
 * un modelo, un circuito) usa sus dos primeras letras.
 */
public final class Iniciales {

    private Iniciales() {
    }

    public static String de(String texto) {
        if (texto == null || texto.isBlank()) {
            return "?";
        }
        String[] partes = texto.trim().split("\\s+");
        if (partes.length == 1) {
            String palabra = partes[0];
            return (palabra.length() >= 2 ? palabra.substring(0, 2) : palabra).toUpperCase(Locale.ROOT);
        }
        String primera = partes[0].substring(0, 1);
        String ultima = partes[partes.length - 1].substring(0, 1);
        return (primera + ultima).toUpperCase(Locale.ROOT);
    }
}
