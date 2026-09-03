package com.formula1.service;

import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.concurrent.locks.LockSupport;

/**
 * Reloj del motor que reparte una duración real entre sus fotogramas.
 * No conoce JavaFX y consulta la finalización manual en intervalos cortos.
 */
final class SimulationPacer {

    private static final long MAXIMA_ESPERA_NANOS = 50_000_000L;
    static final int FOTOGRAMAS_POR_SEGUNDO = 20;

    private final long duracionNanos;
    private final BooleanSupplier finalizarSolicitado;
    private final LongSupplier reloj;
    private final LongConsumer esperar;
    private final long inicioNanos;

    SimulationPacer(int duracionSegundos, BooleanSupplier finalizarSolicitado) {
        this(duracionSegundos * 1_000_000_000L, finalizarSolicitado,
                System::nanoTime, LockSupport::parkNanos);
    }

    /**
     * Cantidad de fotos que publicará la reproducción. Mantiene al menos una
     * por microsector incluso en sesiones configuradas con solo un segundo.
     */
    int totalFotogramas(int minimo) {
        long porCadencia = Math.max(1,
                (duracionNanos * FOTOGRAMAS_POR_SEGUNDO + 999_999_999L) / 1_000_000_000L);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(minimo, porCadencia));
    }

    SimulationPacer(long duracionNanos, BooleanSupplier finalizarSolicitado,
                    LongSupplier reloj, LongConsumer esperar) {
        if (duracionNanos <= 0) {
            throw new IllegalArgumentException("La duración debe ser positiva");
        }
        this.duracionNanos = duracionNanos;
        this.finalizarSolicitado = finalizarSolicitado == null ? () -> false : finalizarSolicitado;
        this.reloj = reloj;
        this.esperar = esperar;
        this.inicioNanos = reloj.getAsLong();
    }

    /** Espera hasta el instante asignado al fotograma o sale al pedir finalizar. */
    boolean completarFotograma(int fotograma, int totalFotogramas) {
        if (fotograma < 1 || fotograma > totalFotogramas) {
            throw new IllegalArgumentException("Fotograma fuera de la simulación");
        }
        long objetivo = inicioNanos + duracionNanos * fotograma / totalFotogramas;
        while (!finalizarSolicitado.getAsBoolean()) {
            long restante = objetivo - reloj.getAsLong();
            if (restante <= 0) {
                return true;
            }
            esperar.accept(Math.min(restante, MAXIMA_ESPERA_NANOS));
        }
        return false;
    }
}
