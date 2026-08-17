package com.formula1.model;

/** Cambios concretos que un evento aplica sobre el motor y la telemetría. */
public record EventImpact(
        double deltaTiempoSegundos,
        double multiplicadorVelocidad,
        double deltaGripPorcentaje,
        double deltaDesgaste,
        double deltaTemperaturaNeumaticosC,
        double deltaTemperaturaMotorC,
        double deltaIntensidadLluviaPorcentaje,
        boolean vueltaInvalidada,
        boolean pilotoFuera,
        TrackFlag bandera) {

    public EventImpact {
        requireFinite(deltaTiempoSegundos, "delta de tiempo");
        requireRange(multiplicadorVelocidad, 0, 1.20, "multiplicador de velocidad");
        requireRange(deltaGripPorcentaje, -40, 20, "delta de grip");
        requireRange(deltaDesgaste, -20, 40, "delta de desgaste");
        requireRange(deltaTemperaturaNeumaticosC, -40, 40, "temperatura de neumáticos");
        requireRange(deltaTemperaturaMotorC, -30, 40, "temperatura del motor");
        requireRange(deltaIntensidadLluviaPorcentaje, -100, 100, "intensidad de lluvia");
        if (bandera == null) {
            bandera = TrackFlag.GREEN;
        }
        if (pilotoFuera && !vueltaInvalidada) {
            throw new IllegalArgumentException("Un piloto fuera debe tener la vuelta invalidada");
        }
    }

    public static EventImpact none() {
        return new EventImpact(0, 1, 0, 0, 0, 0, 0,
                false, false, TrackFlag.GREEN);
    }

    private static void requireFinite(double value, String metric) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Valor inválido para " + metric);
        }
    }

    private static void requireRange(double value, double min, double max, String metric) {
        requireFinite(value, metric);
        if (value < min || value > max) {
            throw new IllegalArgumentException("Valor fuera de rango para " + metric);
        }
    }
}
