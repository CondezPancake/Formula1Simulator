package com.formula1.domain.model;

/** Sector de pista asociado a un evento. */
public enum TrackSector {
    NONE("—"),
    SECTOR_1("Sector 1"),
    SECTOR_2("Sector 2"),
    SECTOR_3("Sector 3");

    private final String etiqueta;

    TrackSector(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean contiene(int segmento, int totalSegmentos) {
        if (this == NONE || segmento < 1 || totalSegmentos < 1) {
            return false;
        }
        return desdeSegmento(segmento, totalSegmentos) == this;
    }

    public static TrackSector desdeSegmento(int segmento, int totalSegmentos) {
        int sector = Math.min(3, ((segmento - 1) * 3 / totalSegmentos) + 1);
        return values()[sector];
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
