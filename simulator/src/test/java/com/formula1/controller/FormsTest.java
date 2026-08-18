package com.formula1.controller;

import com.formula1.data.DataStore;
import com.formula1.service.DriverService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormsTest {

    @Test
    void altaDeVehiculoOfreceTodosLosPilotosDelEquipoSeleccionado() {
        var pilotos = new DriverService(DataStore.enMemoria()).listar();

        var elegibles = Forms.pilotosElegibles(pilotos, "Ferrari");

        assertEquals(2, elegibles.size());
        assertTrue(elegibles.stream().allMatch(p -> "Ferrari".equals(p.getEquipo())));
    }
}
