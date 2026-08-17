package com.formula1.event;

import com.formula1.model.EventImpact;

import java.util.random.RandomGenerator;

@FunctionalInterface
interface ImpactGenerator {
    EventImpact generate(EventContext context, RandomGenerator random);
}
