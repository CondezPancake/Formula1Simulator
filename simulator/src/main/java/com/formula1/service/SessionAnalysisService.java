package com.formula1.service;

import com.formula1.model.EventOccurrence;
import com.formula1.model.EventType;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SessionAnalysis;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.TrackEvolutionSnapshot;
import com.formula1.model.TrackSector;
import com.formula1.model.WeatherSnapshot;
import com.formula1.util.FormatUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Genera explicaciones por reglas del propio motor; no depende de servicios
 * externos ni de JavaFX, asi que se puede probar y persistir directamente.
 */
public class SessionAnalysisService {

    private static final double SECTOR_LOSS_THRESHOLD = 0.080;
    private static final double EVENT_DELTA_THRESHOLD = 0.050;
    private static final double METRIC_MARGIN = 0.97;

    public SessionAnalysis analizar(QualifyingSession sesion) {
        if (sesion == null) {
            throw new ValidationException("La sesion no puede ser nula");
        }

        List<LapResult> validos = resultadosValidos(sesion);
        if (validos.isEmpty()) {
            return SessionAnalysis.vacio();
        }

        LapResult pole = validos.get(0);
        List<String> positivos = new ArrayList<>();
        List<String> negativos = new ArrayList<>();
        List<String> claves = new ArrayList<>();

        explicarResultado(validos, pole, positivos, claves);
        explicarSectores(validos, pole, positivos, negativos);
        explicarConsumoYDesgaste(validos, pole, positivos, negativos);
        explicarEventos(pole, positivos, negativos);
        explicarClima(sesion, positivos, negativos, claves);
        explicarPista(sesion, positivos, negativos, claves);
        explicarTelemetria(sesion, pole, positivos, negativos);

        limitar(positivos, "Ritmo competitivo sostenido frente al promedio de la parrilla");
        limitar(negativos, "Sin factores negativos determinantes");
        limitar(claves, "La pole se explica por ritmo limpio y buena ejecucion general");

        return new SessionAnalysis(
                pole.getPiloto(),
                pole.getTiempoSegundos(),
                validos.size(),
                resumen(validos, pole),
                primeros(positivos, 5),
                primeros(negativos, 5),
                primeros(claves, 4));
    }

    private List<LapResult> resultadosValidos(QualifyingSession sesion) {
        return (sesion.getResultados() == null ? List.<LapResult>of() : sesion.getResultados())
                .stream()
                .filter(LapResult::isVueltaValida)
                .sorted(Comparator.comparingDouble(LapResult::getTiempoSegundos))
                .toList();
    }

    private void explicarResultado(List<LapResult> validos, LapResult pole,
                                   List<String> positivos, List<String> claves) {
        if (validos.size() > 1) {
            double margen = validos.get(1).getTiempoSegundos() - pole.getTiempoSegundos();
            positivos.add("Pole por " + FormatUtils.formatDelta(margen)
                    + " sobre " + validos.get(1).getPiloto());
            claves.add(margen < 0.150
                    ? "Sesion muy cerrada: la diferencia con P2 fue menor a 0.150 s"
                    : "Ventaja clara en la parte alta de la clasificacion");
        } else {
            positivos.add("Unica vuelta valida de la sesion");
            claves.add("La prioridad fue completar una vuelta representativa");
        }
    }

    private void explicarSectores(List<LapResult> validos, LapResult pole,
                                  List<String> positivos, List<String> negativos) {
        if (!pole.hasSectorTimes()) {
            return;
        }
        for (TrackSector sector : List.of(TrackSector.SECTOR_1, TrackSector.SECTOR_2, TrackSector.SECTOR_3)) {
            Optional<LapResult> mejor = validos.stream()
                    .filter(LapResult::hasSectorTimes)
                    .min(Comparator.comparingDouble(r -> r.getSectorTimes().tiempoDe(sector)));
            if (mejor.isEmpty()) {
                continue;
            }
            double perdida = pole.getSectorTimes().tiempoDe(sector)
                    - mejor.get().getSectorTimes().tiempoDe(sector);
            if (mejor.get() == pole || perdida < 0.001) {
                positivos.add("Mejor rendimiento en " + sector.getEtiqueta());
            } else if (perdida >= SECTOR_LOSS_THRESHOLD) {
                negativos.add("Perdio " + FormatUtils.formatDelta(perdida)
                        + " en " + sector.getEtiqueta() + " frente a " + mejor.get().getPiloto());
            }
        }
    }

    private void explicarConsumoYDesgaste(List<LapResult> validos, LapResult pole,
                                          List<String> positivos, List<String> negativos) {
        double consumoPromedio = promedio(validos.stream()
                .mapToDouble(LapResult::getConsumoEstimado).toArray());
        double desgastePromedio = promedio(validos.stream()
                .mapToDouble(LapResult::getDesgasteEstimado).toArray());

        if (pole.getConsumoEstimado() <= consumoPromedio * METRIC_MARGIN) {
            positivos.add("Consumo eficiente respecto al promedio de la parrilla");
        } else if (pole.getConsumoEstimado() > consumoPromedio / METRIC_MARGIN) {
            negativos.add("Consumo por encima del promedio para sostener el ritmo");
        }

        if (pole.getDesgasteEstimado() <= desgastePromedio * METRIC_MARGIN) {
            positivos.add("Baja degradacion de neumaticos");
        } else if (pole.getDesgasteEstimado() > desgastePromedio / METRIC_MARGIN) {
            negativos.add("Degradacion elevada de neumaticos");
        }
    }

