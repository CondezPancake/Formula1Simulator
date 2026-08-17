package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

/**
 * Estado meteorológico inmutable de un segmento de la simulación.
 * Sus métricas usan unidades explícitas para impedir conversiones ambiguas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherSnapshot(
        int segmento,
        int totalSegmentos,
        DynamicWeatherState estado,
        double temperaturaC,
        double humedadPorcentaje,
        double probabilidadLluviaPorcentaje,
        double intensidadLluviaPorcentaje,
        double temperaturaPistaC,
        double gripPorcentaje,
        double traccionPorcentaje,
        double frenadoPorcentaje) {

    public WeatherSnapshot {
        if (totalSegmentos <= 0 || segmento < 1 || segmento > totalSegmentos) {
            throw new IllegalArgumentException("El segmento climático debe pertenecer a la sesión");
        }
        Objects.requireNonNull(estado, "El estado climático es obligatorio");
        requireRange(temperaturaC, -20, 60, "temperatura ambiental");
        requireRange(humedadPorcentaje, 0, 100, "humedad");
        requireRange(probabilidadLluviaPorcentaje, 0, 100, "probabilidad de lluvia");
        requireRange(intensidadLluviaPorcentaje, 0, 100, "intensidad de lluvia");
        requireRange(temperaturaPistaC, -20, 80, "temperatura de pista");
        requireRange(gripPorcentaje, 0, 100, "grip");
        requireRange(traccionPorcentaje, 0, 100, "tracción");
        requireRange(frenadoPorcentaje, 0, 100, "frenado");
    }

    public double progreso() {
        return segmento / (double) totalSegmentos;
    }

    /**
     * Penalización combinada de estado, grip, tracción, frenado y temperatura.
     * Un valor mayor implica una vuelta más lenta.
     */
    public double factorTiempo() {
        double perdidaControl = ((100 - gripPorcentaje)
                + (100 - traccionPorcentaje)
                + (100 - frenadoPorcentaje)) / 300.0;
        double temperaturaNoOptima = Math.abs(temperaturaPistaC - 32) * 0.0004;
        return estado.getFactorTiempoBase() * (1 + 0.10 * perdidaControl + temperaturaNoOptima);
    }

    /** La lluvia reduce ligeramente el consumo al exigir menos aceleración plena. */
    public double factorConsumo() {
        return 1 - 0.06 * (intensidadLluviaPorcentaje / 100);
    }

    /** Temperaturas extremas y poco grip incrementan el castigo al neumático. */
    public double factorDesgaste() {
        double temperatura = Math.abs(temperaturaPistaC - 32) * 0.004;
        double deslizamiento = (100 - gripPorcentaje) * 0.002;
        return 1 + temperatura + deslizamiento;
    }

    public WeatherCondition condicionEquivalente() {
        return estado.getCondicionEquivalente();
    }

    public String estadoPista() {
        return estado.getEstadoPista();
    }

    public String neumaticoRecomendado() {
        return estado.getNeumaticoRecomendado();
    }

    public String estrategiaRecomendada() {
        return estado.getEstrategiaRecomendada();
    }

    /** Aplica una alteración global desde un evento de lluvia o pista. */
    public WeatherSnapshot conImpacto(double deltaIntensidadLluvia,
                                      double deltaGrip) {
        double nuevaIntensidad = clamp(
                intensidadLluviaPorcentaje + deltaIntensidadLluvia, 0, 100);
        double nuevaProbabilidad = clamp(
                probabilidadLluviaPorcentaje + deltaIntensidadLluvia * 0.55, 0, 100);
        double nuevaHumedad = clamp(
                humedadPorcentaje + deltaIntensidadLluvia * 0.25, 0, 100);
        double nuevaTemperaturaPista = clamp(
                temperaturaPistaC - deltaIntensidadLluvia * 0.06, -20, 80);
        double nuevoGrip = clamp(gripPorcentaje + deltaGrip, 0, 100);
        double nuevaTraccion = clamp(
                traccionPorcentaje + deltaGrip - Math.max(0, deltaIntensidadLluvia) * 0.04,
                0, 100);
        double nuevoFrenado = clamp(
                frenadoPorcentaje + deltaGrip - Math.max(0, deltaIntensidadLluvia) * 0.06,
                0, 100);
        DynamicWeatherState nuevoEstado = deltaIntensidadLluvia == 0
                ? estado
                : DynamicWeatherState.desdeIntensidad(nuevaIntensidad);
        return new WeatherSnapshot(segmento, totalSegmentos, nuevoEstado,
                temperaturaC, nuevaHumedad, nuevaProbabilidad, nuevaIntensidad,
                nuevaTemperaturaPista, nuevoGrip, nuevaTraccion, nuevoFrenado);
    }

    private static void requireRange(double value, double min, double max, String metric) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Valor inválido para " + metric);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
