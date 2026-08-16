package com.formula1.util;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean isPositive(double value) {
        return value > 0;
    }

    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
}
