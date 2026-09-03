package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Modos de conducción configurables. Las claves coinciden con los bloques
 * de {@code rendimiento} que define la especificación para cada vehículo.
 */
public enum DrivingMode {

    NORMAL("conduccion_normal", "Normal"),
    AGRESIVA("conduccion_agresiva", "Agresiva"),
    AHORRO("ahorro_combustible", "Ahorro de combustible");

    private final String clave;
    private final String etiqueta;

    DrivingMode(String clave, String etiqueta) {
        this.clave = clave;
        this.etiqueta = etiqueta;
    }

    @JsonValue
    public String getClave() {
        return clave;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @JsonCreator
    public static DrivingMode desdeClave(String clave) {
        for (DrivingMode modo : values()) {
            if (modo.clave.equalsIgnoreCase(clave) || modo.name().equalsIgnoreCase(clave)) {
                return modo;
            }
        }
        throw new IllegalArgumentException("Modo de conducción desconocido: " + clave);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
