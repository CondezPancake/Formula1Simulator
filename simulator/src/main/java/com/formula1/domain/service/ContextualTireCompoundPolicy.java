package com.formula1.domain.service;

import com.formula1.domain.model.EventType;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.PitStopRecord;
import com.formula1.domain.model.TireCompound;

/** Elige entre S, M y H según el motivo y el evento que originó la parada. */
public final class ContextualTireCompoundPolicy implements TireCompoundPolicy {

    @Override
    public TireCompound select(
            TireCompound current, PitStopRecord stop, LapResult context) {
        TireCompound selected = switch (stop.motivo()) {
            case TYRE_CONDITION, EXCESSIVE_WEAR -> moreDurable(current);
            case MECHANICAL_RISK -> faster(current);
            case WEATHER_CHANGE -> weatherCompound(current, context);
        };
        return selected == current ? alternative(current) : selected;
    }

    private TireCompound weatherCompound(TireCompound current, LapResult context) {
        boolean drying = context.getEventos().stream().anyMatch(event ->
                event.tipo() == EventType.RAIN_STOPS
                        || event.tipo() == EventType.TRACK_DRYING);
        return drying ? TireCompound.SOFT : moreDurable(current);
    }

    private TireCompound moreDurable(TireCompound current) {
        return switch (current) {
            case SOFT -> TireCompound.MEDIUM;
            case MEDIUM -> TireCompound.HARD;
            case HARD -> TireCompound.MEDIUM;
        };
    }

    private TireCompound faster(TireCompound current) {
        return current == TireCompound.SOFT ? TireCompound.MEDIUM : TireCompound.SOFT;
    }

    private TireCompound alternative(TireCompound current) {
        return current == TireCompound.MEDIUM ? TireCompound.HARD : TireCompound.MEDIUM;
    }
}
