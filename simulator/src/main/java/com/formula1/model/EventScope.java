package com.formula1.model;

/** Alcance del evento: un piloto concreto o el estado general de pista. */
public enum EventScope {
    INDIVIDUAL("Individual"),
    GLOBAL("Global");

    private final String etiqueta;

    EventScope(String etiqueta) {
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
