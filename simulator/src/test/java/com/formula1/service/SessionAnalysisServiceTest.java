package com.formula1.service;

import com.formula1.model.EventImpact;
import com.formula1.model.EventOccurrence;
import com.formula1.model.EventType;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SectorTimes;
import com.formula1.model.SessionAnalysis;
import com.formula1.model.SimulationConfig;
import com.formula1.model.TrackFlag;
import com.formula1.model.TrackSector;
import com.formula1.model.WeatherCondition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionAnalysisServiceTest {

    private final SessionAnalysisService service = new SessionAnalysisService();

    @Test
    void generaAnalisisDePoleSectoresYEventos() {
        EventOccurrence rebufo = new EventOccurrence(EventType.SLIPSTREAM, 1,
                "Max Verstappen", 1, TrackSector.SECTOR_2,
                new EventImpact(-0.120, 1.0, 0, 0, 0, 0,
                        0, false, false, TrackFlag.GREEN));
        LapResult pole = resultado(1, "Max Verstappen", 70.000,
                new SectorTimes(22.000, 24.000, 24.000));
        pole.setEventos(List.of(rebufo));
        LapResult rival = resultado(16, "Charles Leclerc", 70.320,
                new SectorTimes(22.180, 23.850, 24.290));

        QualifyingSession sesion = new QualifyingSession(
                "Circuito de Monza", WeatherCondition.SECO, new SimulationConfig());
        sesion.setResultados(List.of(pole, rival));

        SessionAnalysis analisis = service.analizar(sesion);

        assertEquals("Max Verstappen", analisis.pilotoPole());
        assertTrue(analisis.resumen().contains("Max Verstappen"));
        assertTrue(analisis.factoresPositivos().stream()
                .anyMatch(texto -> texto.contains("Mejor rendimiento en Sector 1")));
        assertTrue(analisis.factoresPositivos().stream()
                .anyMatch(texto -> texto.contains("Rebufo")));
        assertTrue(analisis.factoresNegativos().stream()
                .anyMatch(texto -> texto.contains("Sector 2")));
    }

    @Test
    void sesionSinVueltasValidasDevuelveAnalisisVacio() {
        QualifyingSession sesion = new QualifyingSession(
                "Circuito de Monza", WeatherCondition.SECO, new SimulationConfig());

        SessionAnalysis analisis = service.analizar(sesion);

        assertEquals("Sin pole", analisis.pilotoPole());
        assertTrue(analisis.factoresNegativos().stream()
                .anyMatch(texto -> texto.contains("invalidadas")));
    }

    @Test
    void rechazaSesionNula() {
        assertThrows(ValidationException.class, () -> service.analizar(null));
    }

    private LapResult resultado(int pilotoId, String piloto, double tiempo, SectorTimes sectores) {
        LapResult result = new LapResult(pilotoId, piloto, "Equipo", "Auto", tiempo);
        result.setConsumoEstimado(2.5 + pilotoId * 0.01);
        result.setDesgasteEstimado(3.0 + pilotoId * 0.01);
        result.setSectorTimes(sectores);
        return result;
    }
}
