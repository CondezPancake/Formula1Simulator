package com.formula1.model;

/** Bandera activa como consecuencia de un incidente. */
public enum TrackFlag {
    GREEN("Pista libre"),
    LOCAL_YELLOW("Bandera amarilla local"),
    YELLOW("Bandera amarilla"),
    RED("Bandera roja");

    private final String etiqueta;

    TrackFlag(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
