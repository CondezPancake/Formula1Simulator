package com.formula1.adapter.in.javafx;

import com.formula1.domain.model.EventCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventNotificationClassificationTest {

    @Test
    void soloLasCategoriasImportantesGeneranAvisoFlotante() {
        assertFalse(SimulationController.esEventoImportante(EventCategory.NO_EVENT));
        assertFalse(SimulationController.esEventoImportante(EventCategory.POSITIVE));
        assertFalse(SimulationController.esEventoImportante(EventCategory.MINOR_NEGATIVE));

        assertTrue(SimulationController.esEventoImportante(EventCategory.MAJOR_NEGATIVE));
        assertTrue(SimulationController.esEventoImportante(EventCategory.WEATHER_TRACK));
        assertTrue(SimulationController.esEventoImportante(EventCategory.EXCEPTIONAL));
    }
}
