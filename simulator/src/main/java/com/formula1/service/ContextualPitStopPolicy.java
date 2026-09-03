package com.formula1.service;

import com.formula1.model.EventType;
import com.formula1.model.LapResult;
import com.formula1.model.PitStopDecision;
import com.formula1.model.PitStopReason;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** Decide una parada por incidencias y desgaste observados en la vuelta. */
public final class ContextualPitStopPolicy implements PitStopPolicy {

    private static final double UMBRAL_DESGASTE_PORCENTAJE = 12.0;
    private static final Set<EventType> RIESGOS_MECANICOS = EnumSet.of(
            EventType.BRAKE_OVERHEATING,
            EventType.ENGINE_TEMPERATURE_HIGH,
            EventType.MINOR_MECHANICAL_ISSUE,
            EventType.POWER_UNIT_DERATING);
    private static final Set<EventType> RIESGOS_NEUMATICOS = EnumSet.of(
            EventType.TYRE_OVERHEATING,
            EventType.TYRE_TOO_COLD);
    private static final Set<EventType> CAMBIOS_CLIMATICOS = EnumSet.of(
            EventType.RAIN_STARTS,
            EventType.RAIN_INTENSIFIES,
            EventType.RAIN_STOPS,
            EventType.TRACK_DRYING);

    @Override
    public Optional<PitStopDecision> evaluate(
            LapResult result, int segment, int totalSegments) {
        if (result == null || !result.isVueltaValida()
                || segment < 1 || totalSegments < 1) {
            return Optional.empty();
        }
        if (containsAny(result, RIESGOS_MECANICOS)) {
            return decision(result, PitStopReason.MECHANICAL_RISK);
        }
        if (containsAny(result, RIESGOS_NEUMATICOS)) {
            return decision(result, PitStopReason.TYRE_CONDITION);
        }
        if (containsAny(result, CAMBIOS_CLIMATICOS)) {
            return decision(result, PitStopReason.WEATHER_CHANGE);
        }
        return result.getDesgasteEstimado() >= UMBRAL_DESGASTE_PORCENTAJE
                ? decision(result, PitStopReason.EXCESSIVE_WEAR)
                : Optional.empty();
    }

    private boolean containsAny(LapResult result, Set<EventType> types) {
        return result.getEventos().stream().anyMatch(event -> types.contains(event.tipo()));
    }

    private Optional<PitStopDecision> decision(LapResult result, PitStopReason reason) {
        return Optional.of(new PitStopDecision(result.getPilotoId(), reason));
    }
}