    private void explicarEventos(LapResult pole, List<String> positivos, List<String> negativos) {
        pole.getEventos().stream()
                .filter(EventOccurrence::ocurrio)
                .forEach(evento -> {
                    double delta = evento.impacto().deltaTiempoSegundos();
                    if (delta <= -EVENT_DELTA_THRESHOLD) {
                        positivos.add(evento.tipo().getEtiqueta() + " en " + evento.sector().getEtiqueta());
                    } else if (delta >= EVENT_DELTA_THRESHOLD || evento.tipo() != EventType.NO_EVENT) {
                        negativos.add(evento.tipo().getEtiqueta() + " en " + evento.sector().getEtiqueta());
                    }
                });
    }

    private void explicarClima(QualifyingSession sesion, List<String> positivos,
                               List<String> negativos, List<String> claves) {
        List<WeatherSnapshot> clima = sesion.getEvolucionClimatica();
        if (clima.isEmpty()) {
            return;
        }
        double grip = clima.stream().mapToDouble(WeatherSnapshot::gripPorcentaje).average().orElse(0);
        double lluvia = clima.stream().mapToDouble(WeatherSnapshot::intensidadLluviaPorcentaje).average().orElse(0);
        if (grip >= 88 && lluvia < 20) {
            positivos.add("Condiciones favorables de grip y pista seca");
        } else if (lluvia >= 35) {
            negativos.add("Lluvia significativa redujo agarre y confianza de frenado");
        }
        claves.add("Grip medio de pista: " + String.format(java.util.Locale.ROOT, "%.0f%%", grip));
    }

    private void explicarPista(QualifyingSession sesion, List<String> positivos,
                               List<String> negativos, List<String> claves) {
        List<TrackEvolutionSnapshot> pista = sesion.getEvolucionPista();
        if (pista.isEmpty()) {
            return;
        }
        TrackEvolutionSnapshot inicial = pista.get(0);
        TrackEvolutionSnapshot finalSesion = pista.get(pista.size() - 1);
        double mejoraGrip = finalSesion.gripFinalPorcentaje() - inicial.gripInicialPorcentaje();
        if (mejoraGrip > 1.0) {
            positivos.add("La goma acumulada mejoro el grip durante la sesion");
        } else if (mejoraGrip < -1.0) {
            negativos.add("La lluvia limpio la pista y redujo la adherencia acumulada");
        }
        claves.add("Goma final en pista: "
                + String.format(java.util.Locale.ROOT, "%.1f%%", finalSesion.gomaFinalPorcentaje()));
    }

    private void explicarTelemetria(QualifyingSession sesion, LapResult pole,
                                    List<String> positivos, List<String> negativos) {
        List<TelemetrySnapshot> muestras = sesion.getEvolucionVuelta().stream()
                .filter(muestra -> muestra.piloto().equals(pole.getPiloto()))
                .toList();
        if (muestras.isEmpty()) {
            return;
        }
        double tempNeumaticos = muestras.stream()
                .mapToDouble(TelemetrySnapshot::temperaturaNeumaticosC).average().orElse(0);
        double tempMotor = muestras.stream()
                .mapToDouble(TelemetrySnapshot::temperaturaMotorC).average().orElse(0);
        if (tempNeumaticos >= 82 && tempNeumaticos <= 105) {
            positivos.add("Neumaticos dentro de la ventana optima");
        } else {
            negativos.add("Temperatura de neumaticos fuera de ventana optima");
        }
        if (tempMotor > 112) {
            negativos.add("Temperatura del motor elevada");
        }
    }

    private String resumen(List<LapResult> validos, LapResult pole) {
        if (validos.size() == 1) {
            return pole.getPiloto() + " marco la unica vuelta valida con "
                    + FormatUtils.formatLapTime(pole.getTiempoSegundos()) + ".";
        }
        double margen = validos.get(1).getTiempoSegundos() - pole.getTiempoSegundos();
        return pole.getPiloto() + " logro la pole con "
                + FormatUtils.formatLapTime(pole.getTiempoSegundos())
                + " y una ventaja de " + FormatUtils.formatDelta(margen) + ".";
    }

    private double promedio(double[] valores) {
        if (valores.length == 0) {
            return 0;
        }
        double suma = 0;
        for (double valor : valores) {
            suma += valor;
        }
        return suma / valores.length;
    }

    private void limitar(List<String> valores, String fallback) {
        if (valores.isEmpty()) {
            valores.add(fallback);
        }
    }

    private List<String> primeros(List<String> valores, int max) {
        return valores.stream().distinct().limit(max).toList();
    }
}
