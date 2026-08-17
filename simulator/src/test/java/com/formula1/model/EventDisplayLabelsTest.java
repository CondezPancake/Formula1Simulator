package com.formula1.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EventDisplayLabelsTest {

    @Test
    void allUserFacingEventLabelsAreSpanishWithoutChangingStableIdentifiers() throws Exception {
        Map<EventType, String> labels = Map.ofEntries(
                entry(EventType.NO_EVENT, "Sin evento"),
                entry(EventType.PERFECT_LAP, "Vuelta perfecta"),
                entry(EventType.CLEAN_AIR, "Aire limpio"),
                entry(EventType.SLIPSTREAM, "Rebufo"),
                entry(EventType.TRACK_EVOLUTION_ADVANTAGE, "Ventaja por evolución de pista"),
                entry(EventType.STRONG_SECTOR, "Sector sobresaliente"),
                entry(EventType.TRAFFIC, "Tráfico"),
                entry(EventType.DRIVER_MISTAKE, "Error del piloto"),
                entry(EventType.LOCK_UP, "Bloqueo de neumáticos"),
                entry(EventType.WHEELSPIN, "Patinaje de ruedas"),
                entry(EventType.WIDE_CORNER, "Salida amplia en curva"),
                entry(EventType.OVERSTEER, "Sobreviraje"),
                entry(EventType.UNDERSTEER, "Subviraje"),
                entry(EventType.HEAVY_TRAFFIC, "Tráfico intenso"),
                entry(EventType.TYRE_OVERHEATING, "Sobrecalentamiento de neumáticos"),
                entry(EventType.TYRE_TOO_COLD, "Neumáticos demasiado fríos"),
                entry(EventType.BRAKE_OVERHEATING, "Sobrecalentamiento de frenos"),
                entry(EventType.ENGINE_TEMPERATURE_HIGH, "Temperatura del motor elevada"),
                entry(EventType.MINOR_MECHANICAL_ISSUE, "Problema mecánico menor"),
                entry(EventType.POWER_UNIT_DERATING, "Reducción de potencia"),
                entry(EventType.YELLOW_FLAG, "Bandera amarilla"),
                entry(EventType.LOCAL_YELLOW_FLAG, "Bandera amarilla local"),
                entry(EventType.RAIN_STARTS, "Comienza la lluvia"),
                entry(EventType.RAIN_INTENSIFIES, "La lluvia se intensifica"),
                entry(EventType.RAIN_STOPS, "Deja de llover"),
                entry(EventType.TRACK_DRYING, "La pista se está secando"),
                entry(EventType.WIND_GUST, "Ráfaga de viento"),
                entry(EventType.RED_FLAG, "Bandera roja"),
                entry(EventType.CRASH, "Accidente"));

        assertEquals(EventType.values().length, labels.size());
        labels.forEach((type, label) -> {
            assertEquals(label, type.getEtiqueta());
            assertFalse(label.contains("_"));
        });
        assertEquals("Negativo leve", EventCategory.MINOR_NEGATIVE.getEtiqueta());
        assertEquals("Negativo importante", EventCategory.MAJOR_NEGATIVE.getEtiqueta());
        assertEquals("Clima o pista", EventCategory.WEATHER_TRACK.getEtiqueta());
        assertEquals("Individual", EventScope.INDIVIDUAL.getEtiqueta());
        assertEquals("Global", EventScope.GLOBAL.getEtiqueta());

        // La etiqueta cambia, pero el identificador persistido sigue estable.
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("\"DRIVER_MISTAKE\"",
                mapper.writeValueAsString(EventType.DRIVER_MISTAKE));
        assertEquals(EventType.DRIVER_MISTAKE,
                mapper.readValue("\"DRIVER_MISTAKE\"", EventType.class));
    }
}
