package com.formula1.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitTest {

    private Circuit circuito(String nombre, double longitudKm, String record) {
        Circuit circuit = new Circuit(nombre, "—", longitudKm, 50);
        circuit.setRecordVuelta(new Circuit.LapRecord(
                com.formula1.util.FormatUtils.parseLapTime(record), "—", 2020));
        return circuit;
    }

    @Test
    void derivaElFactorTecnicoDelRecordReal() {
        // Mónaco: lento y sinuoso. Monza: el templo de la velocidad.
        double monaco = circuito("Mónaco", 3.34, "1:10.166").calcularFactorTecnico();
        double monza = circuito("Monza", 5.79, "1:21.046").calcularFactorTecnico();

        assertEquals(1.98, monaco, 0.02);
        assertEquals(1.32, monza, 0.02);
        assertTrue(monaco > monza, "Mónaco debe ser más técnico que Monza");
    }

    @Test
    void usaElFactorPorDefectoSiNoHayRecord() {
        Circuit sinRecord = new Circuit("Nuevo", "—", 5.0, 50);

        assertEquals(Circuit.FACTOR_TECNICO_POR_DEFECTO, sinRecord.calcularFactorTecnico(), 0.0001);
    }
}
