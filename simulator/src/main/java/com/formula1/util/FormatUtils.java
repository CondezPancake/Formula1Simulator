package com.formula1.util;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String formatLapTime(double segundos) {
        int minutos = (int) (segundos / 60);
        double resto = segundos - (minutos * 60);
        return String.format("%d:%06.3f", minutos, resto);
    }

    /**
     * Inversa de {@link #formatLapTime(double)}: convierte "1:10.166" en
     * 70.166. Acepta también un valor en segundos sin minutos ("70.166").
     */
    public static double parseLapTime(String tiempo) {
        if (tiempo == null || tiempo.isBlank()) {
            return 0.0;
        }
        String valor = tiempo.trim();
        int separador = valor.indexOf(':');
        if (separador < 0) {
            return Double.parseDouble(valor);
        }
        int minutos = Integer.parseInt(valor.substring(0, separador));
        double segundos = Double.parseDouble(valor.substring(separador + 1));
        return minutos * 60 + segundos;
    }

    /** Diferencia respecto a la pole, con el formato "+0.412". */
    public static String formatGap(double gap) {
        if (gap <= 0) {
            return "—";
        }
        return String.format("+%.3f", gap);
    }

    public static String formatPercentage(double value) {
        return String.format("%.0f%%", value * 100);
    }
}
