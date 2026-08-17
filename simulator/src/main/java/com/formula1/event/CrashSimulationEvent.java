package com.formula1.event;

import com.formula1.model.EventImpact;
import com.formula1.model.EventType;
import com.formula1.model.TrackFlag;

import java.util.random.RandomGenerator;

/** Accidente excepcional con severidad variable y posible abandono. */
final class CrashSimulationEvent implements SimulationEvent {

    @Override
    public EventType type() {
        return EventType.CRASH;
    }

    @Override
    public boolean isCompatible(EventContext context) {
        return true;
    }

    @Override
    public double weight(EventContext context) {
        return type().getPesoBase() * context.multiplicadorRiesgoAccidente();
    }

    @Override
    public EventImpact createImpact(EventContext context, RandomGenerator random) {
        double severidad = random.nextDouble();
        boolean fuera = severidad >= 0.25;
        TrackFlag bandera = severidad >= 0.82 ? TrackFlag.RED : TrackFlag.YELLOW;
        return new EventImpact(0, 0,
                range(random, -14, -7), range(random, 3, 12),
                range(random, 2, 10), range(random, 0, 8), 0,
                true, fuera, bandera);
    }

    private double range(RandomGenerator random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
