package com.formula1.model;

import java.util.List;

/**
 * Foto de la clasificación durante la reproducción en vivo.
 *
 * <p>Los fotogramas de presentación son independientes de los microsectores
 * del dominio. De ese modo la interfaz puede refrescarse con fluidez sin
 * multiplicar el clima, los eventos, las paradas o los datos persistidos.</p>
 */
public record LiveClassificationFrame(
        int frame,
        int totalFrames,
        int segment,
        int totalSegments,
        double progress,
        List<LapResult> classification) {

    public LiveClassificationFrame {
        if (frame < 1 || totalFrames < 1 || frame > totalFrames) {
            throw new IllegalArgumentException("El fotograma debe pertenecer a la reproducción");
        }
        if (segment < 1 || totalSegments < 1 || segment > totalSegments) {
            throw new IllegalArgumentException("El segmento debe pertenecer a la vuelta");
        }
        if (!Double.isFinite(progress) || progress <= 0 || progress > 1) {
            throw new IllegalArgumentException("El progreso debe estar entre 0 y 1");
        }
        classification = classification == null ? List.of() : List.copyOf(classification);
    }
}
