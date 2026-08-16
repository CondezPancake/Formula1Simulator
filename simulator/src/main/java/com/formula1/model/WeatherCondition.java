package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Condiciones climáticas de una sesión de clasificación.
 *
 * Son exactamente los tres estados que define la especificación y que se
 * usan como claves en los bloques {@code consumo_combustible} y
 * {@code desgaste_neumaticos} de cada vehículo.
 */
public enum WeatherCondition {

    SECO("seco", "Seco", 1.000),
    LLUVIOSO("lluvioso", "Lluvioso", 1.080),
    EXTREMO("extremo", "Extremo", 1.180);

    private final String clave;
    private final String etiqueta;
    private final double factorTiempo;

    WeatherCondition(String clave, String etiqueta, double factorTiempo) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.factorTiempo = factorTiempo;
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

    @JsonCreator
    public static WeatherCondition desdeClave(String clave) {
        for (WeatherCondition condicion : values()) {
            if (condicion.clave.equalsIgnoreCase(clave) || condicion.name().equalsIgnoreCase(clave)) {
                return condicion;
            }
        }
        throw new IllegalArgumentException("Condición climática desconocida: " + clave);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
