package com.formula1.adapter.in.javafx;

import com.formula1.adapter.out.memory.DataStore;
import com.formula1.application.usecase.DriverService;
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
