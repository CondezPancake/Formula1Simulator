package com.formula1.util;

import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern PERSONA = Pattern.compile("[\\p{L} .''’\\-]+");
    private static final Pattern IDENTIFICADOR = Pattern.compile("[\\p{L}\\p{N} .,'’&()/_+\\-]+");

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

    public static boolean isPersonName(String value, int maxLength) {
        return isNotBlank(value) && value.trim().length() <= maxLength
                && PERSONA.matcher(value.trim()).matches();
    }

    public static boolean isIdentifier(String value, int maxLength) {
        return isNotBlank(value) && value.trim().length() <= maxLength
                && IDENTIFICADOR.matcher(value.trim()).matches();
    }

    public static boolean hasLength(String value, int min, int max) {
        if (value == null) return false;
        int length = value.trim().length();
        return length >= min && length <= max;
    }
}
