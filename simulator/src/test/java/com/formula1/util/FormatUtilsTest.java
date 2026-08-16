package com.formula1.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatUtilsTest {

    @Test
    void parseaUnTiempoConMinutos() {
        assertEquals(70.166, FormatUtils.parseLapTime("1:10.166"), 0.0001);
        assertEquals(81.046, FormatUtils.parseLapTime("1:21.046"), 0.0001);
    }

    @Test
    void parseaUnTiempoSinMinutos() {
        assertEquals(45.5, FormatUtils.parseLapTime("45.5"), 0.0001);
    }

    @Test
    void devuelveCeroAnteEntradaVacia() {
        assertEquals(0.0, FormatUtils.parseLapTime(null), 0.0001);
        assertEquals(0.0, FormatUtils.parseLapTime("  "), 0.0001);
    }

    @Test
    void formateaYParseaSonInversas() {
        double original = 83.421;

        assertEquals(original, FormatUtils.parseLapTime(FormatUtils.formatLapTime(original)), 0.0005);
    }

    @Test
    void formateaElGapRespectoALaPole() {
        assertEquals("+0.412", FormatUtils.formatGap(0.412));
        assertEquals("—", FormatUtils.formatGap(0.0));
    }
}
