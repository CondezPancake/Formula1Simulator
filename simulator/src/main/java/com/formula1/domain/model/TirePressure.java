package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Presión de neumáticos configurable (baja, estándar o alta).
 *
 * Menos presión aumenta la huella de contacto: mejora el tiempo pero
 * dispara el desgaste. Más presión reduce el desgaste a costa de agarre.
 */
public enum TirePressure {

    BAJA("baja", "Baja", 0.995, 1.15),
    ESTANDAR("estandar", "Estándar", 1.000, 1.00),
    ALTA("alta", "Alta", 1.005, 0.90);

    private final String clave;
    private final String etiqueta;
    private final double factorTiempo;
    private final double factorDesgaste;

    TirePressure(String clave, String etiqueta, double factorTiempo, double factorDesgaste) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.factorTiempo = factorTiempo;
        this.factorDesgaste = factorDesgaste;
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

    public double getFactorDesgaste() {
        return factorDesgaste;
    }

    @JsonCreator
    public static TirePressure desdeClave(String clave) {
        for (TirePressure presion : values()) {
            if (presion.clave.equalsIgnoreCase(clave) || presion.name().equalsIgnoreCase(clave)) {
                return presion;
            }
        }
        throw new IllegalArgumentException("Presión de neumáticos desconocida: " + clave);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
