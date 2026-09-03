package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Estado inmutable y persistible de una parada en boxes.
 *
 * <p>No contiene neumáticos ni mensajes de radio: esas responsabilidades se
 * incorporarán sobre este contrato en HU-51 y HU-49 respectivamente.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PitStopRecord(
        String id,
        int pilotoId,
        String piloto,
        int vuelta,
        int segmentoEntrada,
        int segmentoActual,
        PitStopPhase fase,
        PitStopReason motivo,
        double tiempoDetenidoSegundos,
        double tiempoPerdidoSegundos,
        int posicionEntrada,
        int posicionActual) {

    public PitStopRecord {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador de la parada es obligatorio");
        }
        if (pilotoId <= 0 || piloto == null || piloto.isBlank()) {
            throw new IllegalArgumentException("El piloto de la parada es obligatorio");
        }
        if (vuelta < 1 || segmentoEntrada < 1 || segmentoActual < segmentoEntrada) {
            throw new IllegalArgumentException("La vuelta y los segmentos deben ser válidos");
        }
        if (fase == null) {
            throw new IllegalArgumentException("La fase de la parada es obligatoria");
        }
        if (motivo == null) {
            throw new IllegalArgumentException("El motivo de la parada es obligatorio");
        }
        requireNonNegative(tiempoDetenidoSegundos, "tiempo detenido");
        requireNonNegative(tiempoPerdidoSegundos, "tiempo perdido");
        if (posicionEntrada < 1 || posicionActual < 1) {
            throw new IllegalArgumentException("Las posiciones deben ser positivas");
        }
    }

    public int posicionesPerdidas() {
        return Math.max(0, posicionActual - posicionEntrada);
    }

    public PitStopRecord conPosicionActual(int position) {
        return new PitStopRecord(id, pilotoId, piloto, vuelta, segmentoEntrada,
                segmentoActual, fase, motivo, tiempoDetenidoSegundos,
                tiempoPerdidoSegundos, posicionEntrada, position);
    }

    private static void requireNonNegative(double value, String field) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("Valor inválido para " + field);
        }
    }
}
