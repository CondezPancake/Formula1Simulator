package com.formula1.domain.event;

import com.formula1.domain.model.EventType;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Evento que afecta principalmente al rendimiento de un piloto. */
final class PerformanceEvent extends AbstractSimulationEvent {

    PerformanceEvent(EventType type, Predicate<EventContext> compatibility,
                     ToDoubleFunction<EventContext> weightModifier,
                     ImpactGenerator impactGenerator) {
        super(type, compatibility, weightModifier, impactGenerator);
    }
}
