package com.formula1.domain.model;

/** Decisión inmutable producida por una política de estrategia. */
public record PitStopDecision(int pilotoId, PitStopReason motivo) {

    public PitStopDecision {
        if (pilotoId <= 0 || motivo == null) {
            throw new IllegalArgumentException("La decisión de boxes debe indicar piloto y motivo");
        }
    }
}
