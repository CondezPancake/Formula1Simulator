package com.formula1.service;

import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.DynamicWeatherState;
import com.formula1.domain.model.WeatherCondition;
import com.formula1.domain.model.WeatherSnapshot;
import com.formula1.util.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

/** Genera una transición meteorológica suave, reproducible con una semilla. */
final class DynamicWeatherService {

    private final RandomGenerator aleatorio;

    DynamicWeatherService() {
        this(new Random());
    }

    DynamicWeatherService(RandomGenerator aleatorio) {
        this.aleatorio = aleatorio;
    }

    List<WeatherSnapshot> generar(Circuit circuito, WeatherCondition inicial, int segmentos) {
        if (circuito == null || inicial == null || segmentos <= 0) {
            throw new ValidationException("No se puede generar una evolución climática incompleta");
        }

        double riesgoLluvia = MathUtils.clamp(
                circuito.probabilidadDe(WeatherCondition.LLUVIOSO)
                        + circuito.probabilidadDe(WeatherCondition.EXTREMO), 0, 1);
        double intensidadInicial = switch (inicial) {
            case SECO -> 0;
            case LLUVIOSO -> 0.55;
            case EXTREMO -> 0.88;
        };
        double temperaturaBase = switch (inicial) {
            case SECO -> 25;
            case LLUVIOSO -> 20;
            case EXTREMO -> 17;
        } + variacion(-2, 2);
        double humedadBase = switch (inicial) {
            case SECO -> 43;
            case LLUVIOSO -> 68;
            case EXTREMO -> 82;
        };
        double fase = variacion(-0.35, 0.35);
        List<WeatherSnapshot> muestras = new ArrayList<>(segmentos);

        for (int segmento = 1; segmento <= segmentos; segmento++) {
            double progreso = segmento / (double) segmentos;
            double frente = Math.sin(Math.PI * progreso + fase) * (0.08 + 0.18 * riesgoLluvia);
            double tendencia = (riesgoLluvia - intensidadInicial) * 0.45 * progreso;
            double intensidad = MathUtils.clamp(intensidadInicial + tendencia + frente, 0, 1);
            DynamicWeatherState estado = estadoPara(intensidad);
            double temperatura = MathUtils.clamp(temperaturaBase - 2.5 * progreso
                    - 5 * intensidad + 0.7 * Math.sin(2 * Math.PI * progreso), -20, 60);
            double humedad = MathUtils.clamp(humedadBase + 28 * intensidad + 5 * progreso, 0, 100);
            double probabilidad = MathUtils.clamp(
                    100 * (0.55 * riesgoLluvia + 0.65 * intensidad), 0, 100);
            double temperaturaPista = MathUtils.clamp(
                    temperatura + 12 * (1 - intensidad) - 2 * intensidad, -20, 80);
            double grip = MathUtils.clamp(96 - 45 * intensidad - 0.03 * humedad, 42, 96);
            double traccion = MathUtils.clamp(grip - 6 * intensidad, 38, 96);
            double frenado = MathUtils.clamp(grip - 9 * intensidad, 35, 96);

            muestras.add(new WeatherSnapshot(
                    segmento, segmentos, estado, temperatura, humedad,
                    probabilidad, intensidad * 100, temperaturaPista,
                    grip, traccion, frenado));
        }
        return List.copyOf(muestras);
    }

    private DynamicWeatherState estadoPara(double intensidad) {
        return DynamicWeatherState.desdeIntensidad(intensidad * 100);
    }

    private double variacion(double min, double max) {
        return min + aleatorio.nextDouble() * (max - min);
    }
}
