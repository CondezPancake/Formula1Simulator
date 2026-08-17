package com.formula1.event;

import com.formula1.model.EventImpact;
import com.formula1.model.EventOccurrence;
import com.formula1.model.EventScope;
import com.formula1.model.EventType;
import com.formula1.model.LapResult;
import com.formula1.model.LapStatus;
import com.formula1.model.TrackFlag;
import com.formula1.model.TrackSector;
import com.formula1.model.WeatherSnapshot;
import com.formula1.util.MathUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Aplica impactos seleccionados sobre clima, resultado y muestras de sector. */
public final class EventEffectService {

    public List<WeatherSnapshot> applyGlobalWeather(
            List<WeatherSnapshot> weather, List<EventOccurrence> occurrences) {
        List<WeatherSnapshot> updated = new ArrayList<>(weather);
        occurrences.stream()
                .filter(EventOccurrence::ocurrio)
                .filter(event -> event.alcance() == EventScope.GLOBAL)
                .filter(event -> event.impacto().deltaIntensidadLluviaPorcentaje() != 0)
                .forEach(event -> {
                    for (int index = 0; index < updated.size(); index++) {
                        WeatherSnapshot sample = updated.get(index);
                        if (atOrAfter(sample, event.sector())) {
                            updated.set(index, sample.conImpacto(
                                    event.impacto().deltaIntensidadLluviaPorcentaje(),
                                    event.impacto().deltaGripPorcentaje()));
                        }
                    }
                });
        return List.copyOf(updated);
    }

    public void applyResult(LapResult result, double baseTime,
                            double baseConsumption, double baseWear,
                            List<EventOccurrence> occurrences) {
        List<EventOccurrence> actual = occurrences.stream()
                .filter(EventOccurrence::ocurrio)
                .toList();
        result.setEventos(actual);
        double timeDelta = actual.stream()
                .mapToDouble(event -> event.impacto().deltaTiempoSegundos())
                .sum();
        double wearDelta = actual.stream()
                .mapToDouble(event -> event.impacto().deltaDesgaste())
                .sum();
        EventOccurrence invalidating = actual.stream()
                .filter(event -> event.impacto().vueltaInvalidada())
                .max(Comparator.comparing(event -> event.impacto().pilotoFuera()))
                .orElse(null);

        if (invalidating != null) {
            double completedFraction = completedFraction(invalidating.sector());
            result.setTiempoSegundos(0);
            result.setConsumoEstimado(baseConsumption * completedFraction);
            result.setDesgasteEstimado(MathUtils.clamp(
                    baseWear * completedFraction + Math.max(0, wearDelta), 0, 100));
            result.setEstadoVuelta(invalidating.impacto().pilotoFuera()
                    ? LapStatus.OUT : LapStatus.INVALID);
            result.setSectorIncidente(invalidating.sector());
            return;
        }

        result.setTiempoSegundos(Math.max(1, baseTime + timeDelta));
        result.setConsumoEstimado(baseConsumption);
        result.setDesgasteEstimado(MathUtils.clamp(baseWear + wearDelta, 0, 100));
        result.setEstadoVuelta(LapStatus.VALID);
        result.setSectorIncidente(TrackSector.NONE);
    }

    /** Combina como máximo un evento individual y uno global del mismo sector. */
    public EventImpact impactAt(List<EventOccurrence> occurrences, TrackSector sector) {
        List<EventOccurrence> active = occurrences.stream()
                .filter(EventOccurrence::ocurrio)
                .filter(event -> event.sector() == sector)
                .toList();
        if (active.isEmpty()) {
            return EventImpact.none();
        }
        double speed = active.stream()
                .mapToDouble(event -> event.impacto().multiplicadorVelocidad())
                .reduce(1, (left, right) -> left * right);
        double grip = active.stream()
                // El cambio global de lluvia ya está incorporado en WeatherSnapshot.
                .filter(event -> event.impacto().deltaIntensidadLluviaPorcentaje() == 0)
                .mapToDouble(event -> event.impacto().deltaGripPorcentaje())
                .sum();
        double wear = active.stream().mapToDouble(event -> event.impacto().deltaDesgaste()).sum();
        double tyreTemp = active.stream()
                .mapToDouble(event -> event.impacto().deltaTemperaturaNeumaticosC()).sum();
        double engineTemp = active.stream()
                .mapToDouble(event -> event.impacto().deltaTemperaturaMotorC()).sum();
        boolean invalid = active.stream().anyMatch(event -> event.impacto().vueltaInvalidada());
        boolean out = active.stream().anyMatch(event -> event.impacto().pilotoFuera());
        TrackFlag flag = active.stream()
                .map(event -> event.impacto().bandera())
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(TrackFlag.GREEN);
        return new EventImpact(0, speed, grip, wear, tyreTemp, engineTemp,
                0, invalid, out, flag);
    }

    public EventOccurrence eventAt(List<EventOccurrence> occurrences, TrackSector sector,
                                   int driverId, String driver, int lap) {
        return occurrences.stream()
                .filter(EventOccurrence::ocurrio)
                .filter(event -> event.sector() == sector)
                .sorted(Comparator.comparing((EventOccurrence event) -> event.tipo() != EventType.CRASH)
                        .thenComparing(event -> event.alcance() != EventScope.INDIVIDUAL))
                .findFirst()
                .orElseGet(() -> EventOccurrence.noEvent(driverId, driver, lap));
    }

    public boolean crashedAtOrBefore(List<EventOccurrence> occurrences, TrackSector sector) {
        return occurrences.stream()
                .filter(event -> event.tipo() == EventType.CRASH)
                .anyMatch(event -> event.sector().ordinal() <= sector.ordinal());
    }

    public boolean invalidatedAtOrBefore(List<EventOccurrence> occurrences, TrackSector sector) {
        return occurrences.stream()
                .filter(event -> event.impacto().vueltaInvalidada())
                .anyMatch(event -> event.sector().ordinal() <= sector.ordinal());
    }

    private boolean atOrAfter(WeatherSnapshot sample, TrackSector sector) {
        return TrackSector.desdeSegmento(sample.segmento(), sample.totalSegmentos()).ordinal()
                >= sector.ordinal();
    }

    private double completedFraction(TrackSector sector) {
        return switch (sector) {
            case SECTOR_1 -> 1.0 / 6;
            case SECTOR_2 -> 0.5;
            case SECTOR_3 -> 5.0 / 6;
            case NONE -> 0;
        };
    }
}
