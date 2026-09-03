package com.formula1.domain.model;

/** Estados progresivos que puede atravesar la pista durante una sesión. */
public enum DynamicWeatherState {

    SECO("Seco", WeatherCondition.SECO, 1.000,
            "Pista seca", "Slicks", "Ataque controlado"),
    NUBLADO("Nublado", WeatherCondition.SECO, 1.010,
            "Pista seca", "Slicks", "Equilibrada"),
    LLUVIA_LIGERA("Lluvia ligera", WeatherCondition.LLUVIOSO, 1.040,
            "Pista húmeda", "Intermedios", "Conservadora"),
    LLUVIA("Lluvia", WeatherCondition.LLUVIOSO, 1.080,
            "Pista mojada", "Intermedios", "Conservadora"),
    LLUVIA_INTENSA("Lluvia intensa", WeatherCondition.EXTREMO, 1.180,
            "Pista con agua", "Lluvia extrema", "Máxima precaución");

    private final String etiqueta;
    private final WeatherCondition condicionEquivalente;
    private final double factorTiempoBase;
    private final String estadoPista;
    private final String neumaticoRecomendado;
    private final String estrategiaRecomendada;

    DynamicWeatherState(String etiqueta, WeatherCondition condicionEquivalente,
                        double factorTiempoBase, String estadoPista,
                        String neumaticoRecomendado, String estrategiaRecomendada) {
        this.etiqueta = etiqueta;
        this.condicionEquivalente = condicionEquivalente;
        this.factorTiempoBase = factorTiempoBase;
        this.estadoPista = estadoPista;
        this.neumaticoRecomendado = neumaticoRecomendado;
        this.estrategiaRecomendada = estrategiaRecomendada;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public WeatherCondition getCondicionEquivalente() {
        return condicionEquivalente;
    }

    public double getFactorTiempoBase() {
        return factorTiempoBase;
    }

    public String getEstadoPista() {
        return estadoPista;
    }

    public String getNeumaticoRecomendado() {
        return neumaticoRecomendado;
    }

    public String getEstrategiaRecomendada() {
        return estrategiaRecomendada;
    }

    public static DynamicWeatherState desdeIntensidad(double intensidadPorcentaje) {
        if (intensidadPorcentaje < 3) return SECO;
        if (intensidadPorcentaje < 18) return NUBLADO;
        if (intensidadPorcentaje < 38) return LLUVIA_LIGERA;
        if (intensidadPorcentaje < 72) return LLUVIA;
        return LLUVIA_INTENSA;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
