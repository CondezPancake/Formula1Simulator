package com.formula1.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackEvolutionSnapshotTest {

    @Test
    void describesRubberGainAndRainWash() {
        TrackEvolutionSnapshot dry = new TrackEvolutionSnapshot(
                1, "Driver", 82, 86, 0, 0.55, 0);
        TrackEvolutionSnapshot wet = new TrackEvolutionSnapshot(
                2, "Driver", 84, 73, 8, 3, 80);

        assertEquals("Más goma en pista", dry.tendencia());
        assertEquals("La lluvia limpia la pista", wet.tendencia());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () ->
                new TrackEvolutionSnapshot(0, "Driver", 82, 86, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new TrackEvolutionSnapshot(1, "Driver", 101, 86, 0, 1, 0));
    }
}
