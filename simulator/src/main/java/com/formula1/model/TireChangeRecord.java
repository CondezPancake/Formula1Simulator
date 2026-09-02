package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Cambio de compuesto inmutable, correlacionado con una parada en boxes. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TireChangeRecord(
        String pitStopId,
        int pilotoId,
        String piloto,
        int vuelta,
        int segmento,
        TireCompound anterior,
        TireCompound nuevo,
        PitStopReason motivo) {

    public TireChangeRecord {
        if (pitStopId == null || pitStopId.isBlank()) {
            throw new IllegalArgumentException("El cambio debe pertenecer a una parada");
        }
        if (pilotoId <= 0 || piloto == null || piloto.isBlank()) {
            throw new IllegalArgumentException("El piloto del cambio es obligatorio");
        }
        if (vuelta < 1 || segmento < 1) {
            throw new IllegalArgumentException("La vuelta y el segmento deben ser positivos");
        }
        if (anterior == null || nuevo == null || motivo == null) {
            throw new IllegalArgumentException("Los compuestos y el motivo son obligatorios");
        }
        if (anterior == nuevo) {
            throw new IllegalArgumentException("Un cambio debe montar un compuesto diferente");
        }
    }
}
