package com.formula1.domain.service;

import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.LapStatus;
import com.formula1.domain.model.SectorTimes;
import com.formula1.domain.model.TrackSector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectorComparisonServiceTest {

    private final SectorComparisonService comparisons = new SectorComparisonService();

    @Test
    void selectsTheFastestDriverIndependentlyForEachSector() {
        LapResult verstappen = result(1, "Max Verstappen", new SectorTimes(22.341, 24.512, 23.381));
        LapResult leclerc = result(16, "Charles Leclerc", new SectorTimes(22.512, 24.201, 23.699));

        assertEquals(verstappen, comparisons.mejorEn(
                List.of(verstappen, leclerc), TrackSector.SECTOR_1).orElseThrow());
        assertEquals(leclerc, comparisons.mejorEn(
                List.of(verstappen, leclerc), TrackSector.SECTOR_2).orElseThrow());
        assertEquals(verstappen, comparisons.mejorEn(
                List.of(verstappen, leclerc), TrackSector.SECTOR_3).orElseThrow());
    }

    @Test
    void ignoresInvalidAndLegacyResultsWithoutPartials() {
        LapResult invalid = result(1, "Invalid", new SectorTimes(1, 1, 1));
        invalid.setEstadoVuelta(LapStatus.INVALID);
        LapResult legacy = new LapResult(2, "Legacy", "Team", "Car", 70);

        assertTrue(comparisons.mejorEn(
                List.of(invalid, legacy), TrackSector.SECTOR_1).isEmpty());
    }

    private LapResult result(int driverId, String driver, SectorTimes times) {
        LapResult result = new LapResult(driverId, driver, "Team", "Car", times.tiempoTotal());
        result.setEstadoVuelta(LapStatus.VALID);
        result.setSectorTimes(times);
        return result;
    }
}
