package com.formula1.event;

import com.formula1.model.EventImpact;
import com.formula1.model.EventType;

import java.util.random.RandomGenerator;

/** Contrato polimórfico para eventos individuales, globales e incidentes. */
public interface SimulationEvent {

    EventType type();

    boolean isCompatible(EventContext context);

    double weight(EventContext context);

    EventImpact createImpact(EventContext context, RandomGenerator random);
}
