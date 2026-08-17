package com.formula1.model;

/**
 * Lectura inmutable del vehículo durante una vuelta simulada.
 *
 * <p>El motor produce esta información en su propio hilo y la interfaz solo
 * la representa. Mantener el objeto inmutable evita estados parciales cuando
 * la muestra cruza hacia el JavaFX Application Thread.</p>
 */
public record TelemetrySnapshot(
        String piloto,
        String vehiculo,
        int segmento,
        int totalSegmentos,
        double velocidadKmh,
        double velocidadMaximaKmh,
        int rpm,
        double combustibleRestantePorcentaje,
        double desgasteNeumaticosPorcentaje,
        double temperaturaNeumaticosC,
        double temperaturaMotorC,
        int sectorActual,
        double tiempoVueltaSegundos,
        double deltaSegundos,
        String estadoPista) {

    public TelemetrySnapshot {
        requireText(piloto, "El piloto es obligatorio");
        requireText(vehiculo, "El vehículo es obligatorio");
        requireText(estadoPista, "El estado de pista es obligatorio");
        if (totalSegmentos <= 0 || segmento < 1 || segmento > totalSegmentos) {
            throw new IllegalArgumentException("El segmento debe pertenecer a la vuelta");
        }
        if (sectorActual < 1 || sectorActual > 3) {
            throw new IllegalArgumentException("El sector debe estar entre 1 y 3");
        }
        requireRange(velocidadKmh, 0, velocidadMaximaKmh, "velocidad");
        requireRange(velocidadMaximaKmh, 1, 500, "velocidad máxima");
        if (rpm < 0 || rpm > 20_000) {
            throw new IllegalArgumentException("Las RPM deben estar entre 0 y 20000");
        }
        requireRange(combustibleRestantePorcentaje, 0, 100, "combustible");
        requireRange(desgasteNeumaticosPorcentaje, 0, 100, "desgaste");
        requireRange(temperaturaNeumaticosC, 0, 150, "temperatura de neumáticos");
        requireRange(temperaturaMotorC, 0, 160, "temperatura del motor");
        requireRange(tiempoVueltaSegundos, 0, 600, "tiempo de vuelta");
        if (!Double.isFinite(deltaSegundos)) {
            throw new IllegalArgumentException("El delta debe ser finito");
        }
    }

    public double progresoVuelta() {
        return segmento / (double) totalSegmentos;
    }

    public double velocidadRelativa() {
        return velocidadKmh / velocidadMaximaKmh;
    }

    public double rpmRelativas() {
        return Math.min(1, rpm / 15_000.0);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireRange(double value, double min, double max, String metric) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Valor inválido para " + metric);
        }
    }
}
