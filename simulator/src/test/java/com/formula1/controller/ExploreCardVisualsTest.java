package com.formula1.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExploreCardVisualsTest {

    @Test
    void convierteMapaSvgDeWikimediaAImagenCompatibleConJavaFx() {
        assertEquals(
                "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/mapa.svg/640px-mapa.svg.png",
                ExploreCardVisuals.fuenteCompatible(
                        "https://upload.wikimedia.org/wikipedia/commons/4/4e/mapa.svg"));
        assertNull(ExploreCardVisuals.fuenteCompatible("  "));
    }
}
