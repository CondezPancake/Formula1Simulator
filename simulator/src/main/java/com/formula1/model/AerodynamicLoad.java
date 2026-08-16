package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Carga aerodinámica configurable.
 *
 * Más carga da más agarre (mejor tiempo) pero penaliza el consumo por la
 * resistencia añadida; menos carga es lo contrario. Cada opción tiene por
 * tanto una contrapartida, para que la configuración importe.
 */
public enum AerodynamicLoad {

    BAJA("baja", "Baja", 1.010, 0.95, 1.00),
    MEDIA("media", "Media", 1.000, 1.00, 1.00),
    ALTA("alta", "Alta", 0.995, 1.08, 1.05);

    private final String clave;
    private final String etiqueta;
    private final double factorTiempo;
    private final double factorConsumo;
    private final double factorDesgaste;

    AerodynamicLoad(String clave, String etiqueta, double factorTiempo, double factorConsumo, double factorDesgaste) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.factorTiempo = factorTiempo;
        this.factorConsumo = factorConsumo;
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

    public double getFactorConsumo() {
        return factorConsumo;
    }

    public double getFactorDesgaste() {
        return factorDesgaste;
    }

    @JsonCreator
    public static AerodynamicLoad desdeClave(String clave) {
        for (AerodynamicLoad carga : values()) {
            if (carga.clave.equalsIgnoreCase(clave) || carga.name().equalsIgnoreCase(clave)) {
                return carga;
            }
        }
        throw new IllegalArgumentException("Carga aerodinámica desconocida: " + clave);
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
