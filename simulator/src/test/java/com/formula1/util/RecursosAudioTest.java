package com.formula1.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Los ficheros de audio que la aplicación pide existen de verdad.
 *
 * No es una comprobación de adorno: la intro estuvo muda mucho tiempo porque
 * apuntaba a {@code sound-intro.mp3} e {@code intro-f1.mp3}, que nunca se
 * llegaron a empaquetar. Como {@code AudioManager} degrada a silencio a
 * propósito —para que un fichero ausente no tumbe el arranque—, una ruta mal
 * escrita no da ningún error: simplemente no se oye nada. Esto lo caza.
 */
class RecursosAudioTest {

    private void existe(String ruta) {
        assertNotNull(getClass().getResource(ruta),
                "falta el recurso de audio " + ruta + ": la app lo pide y quedaría en silencio");
    }

    @Test
    void laIntroTieneSuSonido() {
        existe("/audio/intro-sound.mp3");
    }

    @Test
    void elMenuTieneSusDosEfectos() {
        existe("/audio/sound1.mp3");
        existe("/audio/sound2.mp3");
    }
}
