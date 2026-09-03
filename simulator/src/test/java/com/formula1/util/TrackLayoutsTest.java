package com.formula1.util;

import com.formula1.data.DataStore;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.TrackLayout;
import com.formula1.domain.model.TrackSector;
import com.formula1.service.CircuitService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los siete circuitos del catálogo tienen que tener su trazado calcado. */
class TrackLayoutsTest {

    @BeforeAll
    static void sembrar() {
        DataStore.getInstance().cargar();
    }

    @Test
    void todoCircuitoDelCatalogoTieneTrazado() {
        // Si alguien renombra un circuito en gen_seed.py, el mapa se quedaria
        // en blanco sin avisar. Esta prueba lo convierte en un fallo ruidoso.
        for (Circuit circuito : new CircuitService().listar()) {
            assertNotNull(TrackLayouts.de(circuito.getNombre()),
                    "Sin trazado para " + circuito.getNombre());
        }
    }

    @Test
    void unCircuitoDesconocidoNoTieneTrazado() {
        // Un circuito dado de alta desde el CRUD no tiene calco, y quien pinta
        // el mapa debe poder distinguirlo.
        assertNull(TrackLayouts.de("Circuito inventado"));
        assertNull(TrackLayouts.de(null));
    }

    @Test
    void losTrazadosSonApaisadosYCerrados() {
        for (String nombre : TrackLayouts.conTrazado()) {
            TrackLayout trazado = TrackLayouts.de(nombre);
            double[] caja = trazado.limites();
            double aspecto = (caja[2] - caja[0]) / (caja[3] - caja[1]);

            // El panel del mapa es apaisado (mínimo 430x320). Un trazado
            // vertical se quedaría con franjas negras a los lados y los coches
            // saldrían diminutos.
            assertTrue(aspecto > 1.0,
                    nombre + " no es apaisado: aspecto " + aspecto);
            assertTrue(trazado.longitud() > 0, nombre + " tiene longitud cero");
        }
    }

    @Test
    void losCortesDeSectorSonLosDelMotor() {
        // No se fijan 0,35 y 0,70 a mano: se derivan de TrackSector para que,
        // si el motor cambia el reparto de segmentos, esta prueba lo cace.
        int total = 20;
        int ultimoDelUno = 0;
        int ultimoDelDos = 0;
        for (int segmento = 1; segmento <= total; segmento++) {
            TrackSector sector = TrackSector.desdeSegmento(segmento, total);
            if (sector == TrackSector.SECTOR_1) {
                ultimoDelUno = segmento;
            } else if (sector == TrackSector.SECTOR_2) {
                ultimoDelDos = segmento;
            }
        }
        assertEquals(0.35, ultimoDelUno / (double) total, 1e-9);
        assertEquals(0.70, ultimoDelDos / (double) total, 1e-9);
    }
}
