package com.formula1.service;

import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.QualifyingSession;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.WeatherCondition;
import com.formula1.util.FormatUtils;

import javafx.concurrent.Task;

import java.util.function.BooleanSupplier;

/**
 * Envuelve {@link QualifyingService#simular} en un {@link Task} de JavaFX
 * para ejecutarlo fuera del hilo de interfaz. El {@code Task} ya publica
 * progreso y mensajes en el hilo de JavaFX, así que no hace falta
 * {@code Platform.runLater} dentro de esta clase.
 *
 * Fase 4 de la migración a hexagonal: antes esto vivía dentro de
 * {@code QualifyingService} (que por eso importaba {@code javafx.concurrent});
 * se extrajo para que el caso de uso —{@code simular}— se pueda ejecutar sin
 * inicializar JavaFX. Sigue en el paquete {@code service} porque coordina
 * miembros de paquete de {@code QualifyingService} ({@code SimulationPacer},
 * {@code ControlSimulacion}, {@code SEGMENTOS_EVOLUCION}); la Fase 7 la
 * reubicará junto al resto de la infraestructura JavaFX sin cambiar su
 * contenido.
 */
public final class QualifyingSessionTaskFactory {

    private final QualifyingService servicio;

    public QualifyingSessionTaskFactory(QualifyingService servicio) {
        this.servicio = servicio;
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config) {
        return crearTarea(config, null);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config,
                                              QualifyingService.Evolucion evolucion) {
        return crearTarea(config, evolucion, null);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config,
                                              QualifyingService.Evolucion evolucion,
                                              QualifyingService.Telemetria telemetria) {
        return crearTarea(config, evolucion, telemetria, null);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config,
                                              QualifyingService.Evolucion evolucion,
                                              QualifyingService.Telemetria telemetria,
                                              QualifyingService.EvolucionPista observadorPista) {
        return crearTarea(config, evolucion, telemetria, observadorPista, null);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config,
                                              QualifyingService.Evolucion evolucion,
                                              QualifyingService.Telemetria telemetria,
                                              QualifyingService.EvolucionPista observadorPista,
                                              QualifyingService.ClasificacionEnVivo clasificacionEnVivo) {
        return crearTarea(config, evolucion, telemetria, observadorPista,
                clasificacionEnVivo, () -> false);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config,
                                              QualifyingService.Evolucion evolucion,
                                              QualifyingService.Telemetria telemetria,
                                              QualifyingService.EvolucionPista observadorPista,
                                              QualifyingService.ClasificacionEnVivo clasificacionEnVivo,
                                              BooleanSupplier finalizarSolicitado) {
        return crearTarea(config, evolucion, telemetria, observadorPista,
                clasificacionEnVivo, null, finalizarSolicitado);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config,
                                              QualifyingService.Evolucion evolucion,
                                              QualifyingService.Telemetria telemetria,
                                              QualifyingService.EvolucionPista observadorPista,
                                              QualifyingService.ClasificacionEnVivo clasificacionEnVivo,
                                              QualifyingService.EventosEnVivo observadorEventos,
                                              BooleanSupplier finalizarSolicitado) {
        return crearTarea(config, evolucion, telemetria, observadorPista,
                clasificacionEnVivo, observadorEventos, null,
                finalizarSolicitado);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config,
                                              QualifyingService.Evolucion evolucion,
                                              QualifyingService.Telemetria telemetria,
                                              QualifyingService.EvolucionPista observadorPista,
                                              QualifyingService.ClasificacionEnVivo clasificacionEnVivo,
                                              QualifyingService.EventosEnVivo observadorEventos,
                                              QualifyingService.PitStopsEnVivo observadorPitStops,
                                              BooleanSupplier finalizarSolicitado) {
        return crearTarea(config, evolucion, telemetria, observadorPista,
                clasificacionEnVivo, observadorEventos, observadorPitStops,
                null, finalizarSolicitado);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config,
                                              QualifyingService.Evolucion evolucion,
                                              QualifyingService.Telemetria telemetria,
                                              QualifyingService.EvolucionPista observadorPista,
                                              QualifyingService.ClasificacionEnVivo clasificacionEnVivo,
                                              QualifyingService.EventosEnVivo observadorEventos,
                                              QualifyingService.PitStopsEnVivo observadorPitStops,
                                              QualifyingService.CambiosNeumaticosEnVivo observadorCambiosNeumaticos,
                                              BooleanSupplier finalizarSolicitado) {
        return new Task<>() {
            @Override
            protected QualifyingSession call() throws Exception {
                Circuit circuito = servicio.validarSeleccion(config);
                SimulationPacer regulador = new SimulationPacer(
                        config.getDuracionSegundos(), finalizarSolicitado);

                WeatherCondition clima = servicio.generarClima(circuito);
                updateMessage("Clima de la sesión: " + clima.getEtiqueta());

                QualifyingSession sesion = servicio.simular(
                        config,
                        clima,
                        (hecho, total, mensaje) -> {
                            updateMessage("Preparando parrilla · " + mensaje);
                        },
                        evolucion,
                        telemetria,
                        observadorPista,
                        clasificacionEnVivo,
                        new QualifyingService.ControlSimulacion() {
                            @Override
                            public int totalFotogramas(int minimo) {
                                return regulador.totalFotogramas(minimo);
                            }

                            @Override
                            public boolean completarFotograma(int fotograma, int total) {
                                updateProgress(fotograma, total);
                                int segmento = Math.min(QualifyingService.SEGMENTOS_EVOLUCION,
                                        Math.max(1, (int) Math.ceil(
                                                fotograma * QualifyingService.SEGMENTOS_EVOLUCION
                                                        / (double) total)));
                                updateMessage("Simulación en vivo · segmento "
                                        + segmento + " de " + QualifyingService.SEGMENTOS_EVOLUCION);
                                return regulador.completarFotograma(fotograma, total);
                            }
                        },
                        observadorEventos,
                        observadorPitStops,
                        observadorCambiosNeumaticos);

                if (!finalizarSolicitado.getAsBoolean()) {
                    updateProgress(1, 1);
                }
                LapResult pole = sesion.getPole();
                updateMessage(finalizarSolicitado.getAsBoolean()
                        ? "Sesión finalizada manualmente y preparada para guardar"
                        : pole == null ? "Sesión sin resultados"
                        : "Pole: " + pole.getPiloto() + " — " + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
                return sesion;
            }
        };
    }
}
