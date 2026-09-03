package com.formula1.domain.service;

import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.PitStopPhase;
import com.formula1.domain.model.PitStopRecord;
import com.formula1.domain.model.TireChangeRecord;
import com.formula1.domain.model.TireCompound;
import com.formula1.util.MathUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mantiene compuestos, registra cambios y calcula su efecto deportivo. */
public final class TireStrategyService {

    public static final TireCompound INITIAL_COMPOUND = TireCompound.MEDIUM;

    private final TireCompoundPolicy policy;
    private final Map<Integer, TireCompound> initialCompounds = new LinkedHashMap<>();
    private final Map<Integer, TireCompound> compounds = new LinkedHashMap<>();
    private final Map<String, TireChangeRecord> changesByStop = new LinkedHashMap<>();

    public TireStrategyService() {
        this(new ContextualTireCompoundPolicy());
    }

    TireStrategyService(TireCompoundPolicy policy) {
        this.policy = policy;
    }

    public void startSession() {
        initialCompounds.clear();
        compounds.clear();
        changesByStop.clear();
    }

    /** Aplica la elección inicial únicamente al piloto configurado. */
    public void startSession(int configuredDriverId, TireCompound initialCompound) {
        startSession();
        TireCompound selected = initialCompound == null
                ? INITIAL_COMPOUND : initialCompound;
        initialCompounds.put(configuredDriverId, selected);
        compounds.put(configuredDriverId, selected);
    }

    /** Ejecuta una sola vez el cambio asociado a la fase detenida. */
    public Optional<TireChangeRecord> changeDuring(
            PitStopRecord stop, LapResult context) {
        if (stop == null || context == null || stop.fase() != PitStopPhase.STOPPED
                || changesByStop.containsKey(stop.id())) {
            return Optional.empty();
        }
        TireCompound current = compoundFor(stop.pilotoId());
        TireCompound target = policy.select(current, stop, context);
        return change(stop, target);
    }

    /** Permite cualquier transición válida S↔M↔H sin acoplarla a la política. */
    public Optional<TireChangeRecord> change(
            PitStopRecord stop, TireCompound target) {
        if (stop == null || stop.fase() != PitStopPhase.STOPPED
                || target == null || changesByStop.containsKey(stop.id())) {
            return Optional.empty();
        }
        TireCompound current = compoundFor(stop.pilotoId());
        if (current == target) {
            return Optional.empty();
        }
        TireChangeRecord change = new TireChangeRecord(
                stop.id(), stop.pilotoId(), stop.piloto(), stop.vuelta(),
                stop.segmentoActual(), current, target, stop.motivo());
        compounds.put(stop.pilotoId(), target);
        changesByStop.put(stop.id(), change);
        return Optional.of(change);
    }

    public TireCompound compoundFor(int driverId) {
        return compounds.getOrDefault(driverId, INITIAL_COMPOUND);
    }

    /** Ajuste acumulado del ritmo desde que se montó el compuesto nuevo. */
    public double timeAdjustmentFor(
            int driverId, double baseLapTime, int segment, int totalSegments) {
        List<TireChangeRecord> changes = changesForDriver(driverId);
        double adjustment = 0;
        TireCompound activeCompound = initialCompoundFor(driverId);
        int stintStart = 0;
        for (TireChangeRecord change : changes) {
            int stintEnd = Math.min(segment, change.segmento());
            int activeSegments = Math.max(0, stintEnd - stintStart);
            adjustment += adjustmentFor(
                    activeCompound, baseLapTime, activeSegments, totalSegments);
            if (segment <= change.segmento()) {
                return adjustment;
            }
            activeCompound = change.nuevo();
            stintStart = change.segmento();
        }
        int activeSegments = Math.max(0, segment - stintStart);
        return adjustment + adjustmentFor(
                activeCompound, baseLapTime, activeSegments, totalSegments);
    }

    private double adjustmentFor(TireCompound compound, double baseLapTime,
                                 int activeSegments, int totalSegments) {
        return baseLapTime * (compound.getFactorTiempo() - 1)
                * activeSegments / totalSegments;
    }

    /** Desgaste del juego actualmente montado; una parada estrena neumáticos. */
    public double wearFor(
            int driverId, double baseLapWear, int segment, int totalSegments) {
        TireChangeRecord change = changeForDriver(driverId).orElse(null);
        if (change == null) {
            return MathUtils.clamp(baseLapWear
                    * initialCompoundFor(driverId).getFactorDesgaste()
                    * segment / (double) totalSegments, 0, 100);
        }
        int segmentsOnNewTires = Math.max(0, segment - change.segmento());
        return MathUtils.clamp(baseLapWear * change.nuevo().getFactorDesgaste()
                * segmentsOnNewTires / totalSegments, 0, 100);
    }

    public List<TireChangeRecord> history() {
        return List.copyOf(changesByStop.values());
    }

    private Optional<TireChangeRecord> changeForDriver(int driverId) {
        return changesForDriver(driverId).stream()
                .reduce((first, second) -> second);
    }

    private TireCompound initialCompoundFor(int driverId) {
        return initialCompounds.getOrDefault(driverId, INITIAL_COMPOUND);
    }

    private List<TireChangeRecord> changesForDriver(int driverId) {
        return changesByStop.values().stream()
                .filter(change -> change.pilotoId() == driverId)
                .toList();
    }
}
