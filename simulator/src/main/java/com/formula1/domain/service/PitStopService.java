package com.formula1.domain.service;

import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.PitStopDecision;
import com.formula1.domain.model.PitStopPhase;
import com.formula1.domain.model.PitStopRecord;
import com.formula1.domain.model.PitStopReason;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Gestiona exclusivamente el ciclo y el coste deportivo de las paradas.
 *
 * <p>El motor consulta la penalización acumulada; la interfaz observa estados
 * inmutables. Así este servicio no depende de JavaFX, radio ni neumáticos.</p>
 */
public final class PitStopService {

    private static final int SEGMENTOS_PARA_COMPLETAR = 3;
    // La sesión representa una sola vuelta, no una carrera completa. Se usa
    // una pérdida escalada: el tránsito no puede consumir 15,5 s de una vuelta.
    private static final double PERDIDA_ENTRADA_SEGUNDOS = 0.45;
    private static final double PERDIDA_SALIDA_SEGUNDOS = 0.55;
    private static final double DETENCION_MINIMA_SEGUNDOS = 1.8;
    private static final double DETENCION_MAXIMA_SEGUNDOS = 2.8;

    private final RandomGenerator random;
    private final Map<Integer, PitStopState> estados = new LinkedHashMap<>();
    private final Map<String, PitStopRecord> historial = new LinkedHashMap<>();

    public PitStopService() {
        this(new Random());
    }

    PitStopService(RandomGenerator random) {
        this.random = random;
    }

    public void startSession() {
        estados.clear();
        historial.clear();
    }

    /**
     * Acepta una única parada por piloto y solo si quedan segmentos para
     * representar entrada, detención, salida y finalización.
     */
    public boolean start(PitStopDecision decision, List<LapResult> classification,
                         int segment, int totalSegments) {
        if (decision == null || estados.containsKey(decision.pilotoId())
                || segment < 1 || segment > totalSegments - SEGMENTOS_PARA_COMPLETAR) {
            return false;
        }
        Optional<LapResult> result = classification.stream()
                .filter(item -> item.getPilotoId() == decision.pilotoId())
                .filter(LapResult::isVueltaValida)
                .findFirst();
        if (result.isEmpty()) {
            return false;
        }

        LapResult driver = result.get();
        double stoppedSeconds = random.nextDouble(
                DETENCION_MINIMA_SEGUNDOS, DETENCION_MAXIMA_SEGUNDOS);
        estados.put(decision.pilotoId(), new PitStopState(
                UUID.randomUUID().toString(), driver.getPilotoId(), driver.getPiloto(),
                segment, driver.getPosicion(), decision.motivo(), stoppedSeconds));
        return true;
    }

    public boolean hasStop(int driverId) {
        return estados.containsKey(driverId);
    }

    /** Avanza las fases sin conocer cómo se representa la clasificación. */
    public void advance(int segment) {
        estados.values().forEach(state -> state.advance(segment));
    }

    /** Penalización que el motor suma al tiempo acumulado del piloto. */
    public double timeLossFor(int driverId) {
        PitStopState state = estados.get(driverId);
        return state == null ? 0 : state.timeLossSeconds();
    }

    /**
     * Consolida la posición después de reordenar y publica solo cambios de fase.
     */
    public List<PitStopRecord> collectUpdates(List<LapResult> classification) {
        List<PitStopRecord> updates = new ArrayList<>();
        for (PitStopState state : estados.values()) {
            if (!state.isDirty()) {
                continue;
            }
            int currentPosition = classification.stream()
                    .filter(item -> item.getPilotoId() == state.driverId())
                    .mapToInt(LapResult::getPosicion)
                    .findFirst()
                    .orElse(state.entryPosition());
            PitStopRecord record = state.snapshot(currentPosition);
            historial.put(record.id(), record);
            updates.add(record);
            state.markPublished();
        }
        return List.copyOf(updates);
    }

    public List<PitStopRecord> history() {
        return List.copyOf(historial.values());
    }

    /** Sincroniza el registro final con la última parrilla sin emitir otra fase. */
    public void updateFinalPositions(List<LapResult> classification) {
        historial.replaceAll((id, record) -> classification.stream()
                .filter(result -> result.getPilotoId() == record.pilotoId())
                .map(result -> record.conPosicionActual(result.getPosicion()))
                .findFirst()
                .orElse(record));
    }

    private static final class PitStopState {

        private final String id;
        private final int driverId;
        private final String driver;
        private final int entrySegment;
        private final int entryPosition;
        private final PitStopReason reason;
        private final double stoppedSeconds;
        private int currentSegment;
        private PitStopPhase phase;
        private double timeLossSeconds;
        private boolean dirty;

        private PitStopState(String id, int driverId, String driver, int entrySegment,
                             int entryPosition, PitStopReason reason, double stoppedSeconds) {
            this.id = id;
            this.driverId = driverId;
            this.driver = driver;
            this.entrySegment = entrySegment;
            this.entryPosition = entryPosition;
            this.reason = reason;
            this.stoppedSeconds = stoppedSeconds;
            this.currentSegment = entrySegment;
            this.phase = PitStopPhase.ENTERING;
            this.dirty = true;
        }

        private void advance(int segment) {
            if (segment < entrySegment) {
                return;
            }
            int offset = segment - entrySegment;
            PitStopPhase nextPhase;
            double nextTimeLoss;
            if (offset == 0) {
                nextPhase = PitStopPhase.ENTERING;
                nextTimeLoss = PERDIDA_ENTRADA_SEGUNDOS;
            } else if (offset == 1) {
                nextPhase = PitStopPhase.STOPPED;
                nextTimeLoss = PERDIDA_ENTRADA_SEGUNDOS + stoppedSeconds;
            } else if (offset == 2) {
                nextPhase = PitStopPhase.EXITING;
                nextTimeLoss = PERDIDA_ENTRADA_SEGUNDOS + stoppedSeconds
                        + PERDIDA_SALIDA_SEGUNDOS;
            } else {
                nextPhase = PitStopPhase.COMPLETED;
                nextTimeLoss = PERDIDA_ENTRADA_SEGUNDOS + stoppedSeconds
                        + PERDIDA_SALIDA_SEGUNDOS;
            }
            if (phase != nextPhase || Double.compare(timeLossSeconds, nextTimeLoss) != 0) {
                phase = nextPhase;
                timeLossSeconds = nextTimeLoss;
                currentSegment = segment;
                dirty = true;
            }
        }

        private PitStopRecord snapshot(int currentPosition) {
            return new PitStopRecord(id, driverId, driver, 1, entrySegment,
                    currentSegment, phase, reason, stoppedSeconds, timeLossSeconds,
                    entryPosition, currentPosition);
        }

        private int driverId() {
            return driverId;
        }

        private int entryPosition() {
            return entryPosition;
        }

        private double timeLossSeconds() {
            return timeLossSeconds;
        }

        private boolean isDirty() {
            return dirty;
        }

        private void markPublished() {
            dirty = false;
        }
    }
}
