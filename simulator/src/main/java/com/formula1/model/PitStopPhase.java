package com.formula1.model;

/** Etapas observables de una parada; HU-51 podrá añadir el trabajo realizado. */
public enum PitStopPhase {
    ENTERING("Entrada a boxes"),
    STOPPED("Detenido en boxes"),
    EXITING("Salida de boxes"),
    COMPLETED("Parada completada");

    private final String etiqueta;

    PitStopPhase(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
