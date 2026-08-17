package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.Circuit;
import com.formula1.model.WeatherCondition;
import com.formula1.model.WeatherSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicWeatherServiceTest {

    @Test
    void generaUnaEvolucionCompletaSuaveYAcotada() {
        Circuit monza = DataStore.enMemoria().circuitos().get("Circuito de Monza");
        DynamicWeatherService servicio = new DynamicWeatherService(new Random(19));

        List<WeatherSnapshot> muestras = servicio.generar(
                monza, WeatherCondition.LLUVIOSO, 20);

        assertEquals(20, muestras.size());
        assertNotEquals(muestras.get(0).temperaturaC(), muestras.get(19).temperaturaC());
        for (int i = 0; i < muestras.size(); i++) {
            WeatherSnapshot muestra = muestras.get(i);
            assertEquals(i + 1, muestra.segmento());
            assertTrue(muestra.humedadPorcentaje() >= 0 && muestra.humedadPorcentaje() <= 100);
            assertTrue(muestra.gripPorcentaje() >= 0 && muestra.gripPorcentaje() <= 100);
            if (i > 0) {
                double salto = Math.abs(muestra.intensidadLluviaPorcentaje()
                        - muestras.get(i - 1).intensidadLluviaPorcentaje());
                assertTrue(salto < 10, "el clima no debe saltar bruscamente entre segmentos");
            }
        }
    }

    @Test
    void rechazaUnaSolicitudIncompleta() {
        DynamicWeatherService servicio = new DynamicWeatherService(new Random(1));
        assertThrows(ValidationException.class,
                () -> servicio.generar(null, WeatherCondition.SECO, 20));
    }
}
