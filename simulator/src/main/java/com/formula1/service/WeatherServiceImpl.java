package com.formula1.service;

import com.formula1.model.TrackStatus;
import com.formula1.model.Weather;
import com.formula1.util.RandomUtils;

import java.util.List;

/**
 * Genera condiciones climáticas iniciales para una sesión (RF-11).
 * La evolución dinámica del clima durante la sesión (HU-29) queda fuera
 * del alcance de este núcleo MVP.
 */
public class WeatherServiceImpl implements WeatherService {

    @Override
    public Weather generateWeather() {
        double temperatura = RandomUtils.randomDouble(15, 35);
        double humedad = RandomUtils.randomDouble(30, 90);
        double probabilidadLluvia = RandomUtils.randomDouble(0, 100);
        TrackStatus estadoPista = RandomUtils.pickRandom(List.of(TrackStatus.values()));
        double intensidadLluvia = estadoPista == TrackStatus.SECO || estadoPista == TrackStatus.NUBLADO
                ? 0
                : RandomUtils.randomDouble(1, 10);
        double temperaturaPista = temperatura + RandomUtils.randomDouble(5, 15);

        return new Weather(temperatura, humedad, probabilidadLluvia, intensidadLluvia, temperaturaPista, estadoPista);
    }
}
