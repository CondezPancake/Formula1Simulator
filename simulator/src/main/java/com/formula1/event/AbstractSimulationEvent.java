package com.formula1.event;

import com.formula1.model.EventImpact;
import com.formula1.model.EventType;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/** Base común que compone compatibilidad, ponderación e impacto. */
abstract class AbstractSimulationEvent implements SimulationEvent {

    private final EventType type;
    private final Predicate<EventContext> compatibility;
    private final ToDoubleFunction<EventContext> weightModifier;
    private final ImpactGenerator impactGenerator;

    AbstractSimulationEvent(EventType type,
                            Predicate<EventContext> compatibility,
                            ToDoubleFunction<EventContext> weightModifier,
                            ImpactGenerator impactGenerator) {
        this.type = type;
        this.compatibility = compatibility;
        this.weightModifier = weightModifier;
        this.impactGenerator = impactGenerator;
    }

    @Override
    public EventType type() {
        return type;
    }

    @Override
    public boolean isCompatible(EventContext context) {
        return compatibility.test(context);
    }

    @Override
    public double weight(EventContext context) {
        return Math.max(0, type.getPesoBase() * weightModifier.applyAsDouble(context));
    }

    @Override
    public EventImpact createImpact(EventContext context, RandomGenerator random) {
        return impactGenerator.generate(context, random);
    }
}
