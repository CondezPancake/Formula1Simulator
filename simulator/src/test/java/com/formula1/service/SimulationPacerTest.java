package com.formula1.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationPacerTest {

    @Test
    void distribuyeLaDuracionEntreTodosLosFotogramas() {
        AtomicLong reloj = new AtomicLong();
        SimulationPacer regulador = new SimulationPacer(
                10_000, () -> false, reloj::get, reloj::addAndGet);

        assertTrue(regulador.completarFotograma(1, 4));
        assertEquals(2_500, reloj.get());
        assertTrue(regulador.completarFotograma(4, 4));
        assertEquals(10_000, reloj.get());
    }

    @Test
    void respondeALaFinalizacionManualSinEsperarAlSiguientePlazo() {
        AtomicLong reloj = new AtomicLong();
        SimulationPacer regulador = new SimulationPacer(
                10_000, () -> true, reloj::get, reloj::addAndGet);

        assertFalse(regulador.completarFotograma(1, 20));
        assertEquals(0, reloj.get());
    }
}
