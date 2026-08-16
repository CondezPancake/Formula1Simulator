package com.formula1.util;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String formatLapTime(double segundos) {
        int minutos = (int) (segundos / 60);
        double resto = segundos - (minutos * 60);
        return String.format("%d:%06.3f", minutos, resto);
    }

    public static String formatPercentage(double value) {
        return String.format("%.0f%%", value * 100);
    }
}
