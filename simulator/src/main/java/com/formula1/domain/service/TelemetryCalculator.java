package com.formula1.domain.service;

import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.EventImpact;
import com.formula1.domain.model.EventOccurrence;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.LapStatus;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.TelemetrySnapshot;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherSnapshot;
import com.formula1.util.MathUtils;

/**
 * Convierte el estado de una vuelta en lecturas de telemetría simulada.
 * Mantiene las fórmulas fuera del coordinador de la sesión y no conoce JavaFX.
 */
public final class TelemetryCalculator {

    public TelemetrySnapshot calcular(Driver piloto, Vehicle vehiculo, Circuit circuito,
                                WeatherSnapshot clima, SimulationConfig config,
                                LapResult resultado, int segmento, int totalSegmentos,
                                double progreso, double velocidad,
                                double combustibleRestante, double desgasteAcumulado,
                                double tiempoAcumulado, double tiempoReferencia,
                                EventImpact impacto, EventOccurrence evento,
                                LapStatus estadoVuelta) {
        double velocidadRelativa = velocidad / vehiculo.getVelocidadMaximaKmh();
        int rpm = (int) Math.round(MathUtils.clamp(
                4_000 + 9_000 * velocidadRelativa + 350 * Math.sin(progreso * 6 * Math.PI),
                4_000, 15_000));

        double referencia = circuito.getRecordVuelta() == null
                ? tiempoReferencia
                : circuito.getRecordVuelta().getTiempoSegundos();

        return new TelemetrySnapshot(
                piloto.getNombre(), vehiculo.getModelo(), segmento, totalSegmentos,
                velocidad, vehiculo.getVelocidadMaximaKmh(), rpm,
                combustibleRestante, MathUtils.clamp(desgasteAcumulado, 0, 100),
                temperaturaNeumaticos(clima, config, velocidadRelativa, progreso)
                        + impacto.deltaTemperaturaNeumaticosC(),
                temperaturaMotor(clima, config, velocidadRelativa, progreso)
                        + impacto.deltaTemperaturaMotorC(),
                Math.min(3, ((segmento - 1) * 3 / totalSegmentos) + 1),
                tiempoAcumulado, tiempoAcumulado - referencia * progreso,
                clima, estadoVuelta, evento);
    }

    private double temperaturaNeumaticos(WeatherSnapshot clima, SimulationConfig config,
                                          double velocidadRelativa, double progreso) {
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
        double enfriamientoLluvia = 18 * (clima.intensidadLluviaPorcentaje() / 100);
        return MathUtils.clamp(58 + 0.35 * clima.temperaturaPistaC()
                + ajusteModo + ajustePresion + 18 * velocidadRelativa
                - enfriamientoLluvia + 3 * Math.sin(progreso * 4 * Math.PI), 35, 125);
    }

    private double temperaturaMotor(WeatherSnapshot clima, SimulationConfig config,
                                     double velocidadRelativa,
                                     double progreso) {
        double ajusteModo = switch (config.getModo()) {
            case AGRESIVA -> 7;
            case NORMAL -> 0;
            case AHORRO -> -5;
        };
        double ajusteAmbiente = (clima.temperaturaC() - 20) * 0.12;
        return MathUtils.clamp(86 + ajusteModo + ajusteAmbiente + 30 * velocidadRelativa
                + 2 * Math.sin(progreso * 2 * Math.PI), 75, 125);
    }
}
