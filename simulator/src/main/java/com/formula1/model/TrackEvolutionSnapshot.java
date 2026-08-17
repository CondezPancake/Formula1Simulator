package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Estado consolidado de la pista durante la vuelta de un piloto.
 * Distingue la goma acumulada del grip final, que también depende del clima.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrackEvolutionSnapshot(
        int vuelta,
        String piloto,
        double gripInicialPorcentaje,
        double gripFinalPorcentaje,
        double gomaInicialPorcentaje,
        double gomaFinalPorcentaje,
        double lluviaPromedioPorcentaje) {

    public TrackEvolutionSnapshot {
        if (vuelta <= 0) {
            throw new IllegalArgumentException("La vuelta debe ser positiva");
        }
        if (piloto == null || piloto.isBlank()) {
            throw new IllegalArgumentException("El piloto es obligatorio");
        }
        requirePercentage(gripInicialPorcentaje, "grip inicial");
        requirePercentage(gripFinalPorcentaje, "grip final");
        requirePercentage(gomaInicialPorcentaje, "goma inicial");
        requirePercentage(gomaFinalPorcentaje, "goma final");
        requirePercentage(lluviaPromedioPorcentaje, "lluvia promedio");
    }

    public String tendencia() {
        if (gomaFinalPorcentaje < gomaInicialPorcentaje - 0.01) {
            return "La lluvia limpia la pista";
        }
        if (gomaFinalPorcentaje > gomaInicialPorcentaje + 0.01) {
            return "Más goma en pista";
        }
        return "Pista estable";
    }

    private static void requirePercentage(double value, String metric) {
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException("Valor inválido para " + metric);
        }
    }
}
