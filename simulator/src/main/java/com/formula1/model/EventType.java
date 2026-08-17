package com.formula1.model;

/** Catálogo estable de HU-30. Los pesos se comparan dentro de cada categoría. */
public enum EventType {
    NO_EVENT("Sin evento", EventCategory.NO_EVENT, EventScope.INDIVIDUAL, 1, 0),

    PERFECT_LAP("Perfect Lap", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 1.0, 4),
    CLEAN_AIR("Clean Air", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 2.1, 2),
    SLIPSTREAM("Slipstream", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 1.8, 2),
    TRACK_EVOLUTION_ADVANTAGE("Track Evolution Advantage", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 1.5, 3),
    STRONG_SECTOR("Strong Sector", EventCategory.POSITIVE, EventScope.INDIVIDUAL, 2.6, 1),

    TRAFFIC("Traffic", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 2.5, 1),
    DRIVER_MISTAKE("Driver Mistake", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.8, 2),
    LOCK_UP("Lock Up", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.5, 2),
    WHEELSPIN("Wheelspin", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.6, 2),
    WIDE_CORNER("Wide Corner", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.8, 2),
    OVERSTEER("Oversteer", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.4, 2),
    UNDERSTEER("Understeer", EventCategory.MINOR_NEGATIVE, EventScope.INDIVIDUAL, 1.4, 2),

    HEAVY_TRAFFIC("Heavy Traffic", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 1.5, 3),
    TYRE_OVERHEATING("Tyre Overheating", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 1.3, 3),
    TYRE_TOO_COLD("Tyre Too Cold", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 1.2, 3),
    BRAKE_OVERHEATING("Brake Overheating", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 0.9, 4),
    ENGINE_TEMPERATURE_HIGH("Engine Temperature High", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 0.8, 4),
    MINOR_MECHANICAL_ISSUE("Minor Mechanical Issue", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 0.7, 5),
    POWER_UNIT_DERATING("Power Unit Derating", EventCategory.MAJOR_NEGATIVE, EventScope.INDIVIDUAL, 0.6, 5),

    YELLOW_FLAG("Yellow Flag", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.0, 4),
    LOCAL_YELLOW_FLAG("Local Yellow Flag", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.8, 3),
    RAIN_STARTS("Rain Starts", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.4, 5),
    RAIN_INTENSIFIES("Rain Intensifies", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.2, 4),
    RAIN_STOPS("Rain Stops", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.0, 5),
    TRACK_DRYING("Track Drying", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.3, 3),
    WIND_GUST("Wind Gust", EventCategory.WEATHER_TRACK, EventScope.GLOBAL, 1.7, 2),

    RED_FLAG("Red Flag", EventCategory.EXCEPTIONAL, EventScope.GLOBAL, 4.0, 10),
    CRASH("Crash / Accident", EventCategory.EXCEPTIONAL, EventScope.INDIVIDUAL, 1.0, 10);

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
