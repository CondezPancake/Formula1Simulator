package com.formula1.util;

import java.util.Collection;

public final class MathUtils {

    private MathUtils() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double average(Collection<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static double percentageOf(double part, double total) {
        if (total == 0) {
            return 0.0;
        }
        return (part / total) * 100.0;
    }
}
