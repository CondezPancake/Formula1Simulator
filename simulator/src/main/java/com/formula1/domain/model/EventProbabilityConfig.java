package com.formula1.domain.model;

/** Distribución configurable de la primera etapa de selección de eventos. */
public record EventProbabilityConfig(
        double sinEvento,
        double positivo,
        double negativoLeve,
        double negativoImportante,
        double climaPista,
        double excepcional,
        double coexistenciaGlobal) {

    private static final double TOLERANCIA = 1e-9;

    public EventProbabilityConfig {
        requireProbability(sinEvento, "sin evento");
        requireProbability(positivo, "positivo");
        requireProbability(negativoLeve, "negativo leve");
        requireProbability(negativoImportante, "negativo importante");
        requireProbability(climaPista, "clima/pista");
        requireProbability(excepcional, "excepcional");
        requireProbability(coexistenciaGlobal, "coexistencia global");
        double total = sinEvento + positivo + negativoLeve
                + negativoImportante + climaPista + excepcional;
        if (Math.abs(total - 1) > TOLERANCIA) {
            throw new IllegalArgumentException("Las probabilidades principales deben sumar 1");
        }
    }

    public static EventProbabilityConfig standard() {
        return new EventProbabilityConfig(0.72, 0.08, 0.12, 0.05, 0.02, 0.01, 0.005);
    }

    public static EventProbabilityConfig disabled() {
        return new EventProbabilityConfig(1, 0, 0, 0, 0, 0, 0);
    }

    public double probabilidad(EventCategory categoria) {
        return switch (categoria) {
            case NO_EVENT -> sinEvento;
            case POSITIVE -> positivo;
            case MINOR_NEGATIVE -> negativoLeve;
            case MAJOR_NEGATIVE -> negativoImportante;
            case WEATHER_TRACK -> climaPista;
            case EXCEPTIONAL -> excepcional;
        };
    }

    private static void requireProbability(double value, String name) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Probabilidad inválida para " + name);
        }
    }
}
