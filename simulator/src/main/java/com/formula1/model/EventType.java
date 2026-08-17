package com.formula1.model;

/** Catálogo estable de HU-30. Los pesos se comparan dentro de cada categoría. */
public enum EventType {
    NO_EVENT("Sin evento", EventCategory.NO_EVENT, EventScope.INDIVIDUAL, 1, 0),

    PERFECT_LAP("Vuelta perfecta", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 1.0, 4),
    CLEAN_AIR("Aire limpio", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 2.1, 2),
    SLIPSTREAM("Rebufo", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 1.8, 2),
    TRACK_EVOLUTION_ADVANTAGE("Ventaja por evolución de pista", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 1.5, 3),
    STRONG_SECTOR("Sector sobresaliente", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 2.6, 1),

    TRAFFIC("Tráfico", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 2.5, 1),
    DRIVER_MISTAKE("Error del piloto", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.8, 2),
    LOCK_UP("Bloqueo de neumáticos", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.5, 2),
    WHEELSPIN("Patinaje de ruedas", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.6, 2),
    WIDE_CORNER("Salida amplia en curva", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.8, 2),
    OVERSTEER("Sobreviraje", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.4, 2),
    UNDERSTEER("Subviraje", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.4, 2),

    HEAVY_TRAFFIC("Tráfico intenso", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 1.5, 3),
    TYRE_OVERHEATING("Sobrecalentamiento de neumáticos", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 1.3, 3),
    TYRE_TOO_COLD("Neumáticos demasiado fríos", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 1.2, 3),
    BRAKE_OVERHEATING("Sobrecalentamiento de frenos", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 0.9, 4),
    ENGINE_TEMPERATURE_HIGH("Temperatura del motor elevada", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 0.8, 4),
    MINOR_MECHANICAL_ISSUE("Problema mecánico menor", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 0.7, 5),
    POWER_UNIT_DERATING("Reducción de potencia", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 0.6, 5),

    YELLOW_FLAG("Bandera amarilla", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.0, 4),
    LOCAL_YELLOW_FLAG("Bandera amarilla local", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.8, 3),
    RAIN_STARTS("Comienza la lluvia", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.4, 5),
    RAIN_INTENSIFIES("La lluvia se intensifica", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.2, 4),
    RAIN_STOPS("Deja de llover", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.0, 5),
    TRACK_DRYING("La pista se está secando", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.3, 3),
    WIND_GUST("Ráfaga de viento", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.7, 2),

    RED_FLAG("Bandera roja", EventCategory.EXCEPTIONAL, EventScope.GLOBAL, 4.0, 10),
    CRASH("Accidente", EventCategory.EXCEPTIONAL, EventScope.INDIVIDUAL, 1.0, 10);

    private final String etiqueta;
    private final EventCategory categoria;
    private final EventScope alcance;
    private final double pesoBase;
    private final int cooldownVueltas;

    EventType(String etiqueta, EventCategory categoria, EventScope alcance,
              double pesoBase, int cooldownVueltas) {
        this.etiqueta = etiqueta;
        this.categoria = categoria;
        this.alcance = alcance;
        this.pesoBase = pesoBase;
        this.cooldownVueltas = cooldownVueltas;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public EventCategory getCategoria() {
        return categoria;
    }

    public EventScope getAlcance() {
        return alcance;
    }

    public double getPesoBase() {
        return pesoBase;
    }

    public int getCooldownVueltas() {
        return cooldownVueltas;
    }
}
