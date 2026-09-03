package com.formula1.domain.event;

import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.EventImpact;
import com.formula1.domain.model.EventType;
import com.formula1.domain.model.TrackFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/** Construye el catálogo completo sin trasladar reglas al coordinador. */
public final class EventCatalog {

    private EventCatalog() {
    }

    public static List<SimulationEvent> defaultEvents() {
        List<SimulationEvent> events = new ArrayList<>();
        Predicate<EventContext> always = context -> true;
        ToDoubleFunction<EventContext> neutral = context -> 1;

        // Eventos positivos: habilidad y consistencia favorecen su peso relativo.
        events.add(performance(EventType.PERFECT_LAP, always,
                context -> 0.6 + context.habilidadNormalizada(),
                impact(-0.35, -0.10, 1.015, 0, 2, -0.20, 0,
                        0, 2, 0, 1)));
        events.add(performance(EventType.CLEAN_AIR,
                context -> context.nivelTrafico() <= 1, neutral,
                impact(-0.20, -0.05, 1.010, 0, 1, -0.10, 0,
                        0, 1, 0, 0)));
        events.add(performance(EventType.SLIPSTREAM,
                context -> context.nivelTrafico() >= 1, neutral,
                impact(-0.18, -0.04, 1.022, 0, 0, 0, 0.05,
                        0, 2, 0, 1)));
        events.add(performance(EventType.TRACK_EVOLUTION_ADVANTAGE,
                context -> context.climaDelSector().gripPorcentaje() >= 65, neutral,
                impact(-0.15, -0.03, 1.006, 1, 3, -0.05, 0,
                        0, 2, 0, 0)));
        events.add(performance(EventType.STRONG_SECTOR, always,
                context -> 0.7 + context.habilidadNormalizada(),
                impact(-0.18, -0.05, 1.010, 0, 1, -0.05, 0,
                        0, 2, 0, 0)));

        // Errores leves: el multiplicador incorpora piloto, lluvia, grip y estrategia.
        events.add(performance(EventType.TRAFFIC,
                context -> context.nivelTrafico() >= 1, neutral,
                impact(0.10, 0.40, 0.980, -1, 0, 0, 0.10,
                        0, 2, 0, 1)));
        events.add(performance(EventType.DRIVER_MISTAKE, always,
                EventContext::multiplicadorRiesgoError,
                impact(0.10, 0.60, 0.965, -3, -1, 0.05, 0.30,
                        1, 4, 0, 2)));
        events.add(performance(EventType.LOCK_UP,
                context -> context.climaDelSector().gripPorcentaje() < 90
                        || context.configuracion().getModo() == DrivingMode.AGRESIVA,
                EventContext::multiplicadorRiesgoError,
                impact(0.15, 0.50, 0.955, -4, -2, 0.20, 0.60,
                        4, 10, 1, 4)));
        events.add(performance(EventType.WHEELSPIN,
                context -> context.climaDelSector().gripPorcentaje() < 86
                        || context.climaDelSector().intensidadLluviaPorcentaje() > 10,
                EventContext::multiplicadorRiesgoError,
                impact(0.08, 0.35, 0.970, -3, -1, 0.10, 0.35,
                        2, 7, 0, 2)));
        events.add(performance(EventType.WIDE_CORNER, always,
                EventContext::multiplicadorRiesgoError,
                impact(0.12, 0.45, 0.960, -3, -1, 0.10, 0.40,
                        1, 5, 0, 2)));
        events.add(performance(EventType.OVERSTEER,
                context -> context.climaDelSector().gripPorcentaje() < 92,
                EventContext::multiplicadorRiesgoError,
                impact(0.10, 0.38, 0.965, -4, -1, 0.10, 0.45,
                        2, 6, 0, 2)));
        events.add(performance(EventType.UNDERSTEER, always,
                EventContext::multiplicadorRiesgoError,
                impact(0.10, 0.40, 0.965, -3, -1, 0.10, 0.40,
                        1, 5, 0, 2)));

        // Impactos importantes y técnicos.
        events.add(performance(EventType.HEAVY_TRAFFIC,
                context -> context.nivelTrafico() >= 3, neutral,
                impact(0.30, 0.90, 0.930, -2, 0, 0.10, 0.35,
                        1, 4, 0, 2)));
        events.add(performance(EventType.TYRE_OVERHEATING,
                context -> context.temperaturaNeumaticosC() >= 92
                        || context.configuracion().getModo() == DrivingMode.AGRESIVA,
                context -> context.multiplicadorEstrategia(),
                impact(0.20, 0.65, 0.955, -5, -2, 0.30, 0.90,
                        8, 18, 0, 2)));
        events.add(performance(EventType.TYRE_TOO_COLD,
                context -> context.temperaturaNeumaticosC() <= 75
                        || context.climaDelSector().intensidadLluviaPorcentaje() >= 25,
                context -> context.multiplicadorRiesgoClimatico(),
                impact(0.18, 0.55, 0.960, -6, -2, 0.15, 0.50,
                        -12, -5, 0, 1)));
        events.add(performance(EventType.BRAKE_OVERHEATING,
                context -> context.temperaturaMotorC() >= 104
                        || context.configuracion().getModo() == DrivingMode.AGRESIVA,
                EventContext::multiplicadorRiesgoVehiculo,
                impact(0.25, 0.70, 0.950, -3, -1, 0.10, 0.45,
                        1, 4, 6, 14)));
        events.add(performance(EventType.ENGINE_TEMPERATURE_HIGH,
                context -> context.temperaturaMotorC() >= 104
                        || context.configuracion().getModo() == DrivingMode.AGRESIVA,
                EventContext::multiplicadorRiesgoVehiculo,
                impact(0.25, 0.80, 0.945, -1, 0, 0.05, 0.25,
                        0, 2, 8, 18)));
        events.add(performance(EventType.MINOR_MECHANICAL_ISSUE, always,
                EventContext::multiplicadorRiesgoVehiculo,
                impact(0.30, 0.85, 0.940, -1, 0, 0.05, 0.35,
                        0, 3, 2, 10)));
        events.add(performance(EventType.POWER_UNIT_DERATING,
                context -> context.temperaturaMotorC() >= 102
                        || context.multiplicadorRiesgoVehiculo() > 1.05,
                EventContext::multiplicadorRiesgoVehiculo,
                impact(0.40, 1.20, 0.910, 0, 0, 0, 0.20,
                        0, 1, 5, 15)));

        // Eventos globales: banderas y cambios reales en el clima/pista.
        events.add(track(EventType.YELLOW_FLAG, always, neutral,
                flagImpact(0.25, 0.80, 0.82, TrackFlag.YELLOW)));
        events.add(track(EventType.LOCAL_YELLOW_FLAG, always, neutral,
                flagImpact(0.15, 0.50, 0.88, TrackFlag.LOCAL_YELLOW)));
        events.add(track(EventType.RAIN_STARTS,
                context -> context.climaDelSector().intensidadLluviaPorcentaje() < 15
                        && context.climaDelSector().probabilidadLluviaPorcentaje() >= 20,
                EventContext::multiplicadorRiesgoClimatico,
                weatherImpact(12, 28, 0.970, -8, -4, -9, -4)));
        events.add(track(EventType.RAIN_INTENSIFIES,
                context -> context.climaDelSector().intensidadLluviaPorcentaje() >= 5
                        && context.climaDelSector().intensidadLluviaPorcentaje() < 78,
                EventContext::multiplicadorRiesgoClimatico,
                weatherImpact(10, 24, 0.950, -9, -4, -10, -4)));
        events.add(track(EventType.RAIN_STOPS,
                context -> context.climaDelSector().intensidadLluviaPorcentaje() > 18,
                neutral,
                weatherImpact(-35, -15, 1.010, 1, 4, 2, 7)));
        events.add(track(EventType.TRACK_DRYING,
                context -> context.climaDelSector().intensidadLluviaPorcentaje() < 25
                        && !"Pista seca".equals(context.climaDelSector().estadoPista()),
                neutral,
                weatherImpact(-18, -6, 1.015, 4, 8, 3, 8)));
        events.add(track(EventType.WIND_GUST, always, neutral,
                impact(0.05, 0.35, 0.975, -3, -1, 0, 0.15,
                        -1, 2, 0, 1)));

        events.add(track(EventType.RED_FLAG, always,
                context -> 0.8 + 0.4 * context.multiplicadorRiesgoClimatico(),
                (context, random) -> new EventImpact(0, 0, -8, 0,
                        0, 0, 0, true, false, TrackFlag.RED)));
        events.add(new CrashSimulationEvent());

        return List.copyOf(events);
    }

