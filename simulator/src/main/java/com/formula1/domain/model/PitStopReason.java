package com.formula1.domain.model;

/** Motivo de negocio que llevó al motor a ordenar una parada. */
public enum PitStopReason {
    TYRE_CONDITION("Estado crítico de neumáticos"),
    WEATHER_CHANGE("Cambio de condiciones"),
    MECHANICAL_RISK("Riesgo mecánico"),
    EXCESSIVE_WEAR("Desgaste excesivo");

    private final String etiqueta;

    PitStopReason(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
