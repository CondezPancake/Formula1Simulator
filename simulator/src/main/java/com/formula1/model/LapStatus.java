package com.formula1.model;

/** Validez de la vuelta y disponibilidad posterior del piloto. */
public enum LapStatus {
    VALID("Válida"),
    INVALID("Invalidada"),
    OUT("Fuera de sesión");

    private final String etiqueta;

    LapStatus(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
