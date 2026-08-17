package com.formula1.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherSnapshotTest {

    @Test
    void unaPistaMojadaPenalizaTiempoYRecomiendaNeumaticosApropiados() {
        WeatherSnapshot seco = muestra(DynamicWeatherState.SECO, 0, 95, 95, 94, 36);
        WeatherSnapshot lluvia = muestra(
                DynamicWeatherState.LLUVIA, 65, 62, 58, 55, 20);

        assertTrue(lluvia.factorTiempo() > seco.factorTiempo());
        assertTrue(lluvia.factorDesgaste() > seco.factorDesgaste());
        assertEquals("Intermedios", lluvia.neumaticoRecomendado());
        assertEquals("Conservadora", lluvia.estrategiaRecomendada());
    }

    @Test
    void rechazaPorcentajesFueraDeRango() {
        assertThrows(IllegalArgumentException.class,
                () -> muestra(DynamicWeatherState.SECO, 101, 95, 95, 94, 36));
    }

    @Test
    void conservaLaMuestraAlSerializarlaParaMongo() throws Exception {
        WeatherSnapshot original = muestra(
                DynamicWeatherState.LLUVIA_LIGERA, 25, 80, 77, 75, 24);
        ObjectMapper mapper = new ObjectMapper();

        WeatherSnapshot recuperada = mapper.readValue(
                mapper.writeValueAsString(original), WeatherSnapshot.class);

        assertEquals(original, recuperada);
    }

    private WeatherSnapshot muestra(DynamicWeatherState estado, double intensidad,
                                     double grip, double traccion, double frenado,
                                     double temperaturaPista) {
        return new WeatherSnapshot(1, 1, estado, 24, 60, 50,
                intensidad, temperaturaPista, grip, traccion, frenado);
    }
}
