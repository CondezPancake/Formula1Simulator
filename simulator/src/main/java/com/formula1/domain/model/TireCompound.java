package com.formula1.domain.model;

/** Compuestos secos de HU-51 con sus contrapartidas de ritmo y desgaste. */
public enum TireCompound {
    SOFT("S", "Soft", 0.985, 1.28),
    MEDIUM("M", "Medium", 1.000, 1.00),
    HARD("H", "Hard", 1.012, 0.76);

    private final String codigo;
    private final String etiqueta;
    private final double factorTiempo;
    private final double factorDesgaste;

    TireCompound(String codigo, String etiqueta,
                 double factorTiempo, double factorDesgaste) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
        this.factorTiempo = factorTiempo;
        this.factorDesgaste = factorDesgaste;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public double getFactorTiempo() {
        return factorTiempo;
    }

    public double getFactorDesgaste() {
        return factorDesgaste;
    }

    @Override
    public String toString() {
        return codigo + " · " + etiqueta;
    }
}
