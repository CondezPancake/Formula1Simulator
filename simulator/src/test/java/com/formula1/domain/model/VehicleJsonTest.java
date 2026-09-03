package com.formula1.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El rendimiento usa enums como CLAVES de mapa, que Jackson trata con un
 * mecanismo distinto al de los valores. Estos tests fijan ese contrato:
 * si se rompe, el seed y la persistencia dejarían de leerse.
 */
class VehicleJsonTest {

    private static final String JSON = """
            {
              "modelo": "RB20",
              "equipo": "Red Bull Racing",
              "motor": "Honda",
              "velocidad_maxima_kmh": 360,
              "aceleracion_0_100": 2.5,
              "pilotos": [1, 2],
              "rendimiento": {
                "conduccion_normal": {
                  "velocidad_promedio_kmh": 320,
                  "consumo_combustible": { "seco": 1.9, "lluvioso": 2.1, "extremo": 2.4 },
                  "desgaste_neumaticos": { "seco": 1.5, "lluvioso": 0.8, "extremo": 2.5 }
                },
                "conduccion_agresiva": {
                  "velocidad_promedio_kmh": 340,
                  "consumo_combustible": { "seco": 2.4, "lluvioso": 2.6, "extremo": 3.0 },
                  "desgaste_neumaticos": { "seco": 2.2, "lluvioso": 1.2, "extremo": 3.5 }
                }
              }
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void leeElFormatoLiteralDeLaEspecificacion() throws Exception {
        Vehicle rb20 = mapper.readValue(JSON, Vehicle.class);

        assertEquals("RB20", rb20.getModelo());
        assertEquals(360, rb20.getVelocidadMaximaKmh());
        assertEquals(2.5, rb20.getAceleracion0100(), 0.001);
        assertEquals(2, rb20.getPilotos().size());

        Vehicle.Performance normal = rb20.rendimientoDe(DrivingMode.NORMAL);
        assertEquals(320, normal.getVelocidadPromedioKmh());
        assertEquals(1.9, normal.consumoCon(WeatherCondition.SECO), 0.001);
        assertEquals(2.5, normal.desgasteCon(WeatherCondition.EXTREMO), 0.001);

        assertEquals(340, rb20.rendimientoDe(DrivingMode.AGRESIVA).getVelocidadPromedioKmh());
    }

    @Test
    void caeAModoNormalSiElModoNoEstaDefinido() throws Exception {
        Vehicle rb20 = mapper.readValue(JSON, Vehicle.class);

        // El JSON no define ahorro_combustible: no debe devolver null.
        assertEquals(320, rb20.rendimientoDe(DrivingMode.AHORRO).getVelocidadPromedioKmh());
    }

    @Test
    void sobreviveAlRoundTripDeSerializacion() throws Exception {
        Vehicle original = mapper.readValue(JSON, Vehicle.class);

        Vehicle copia = mapper.readValue(mapper.writeValueAsString(original), Vehicle.class);

        assertEquals(original.getModelo(), copia.getModelo());
        assertEquals(1.9, copia.rendimientoDe(DrivingMode.NORMAL).consumoCon(WeatherCondition.SECO), 0.001);
        assertEquals(3.5, copia.rendimientoDe(DrivingMode.AGRESIVA).desgasteCon(WeatherCondition.EXTREMO), 0.001);
    }
}
