package com.formula1.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TelemetrySnapshotTest {

    @Test
    void calculaIndicadoresNormalizados() {
        TelemetrySnapshot muestra = muestraValida(10, 20, 170, 340, 7_500);

        assertEquals(0.5, muestra.progresoVuelta(), 1e-9);
        assertEquals(0.5, muestra.velocidadRelativa(), 1e-9);
        assertEquals(0.5, muestra.rpmRelativas(), 1e-9);
    }

    @Test
    void rechazaLecturasFueraDeLosLimitesDelContrato() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> muestraValida(0, 20, 170, 340, 7_500)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> muestraValida(10, 20, 341, 340, 7_500)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> muestraValida(10, 20, 170, 340, 20_001)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new TelemetrySnapshot("Piloto", "Auto", 10, 20,
                                170, 340, 7_500, -1, 5, 90, 105,
                                2, 40, 0.2, "Pista seca")));
    }

    private TelemetrySnapshot muestraValida(int segmento, int totalSegmentos,
                                             double velocidad, double velocidadMaxima,
                                             int rpm) {
        return new TelemetrySnapshot(
                "Piloto", "Auto", segmento, totalSegmentos,
                velocidad, velocidadMaxima, rpm, 50, 5,
                90, 105, 2, 40, 0.2, "Pista seca");
    }
}
