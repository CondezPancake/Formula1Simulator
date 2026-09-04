package com.formula1.domain.event;

import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/** Selección por ruleta ponderada; los candidatos con peso cero se ignoran. */
public final class WeightedEventSelector {

    public <T> Optional<T> select(List<T> candidates,
                                  ToDoubleFunction<T> weight,
                                  RandomGenerator random) {
        double total = candidates.stream()
                .mapToDouble(candidate -> Math.max(0, weight.applyAsDouble(candidate)))
                .sum();
        if (total <= 0) {
            return Optional.empty();
        }

        double draw = random.nextDouble() * total;
        double accumulated = 0;
        for (T candidate : candidates) {
            accumulated += Math.max(0, weight.applyAsDouble(candidate));
            if (draw < accumulated) {
                return Optional.of(candidate);
            }
        }
        return Optional.of(candidates.get(candidates.size() - 1));
    }
}