    private static SimulationEvent performance(EventType type,
                                               Predicate<EventContext> compatibility,
                                               ToDoubleFunction<EventContext> modifier,
                                               ImpactGenerator impact) {
        return new PerformanceEvent(type, compatibility, modifier, impact);
    }

    private static SimulationEvent track(EventType type,
                                         Predicate<EventContext> compatibility,
                                         ToDoubleFunction<EventContext> modifier,
                                         ImpactGenerator impact) {
        return new TrackSimulationEvent(type, compatibility, modifier, impact);
    }

    private static ImpactGenerator impact(double minTime, double maxTime, double speed,
                                          double minGrip, double maxGrip,
                                          double minWear, double maxWear,
                                          double minTyreTemp, double maxTyreTemp,
                                          double minEngineTemp, double maxEngineTemp) {
        return (context, random) -> new EventImpact(
                range(random, minTime, maxTime), speed,
                range(random, minGrip, maxGrip), range(random, minWear, maxWear),
                range(random, minTyreTemp, maxTyreTemp),
                range(random, minEngineTemp, maxEngineTemp), 0,
                false, false, TrackFlag.GREEN);
    }

    private static ImpactGenerator flagImpact(double minTime, double maxTime,
                                              double speed, TrackFlag flag) {
        return (context, random) -> new EventImpact(
                range(random, minTime, maxTime), speed, -2, 0,
                -2, 0, 0, false, false, flag);
    }

    private static ImpactGenerator weatherImpact(double minRain, double maxRain,
                                                 double speed,
                                                 double minGrip, double maxGrip,
                                                 double minTyreTemp, double maxTyreTemp) {
        return (context, random) -> new EventImpact(
                0, speed, range(random, minGrip, maxGrip), 0,
                range(random, minTyreTemp, maxTyreTemp), 0,
                range(random, minRain, maxRain), false, false, TrackFlag.GREEN);
    }

    private static double range(RandomGenerator random, double min, double max) {
        return min == max ? min : min + random.nextDouble() * (max - min);
    }
}
