package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Parciales inmutables de una vuelta completa.
 *
 * <p>El tiempo total sigue perteneciendo a {@link LapResult}; este objeto
 * agrupa los tres valores que siempre deben existir y ser válidos juntos.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SectorTimes(
        double sector1Seconds,
        double sector2Seconds,
        double sector3Seconds) {

    public SectorTimes {
        requirePositive(sector1Seconds, "sector 1");
        requirePositive(sector2Seconds, "sector 2");
        requirePositive(sector3Seconds, "sector 3");
    }

    /** Devuelve el parcial solicitado y rechaza el valor NONE explícitamente. */
    public double tiempoDe(TrackSector sector) {
        if (sector == null) {
            throw new IllegalArgumentException("El sector es obligatorio");
        }
        return switch (sector) {
            case SECTOR_1 -> sector1Seconds;
            case SECTOR_2 -> sector2Seconds;
            case SECTOR_3 -> sector3Seconds;
            case NONE -> throw new IllegalArgumentException("NONE no representa un parcial");
        };
    }

    @JsonIgnore
    public double tiempoTotal() {
        return sector1Seconds + sector2Seconds + sector3Seconds;
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("El tiempo de " + name + " debe ser positivo y finito");
        }
    }
}
