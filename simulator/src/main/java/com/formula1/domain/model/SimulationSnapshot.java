package com.formula1.domain.model;

/**
 * Muestra inmutable de la evolución del vehículo seleccionado durante una vuelta.
 *
 * <p>Los acumulados avanzan hasta los totales calculados por el motor. De esta
 * forma, la interfaz representa resultados del dominio y no valores decorativos
 * desconectados de la simulación.</p>
 */
public record SimulationSnapshot(
        String piloto,
        String vehiculo,
        int segmento,
        int totalSegmentos,
        double velocidadKmh,
        double velocidadMaximaKmh,
        double consumoAcumulado,
        double consumoTotal,
        double desgasteAcumulado,
        double desgasteTotal) {

    public SimulationSnapshot {
        if (piloto == null || piloto.isBlank() || vehiculo == null || vehiculo.isBlank()) {
            throw new IllegalArgumentException("El piloto y el vehículo son obligatorios");
        }
        if (totalSegmentos <= 0 || segmento < 1 || segmento > totalSegmentos) {
            throw new IllegalArgumentException("El segmento debe pertenecer a la vuelta");
        }
        if (!sonMetricasValidas(velocidadKmh, velocidadMaximaKmh,
                consumoAcumulado, consumoTotal, desgasteAcumulado, desgasteTotal)) {
            throw new IllegalArgumentException("Las métricas de evolución deben ser finitas y no negativas");
        }
        if (consumoAcumulado > consumoTotal || desgasteAcumulado > desgasteTotal) {
            throw new IllegalArgumentException("Un valor acumulado no puede superar su total");
        }
    }

    public double progreso() {
        return segmento / (double) totalSegmentos;
    }

    public double velocidadRelativa() {
        return velocidadMaximaKmh == 0 ? 0 : Math.min(1, velocidadKmh / velocidadMaximaKmh);
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
