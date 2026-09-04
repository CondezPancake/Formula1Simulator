package com.formula1.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SectorTimesTest {

    @Test
    void exposesEachPartialAndItsTotal() {
        SectorTimes times = new SectorTimes(22.341, 24.512, 23.381);

        assertEquals(22.341, times.tiempoDe(TrackSector.SECTOR_1), 1e-9);
        assertEquals(24.512, times.tiempoDe(TrackSector.SECTOR_2), 1e-9);
        assertEquals(23.381, times.tiempoDe(TrackSector.SECTOR_3), 1e-9);
        assertEquals(70.234, times.tiempoTotal(), 1e-9);
    }

    @Test
    void rejectsIncompleteOrInvalidPartials() {
        assertThrows(IllegalArgumentException.class,
                () -> new SectorTimes(0, 24.512, 23.381));
        assertThrows(IllegalArgumentException.class,
                () -> new SectorTimes(22.341, Double.NaN, 23.381));
        SectorTimes times = new SectorTimes(22.341, 24.512, 23.381);
        assertThrows(IllegalArgumentException.class,
                () -> times.tiempoDe(TrackSector.NONE));
    }
}
