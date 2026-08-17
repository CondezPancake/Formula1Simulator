package com.formula1.event;

import com.formula1.model.EventType;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Evento global que afecta clima, banderas o estado de pista. */
final class TrackSimulationEvent extends AbstractSimulationEvent {

    TrackSimulationEvent(EventType type, Predicate<EventContext> compatibility,
                         ToDoubleFunction<EventContext> weightModifier,
                         ImpactGenerator impactGenerator) {
        super(type, compatibility, weightModifier, impactGenerator);
    }
}
