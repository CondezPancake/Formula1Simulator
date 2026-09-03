package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Rol del piloto dentro de su equipo, tal como lo define la especificación.
 */
public enum DriverRole {

    LIDER("Líder"),
    ESCUDERO("Escudero");

    private final String etiqueta;

    DriverRole(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getEtiqueta() {
        return etiqueta;
    }

    @JsonCreator
    public static DriverRole desdeEtiqueta(String etiqueta) {
        for (DriverRole rol : values()) {
            if (rol.etiqueta.equalsIgnoreCase(etiqueta) || rol.name().equalsIgnoreCase(etiqueta)) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol de piloto desconocido: " + etiqueta);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
