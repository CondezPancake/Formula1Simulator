package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Estrategia de combustible configurable.
 *
 * Empujar quema más combustible a cambio de tiempo; ahorrar es lo inverso.
 */
public enum FuelStrategy {

    AGRESIVA("agresiva", "Agresiva", 0.990, 1.15),
    BALANCEADA("balanceada", "Balanceada", 1.000, 1.00),
    AHORRO("ahorro", "Ahorro", 1.010, 0.85);

    private final String clave;
    private final String etiqueta;
    private final double factorTiempo;
    private final double factorConsumo;

    FuelStrategy(String clave, String etiqueta, double factorTiempo, double factorConsumo) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.factorTiempo = factorTiempo;
        this.factorConsumo = factorConsumo;
    }

    @JsonValue
    public String getClave() {
        return clave;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public double getFactorTiempo() {
        return factorTiempo;
    }

    public double getFactorConsumo() {
        return factorConsumo;
    }

    @JsonCreator
    public static FuelStrategy desdeClave(String clave) {
        for (FuelStrategy estrategia : values()) {
            if (estrategia.clave.equalsIgnoreCase(clave) || estrategia.name().equalsIgnoreCase(clave)) {
                return estrategia;
            }
        }
        throw new IllegalArgumentException("Estrategia de combustible desconocida: " + clave);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
