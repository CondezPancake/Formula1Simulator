package com.formula1.domain.model;

/** Grupos configurables usados en la primera etapa de selección de eventos. */
public enum EventCategory {
    NO_EVENT("Sin evento"),
    POSITIVE("Positivo"),
    MINOR_NEGATIVE("Negativo leve"),
    MAJOR_NEGATIVE("Negativo importante"),
    WEATHER_TRACK("Clima o pista"),
    EXCEPTIONAL("Excepcional");

    private final String etiqueta;

    EventCategory(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
