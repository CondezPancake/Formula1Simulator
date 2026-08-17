package com.formula1.service;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.LapResult;
import com.formula1.model.SimulationConfig;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.util.MathUtils;

/**
 * Convierte el estado de una vuelta en lecturas de telemetría simulada.
 * Mantiene las fórmulas fuera del coordinador de la sesión y no conoce JavaFX.
 */
final class TelemetryCalculator {

    TelemetrySnapshot calcular(Driver piloto, Vehicle vehiculo, Circuit circuito,
                                WeatherCondition clima, SimulationConfig config,
                                LapResult resultado, int segmento, int totalSegmentos,
                                double progreso, double velocidad) {
        double velocidadRelativa = velocidad / vehiculo.getVelocidadMaximaKmh();
        int rpm = (int) Math.round(MathUtils.clamp(
                4_000 + 9_000 * velocidadRelativa + 350 * Math.sin(progreso * 6 * Math.PI),
                4_000, 15_000));

        // El depósito se expresa como porcentaje de la carga asignada a esta vuelta.
        double combustibleRestante = 100 * (1 - progreso);
        double desgaste = MathUtils.clamp(resultado.getDesgasteEstimado() * progreso, 0, 100);
        double tiempoVuelta = resultado.getTiempoSegundos() * progreso;
        double referencia = circuito.getRecordVuelta() == null
                ? resultado.getTiempoSegundos()
                : circuito.getRecordVuelta().getTiempoSegundos();

        return new TelemetrySnapshot(
                piloto.getNombre(), vehiculo.getModelo(), segmento, totalSegmentos,
                velocidad, vehiculo.getVelocidadMaximaKmh(), rpm,
                combustibleRestante, desgaste,
                temperaturaNeumaticos(clima, config, velocidadRelativa, progreso),
                temperaturaMotor(config, velocidadRelativa, progreso),
                Math.min(3, ((segmento - 1) * 3 / totalSegmentos) + 1),
                tiempoVuelta, tiempoVuelta - referencia * progreso,
                estadoPista(clima));
    }

    private double temperaturaNeumaticos(WeatherCondition clima, SimulationConfig config,
                                          double velocidadRelativa, double progreso) {
        double baseClima = switch (clima) {
            case SECO -> 78;
            case LLUVIOSO -> 62;
            case EXTREMO -> 50;
        };
        double ajusteModo = switch (config.getModo()) {
            case AGRESIVA -> 8;
            case NORMAL -> 0;
            case AHORRO -> -5;
        };
        double ajustePresion = switch (config.getPresion()) {
            case BAJA -> 5;
            case ESTANDAR -> 0;
            case ALTA -> -3;
        };
        return MathUtils.clamp(baseClima + ajusteModo + ajustePresion
                + 16 * velocidadRelativa + 3 * Math.sin(progreso * 4 * Math.PI), 35, 125);
    }

    private double temperaturaMotor(SimulationConfig config, double velocidadRelativa,
                                     double progreso) {
        double ajusteModo = switch (config.getModo()) {
            case AGRESIVA -> 7;
            case NORMAL -> 0;
            case AHORRO -> -5;
        };
        return MathUtils.clamp(86 + ajusteModo + 30 * velocidadRelativa
                + 2 * Math.sin(progreso * 2 * Math.PI), 75, 125);
    }

    private String estadoPista(WeatherCondition clima) {
        return switch (clima) {
            case SECO -> "Pista seca";
            case LLUVIOSO -> "Pista mojada";
            case EXTREMO -> "Adherencia crítica";
        };
    }
}
