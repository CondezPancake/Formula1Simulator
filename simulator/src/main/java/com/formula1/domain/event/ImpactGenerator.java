package com.formula1.domain.event;

import com.formula1.domain.model.EventImpact;

import java.util.random.RandomGenerator;

@FunctionalInterface
interface ImpactGenerator {
    EventImpact generate(EventContext context, RandomGenerator random);
}
