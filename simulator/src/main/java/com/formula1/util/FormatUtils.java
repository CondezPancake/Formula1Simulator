package com.formula1.util;

import com.formula1.model.LapResult;

import java.util.Locale;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String formatLapTime(double segundos) {
        int minutos = (int) (segundos / 60);
        double resto = segundos - (minutos * 60);
        return String.format(Locale.ROOT, "%d:%06.3f", minutos, resto);
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
        return String.format(Locale.ROOT, "+%.3f", gap);
    }

    /** Delta en vivo: negativo es mas rapido y positivo es mas lento. */
    public static String formatDelta(double delta) {
        if (Math.abs(delta) < 0.0005) {
            return "±0.000";
        }
        return String.format(Locale.ROOT, "%+.3f", delta);
    }

    public static String formatPercentage(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100);
    }

    public static String formatLapResult(LapResult result) {
        return result.isVueltaValida()
                ? formatLapTime(result.getTiempoSegundos())
                : "INVALID";
    }
}
