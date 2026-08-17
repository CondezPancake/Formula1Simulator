package com.formula1.model;

/**
 * Resumen inmutable de una clasificación, independiente de cualquier
 * componente JavaFX para que pueda probarse y reutilizarse.
 */
public record SessionStatistics(
        int participantes,
        double tiempoPole,
        double tiempoPromedio,
        double diferenciaMaxima,
        double consumoPromedio,
        double desgastePromedio) {

    public SessionStatistics {
        if (participantes < 0) {
            throw new IllegalArgumentException("La cantidad de participantes no puede ser negativa");
        }
        if (!sonMetricasValidas(tiempoPole, tiempoPromedio, diferenciaMaxima,
                consumoPromedio, desgastePromedio)) {
            throw new IllegalArgumentException("Las estadísticas deben ser finitas y no negativas");
        }
    }

    public static SessionStatistics vacias() {
        return new SessionStatistics(0, 0, 0, 0, 0, 0);
    }

    public boolean tieneResultados() {
        return participantes > 0;
    }

    private static boolean sonMetricasValidas(double... metricas) {
        for (double metrica : metricas) {
            if (!Double.isFinite(metrica) || metrica < 0) {
                return false;
            }
        }
        return true;
    }
}
