package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Evento resuelto y persistible, incluida su magnitud aleatoria. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventOccurrence(
        EventType tipo,
        int pilotoId,
        String piloto,
        int vuelta,
        TrackSector sector,
        EventImpact impacto) {

    public EventOccurrence {
        if (tipo == null || sector == null || impacto == null) {
            throw new IllegalArgumentException("El evento, sector e impacto son obligatorios");
        }
        if (vuelta < 1) {
            throw new IllegalArgumentException("La vuelta del evento debe ser positiva");
        }
        if (tipo != EventType.NO_EVENT && (piloto == null || piloto.isBlank())) {
            throw new IllegalArgumentException("El piloto del evento es obligatorio");
        }
    }

    public static EventOccurrence noEvent(int pilotoId, String piloto, int vuelta) {
        return new EventOccurrence(EventType.NO_EVENT, pilotoId, piloto, vuelta,
                TrackSector.NONE, EventImpact.none());
    }

    public boolean ocurrio() {
        return tipo != EventType.NO_EVENT;
    }

    public EventCategory categoria() {
        return tipo.getCategoria();
    }

    public EventScope alcance() {
        return tipo.getAlcance();
    }

    public String resumen() {
        if (!ocurrio()) {
            return tipo.getEtiqueta();
        }
        return tipo.getEtiqueta() + " · " + sector.getEtiqueta();
    }
}
