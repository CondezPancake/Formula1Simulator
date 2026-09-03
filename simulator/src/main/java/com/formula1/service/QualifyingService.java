package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.data.QualifyingDataPort;
import com.formula1.domain.event.EventContext;
import com.formula1.domain.event.EventContextFactory;
import com.formula1.domain.event.EventEffectService;
import com.formula1.domain.event.EventManager;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.EventImpact;
import com.formula1.domain.model.EventOccurrence;
import com.formula1.domain.model.LapResult;
import com.formula1.domain.model.LapStatus;
import com.formula1.domain.model.LiveClassificationFrame;
import com.formula1.domain.model.PitStopRecord;
import com.formula1.domain.model.QualifyingSession;
import com.formula1.domain.model.SectorTimes;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.SimulationSnapshot;
import com.formula1.domain.model.TelemetrySnapshot;
import com.formula1.domain.model.TireChangeRecord;
import com.formula1.domain.model.TrackSector;
import com.formula1.domain.model.TrackEvolutionSnapshot;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherCondition;
import com.formula1.domain.model.WeatherSnapshot;
import com.formula1.util.DateUtils;
import com.formula1.util.FormatUtils;
import com.formula1.util.MathUtils;
import com.formula1.util.RandomUtils;
import com.formula1.util.ValidationUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ejecuta una sesión de clasificación: genera el clima, calcula el tiempo de
 * los 20 pilotos, ordena la parrilla y guarda el resultado.
 */
public class QualifyingService {

    static final int SEGMENTOS_EVOLUCION = 20;

    private final QualifyingDataPort datos;
    private final DriverService pilotos;
    private final VehicleService vehiculos;
    private final CircuitService circuitos;
    private final LapTimeCalculator calculadora;
    private final TelemetryCalculator calculadoraTelemetria;
    private final SectorTimeCalculator calculadoraSectores;
    private final TrackEvolutionService evolucionPista;
    private final DynamicWeatherService climaDinamico;
    private final EventManager eventos;
    private final EventContextFactory fabricaContextoEventos;
    private final EventEffectService efectosEventos;
    private final PitStopService paradasBoxes;
    private final PitStopPolicy politicaPitStop;
    private final TireStrategyService estrategiaNeumaticos;

    public QualifyingService() {
        this(DataStore.getInstance(), new LapTimeCalculator());
    }

    /** Permite reproducir únicamente la secuencia de eventos de una ejecución. */
    public QualifyingService(long eventSeed) {
        this(DataStore.getInstance(), new LapTimeCalculator(),
                new DynamicWeatherService(), new EventManager(eventSeed));
    }

    public QualifyingService(QualifyingDataPort datos, LapTimeCalculator calculadora) {
        this(datos, calculadora, new DynamicWeatherService());
    }

    QualifyingService(QualifyingDataPort datos, LapTimeCalculator calculadora,
                      DynamicWeatherService climaDinamico) {
        this(datos, calculadora, climaDinamico, new EventManager());
    }

    QualifyingService(QualifyingDataPort datos, LapTimeCalculator calculadora,
                      DynamicWeatherService climaDinamico, EventManager eventos) {
        this(datos, calculadora, climaDinamico, eventos, new PitStopService());
    }

    QualifyingService(QualifyingDataPort datos, LapTimeCalculator calculadora,
                      DynamicWeatherService climaDinamico, EventManager eventos,
                      PitStopService paradasBoxes) {
        this(datos, calculadora, climaDinamico, eventos, paradasBoxes,
                new ContextualPitStopPolicy());
    }

    QualifyingService(QualifyingDataPort datos, LapTimeCalculator calculadora,
                      DynamicWeatherService climaDinamico, EventManager eventos,
                      PitStopService paradasBoxes, PitStopPolicy politicaPitStop) {
        this(datos, calculadora, climaDinamico, eventos, paradasBoxes,
                politicaPitStop, new TireStrategyService());
    }

    QualifyingService(QualifyingDataPort datos, LapTimeCalculator calculadora,
                      DynamicWeatherService climaDinamico, EventManager eventos,
                      PitStopService paradasBoxes, PitStopPolicy politicaPitStop,
                      TireStrategyService estrategiaNeumaticos) {
        this.datos = datos;
        this.calculadora = calculadora;
        this.calculadoraTelemetria = new TelemetryCalculator();
        this.calculadoraSectores = new SectorTimeCalculator();
        this.evolucionPista = new TrackEvolutionService();
        this.climaDinamico = climaDinamico;
        this.eventos = eventos;
        this.fabricaContextoEventos = new EventContextFactory();
        this.efectosEventos = new EventEffectService();
        this.paradasBoxes = paradasBoxes;
        this.politicaPitStop = politicaPitStop;
        this.estrategiaNeumaticos = estrategiaNeumaticos;
        this.pilotos = new DriverService(datos);
        this.vehiculos = new VehicleService(datos);
        this.circuitos = new CircuitService(datos);
    }

    /** Elige el clima de la sesión según la distribución típica del circuito. */
    public WeatherCondition generarClima(Circuit circuito) {
        double tirada = RandomUtils.randomDouble(0, 1);
        double acumulado = 0;
        for (WeatherCondition clima : WeatherCondition.values()) {
            acumulado += circuito.probabilidadDe(clima);
            if (tirada <= acumulado) {
                return clima;
            }
        }
        return WeatherCondition.SECO;
    }

    /**
     * Simula la sesión completa. El piloto elegido por el usuario corre con
     * su configuración; el resto de la parrilla usa la configuración de clasificación.
     *
     * @param progreso callback opcional (piloto procesado, total, mensaje).
     */
    public QualifyingSession simular(SimulationConfig config, WeatherCondition clima, Progreso progreso) {
        return simular(config, clima, progreso, null);
    }

    QualifyingSession simular(SimulationConfig config, WeatherCondition clima,
                              Progreso progreso, Evolucion evolucion) {
        return simular(config, clima, progreso, evolucion, null);
    }

    QualifyingSession simular(SimulationConfig config, WeatherCondition clima,
                              Progreso progreso, Evolucion evolucion, Telemetria telemetria) {
        return simular(config, clima, progreso, evolucion, telemetria, null);
    }

    QualifyingSession simular(SimulationConfig config, WeatherCondition clima,
                              Progreso progreso, Evolucion evolucion, Telemetria telemetria,
                              EvolucionPista observadorPista) {
        return simular(config, clima, progreso, evolucion, telemetria,
                observadorPista, null);
    }

    QualifyingSession simular(SimulationConfig config, WeatherCondition clima,
                              Progreso progreso, Evolucion evolucion, Telemetria telemetria,
                              EvolucionPista observadorPista,
                              ClasificacionEnVivo clasificacionEnVivo) {
        return simular(config, clima, progreso, evolucion, telemetria,
                observadorPista, clasificacionEnVivo, null);
    }

    QualifyingSession simular(SimulationConfig config, WeatherCondition clima,
                              Progreso progreso, Evolucion evolucion, Telemetria telemetria,
                              EvolucionPista observadorPista,
                              ClasificacionEnVivo clasificacionEnVivo,
                              ControlSimulacion controlSimulacion) {
        return simular(config, clima, progreso, evolucion, telemetria,
                observadorPista, clasificacionEnVivo, controlSimulacion, null);
    }

    QualifyingSession simular(SimulationConfig config, WeatherCondition clima,
                              Progreso progreso, Evolucion evolucion, Telemetria telemetria,
                              EvolucionPista observadorPista,
                              ClasificacionEnVivo clasificacionEnVivo,
                              ControlSimulacion controlSimulacion,
                              EventosEnVivo observadorEventos) {
        return simular(config, clima, progreso, evolucion, telemetria,
                observadorPista, clasificacionEnVivo, controlSimulacion,
                observadorEventos, null);
    }

    QualifyingSession simular(SimulationConfig config, WeatherCondition clima,
                              Progreso progreso, Evolucion evolucion, Telemetria telemetria,
                              EvolucionPista observadorPista,
                              ClasificacionEnVivo clasificacionEnVivo,
                              ControlSimulacion controlSimulacion,
                              EventosEnVivo observadorEventos,
                              PitStopsEnVivo observadorPitStops) {
        return simular(config, clima, progreso, evolucion, telemetria,
                observadorPista, clasificacionEnVivo, controlSimulacion,
                observadorEventos, observadorPitStops, null);
    }

    QualifyingSession simular(SimulationConfig config, WeatherCondition clima,
                              Progreso progreso, Evolucion evolucion, Telemetria telemetria,
                              EvolucionPista observadorPista,
                              ClasificacionEnVivo clasificacionEnVivo,
                              ControlSimulacion controlSimulacion,
                              EventosEnVivo observadorEventos,
                              PitStopsEnVivo observadorPitStops,
                              CambiosNeumaticosEnVivo observadorCambiosNeumaticos) {
        Circuit circuito = validarSeleccion(config);
        if (clima == null) {
            throw new ValidationException("Las condiciones climáticas no pueden ser nulas");
        }

        List<Driver> parrilla = participantesConSeleccionPrimero(config.getPilotoId());
        List<LapResult> resultados = new ArrayList<>();
        List<EventOccurrence> eventosSesion = new ArrayList<>();
        List<SimulationSnapshot> evolucionSeleccionada = new ArrayList<>();
        List<TelemetrySnapshot> evolucionVuelta = new ArrayList<>();
        List<TrackEvolutionSnapshot> historialPista = new ArrayList<>();
        List<WeatherSnapshot> climaBase = climaDinamico.generar(
                circuito, clima, SEGMENTOS_EVOLUCION);
        List<WeatherSnapshot> climaSeleccionado = List.of();
        double gomaPista = 0;
        eventos.startSession();
        paradasBoxes.startSession();
        estrategiaNeumaticos.startSession(
                config.getPilotoId(), config.getCompuestoInicial());

        for (int i = 0; i < parrilla.size(); i++) {
            Driver piloto = parrilla.get(i);
            Optional<Vehicle> vehiculo = vehiculos.delPiloto(piloto.getId());
            if (vehiculo.isEmpty()) {
                continue;
            }
            Vehicle coche = vehiculo.get();

            // La selección pertenece a un piloto, no al monoplaza completo:
            // así su compañero mantiene la estrategia general de la parrilla.
            SimulationConfig configPiloto = piloto.getId() == config.getPilotoId()
                    ? config
                    : SimulationConfig.paraClasificacion();

            TrackEvolutionService.Evolution pistaContexto = evolucionPista.evolucionar(
                    climaBase, gomaPista, i + 1, piloto.getNombre());
            double desgastePrevio = calculadora.desgastePorVuelta(
                    coche, circuito, pistaContexto.clima(), configPiloto);
            EventContext contexto = fabricaContextoEventos.create(
                    i + 1, piloto, coche, configPiloto, pistaContexto.clima(), desgastePrevio);
            List<EventOccurrence> eventosVuelta = eventos.resolve(contexto);
            climaBase = efectosEventos.applyGlobalWeather(climaBase, eventosVuelta);
            TrackEvolutionService.Evolution pistaVuelta = evolucionPista.evolucionar(
                    climaBase, gomaPista, i + 1, piloto.getNombre());
            List<WeatherSnapshot> climaVuelta = pistaVuelta.clima();
            gomaPista = pistaVuelta.gomaFinalPorcentaje();
            historialPista.add(pistaVuelta.resumen());
            if (observadorPista != null) {
                observadorPista.actualizar(pistaVuelta.resumen());
            }

            double tiempoBase = calculadora.calcularTiempo(
                    piloto, coche, circuito, climaVuelta, configPiloto);
            double consumoBase = calculadora.consumoPorVuelta(
                    coche, circuito, climaVuelta, configPiloto);
            double desgasteBase = calculadora.desgastePorVuelta(
                    coche, circuito, climaVuelta, configPiloto);

            LapResult resultado = new LapResult(piloto.getId(), piloto.getNombre(),
                    piloto.getEquipo(), coche.getModelo(), tiempoBase);
            efectosEventos.applyResult(resultado, tiempoBase, consumoBase,
                    desgasteBase, eventosVuelta);
            if (resultado.isVueltaValida()) {
                resultado.setSectorTimes(calculadoraSectores.calcular(
                        tiempoBase, resultado.getTiempoSegundos(),
                        climaVuelta, eventosVuelta));
            }
            resultados.add(resultado);
            eventosVuelta.stream()
                    .filter(EventOccurrence::ocurrio)
                    .forEach(eventosSesion::add);

            if (piloto.getId() == config.getPilotoId()) {
                climaSeleccionado = climaVuelta;
                emitirMuestras(piloto, coche, circuito, climaVuelta, configPiloto,
                        resultado, tiempoBase, eventosVuelta,
                        evolucionSeleccionada::add, evolucionVuelta::add);
            }

            if (progreso != null) {
                progreso.avanzar(i + 1, parrilla.size(),
                        piloto.getNombre() + " — " + formatoResultado(resultado));
            }
        }

        ordenarParrilla(resultados);
        EstadoReproduccion estadoReproduccion = reproducirVueltaEnVivo(
                resultados, evolucionSeleccionada, evolucionVuelta,
                evolucion, telemetria, clasificacionEnVivo, controlSimulacion,
                eventosSesion, observadorEventos, observadorPitStops,
                observadorCambiosNeumaticos);
        List<LapResult> resultadosGuardados = estadoReproduccion.completa()
                ? resultadosConEstrategia(resultados)
                : estadoReproduccion.clasificacion();
        int segmentosGenerados = estadoReproduccion.segmentosGenerados();

        QualifyingSession sesion = new QualifyingSession(circuito.getNombre(), clima, config);
        sesion.setResultados(resultadosGuardados);
        List<WeatherSnapshot> climaSesion = climaSeleccionado.isEmpty()
                ? climaBase : climaSeleccionado;
        sesion.setEvolucionClimatica(primeros(climaSesion, segmentosGenerados));
        sesion.setEvolucionVuelta(primeros(evolucionVuelta, segmentosGenerados));
        sesion.setEvolucionPista(historialPista);
        sesion.setEventos(eventosHasta(eventosSesion, segmentosGenerados));
        sesion.setParadasBoxes(paradasBoxes.history());
        sesion.setCambiosNeumaticos(estrategiaNeumaticos.history());
        sesion.setFecha(DateUtils.format(DateUtils.now()));
        config.setGuardadoEn(sesion.getFecha());
        return sesion;
    }

    /**
     * Reproduce una vuelta común para toda la parrilla. Los fotogramas visuales
     * son más frecuentes que los microsectores del dominio: la torre se mueve
     * con fluidez, mientras eventos, telemetría y estrategia avanzan una sola
     * vez al cruzar cada microsector.
     */
    private EstadoReproduccion reproducirVueltaEnVivo(
                                        List<LapResult> resultados,
                                        List<SimulationSnapshot> evolucionSeleccionada,
                                        List<TelemetrySnapshot> telemetriaSeleccionada,
                                        Evolucion evolucion, Telemetria telemetria,
                                        ClasificacionEnVivo clasificacionEnVivo,
                                        ControlSimulacion controlSimulacion,
                                        List<EventOccurrence> eventosSesion,
                                        EventosEnVivo observadorEventos,
                                        PitStopsEnVivo observadorPitStops,
                                        CambiosNeumaticosEnVivo observadorCambiosNeumaticos) {
        List<LapResult> ultimaClasificacion = List.of();
        int segmentosGenerados = 0;
        int totalFotogramas = controlSimulacion == null
                ? SEGMENTOS_EVOLUCION
                : controlSimulacion.totalFotogramas(SEGMENTOS_EVOLUCION);
        for (int fotograma = 1; fotograma <= totalFotogramas; fotograma++) {
            double progreso = fotograma / (double) totalFotogramas;
            double progresoEnSegmentos = progreso * SEGMENTOS_EVOLUCION;
            int segmento = Math.min(SEGMENTOS_EVOLUCION,
                    Math.max(1, (int) Math.ceil(progresoEnSegmentos)));
            boolean nuevoSegmento = segmento > segmentosGenerados;

            if (nuevoSegmento) {
                TrackSector sector = TrackSector.desdeSegmento(segmento, SEGMENTOS_EVOLUCION);
                TrackSector sectorAnterior = segmento == 1 ? TrackSector.NONE
                        : TrackSector.desdeSegmento(segmento - 1, SEGMENTOS_EVOLUCION);
                if (observadorEventos != null && sector != sectorAnterior) {
                    eventosSesion.stream()
                            .filter(EventOccurrence::ocurrio)
                            .filter(evento -> evento.sector() == sector)
                            .forEach(observadorEventos::actualizar);
                }
                List<LapResult> clasificacionAntesDeBoxes =
                        clasificacionEnSegmento(resultados, segmento);
                clasificacionAntesDeBoxes.stream()
                        .filter(resultado -> !paradasBoxes.hasStop(resultado.getPilotoId()))
                        .map(resultado -> politicaPitStop.evaluate(
                                resultado, segmento, SEGMENTOS_EVOLUCION))
                        .flatMap(Optional::stream)
                        .forEach(decision -> paradasBoxes.start(
                                decision, clasificacionAntesDeBoxes,
                                segmento, SEGMENTOS_EVOLUCION));
                paradasBoxes.advance(segmento);
                ultimaClasificacion = clasificacionEnSegmento(resultados, segmento);
                List<PitStopRecord> actualizacionesBoxes =
                        paradasBoxes.collectUpdates(ultimaClasificacion);
                if (observadorPitStops != null) {
                    actualizacionesBoxes.forEach(observadorPitStops::actualizar);
                }
                List<LapResult> clasificacionActual = ultimaClasificacion;
                List<TireChangeRecord> cambios = actualizacionesBoxes.stream()
                        .flatMap(parada -> clasificacionActual.stream()
                                .filter(resultado -> resultado.getPilotoId() == parada.pilotoId())
                                .findFirst()
                                .flatMap(resultado -> estrategiaNeumaticos.changeDuring(
                                        parada, resultado))
                                .stream())
                        .toList();
                if (observadorCambiosNeumaticos != null) {
                    cambios.forEach(observadorCambiosNeumaticos::actualizar);
                }
                if (evolucion != null && segmento <= evolucionSeleccionada.size()) {
                    evolucion.actualizar(evolucionSeleccionada.get(segmento - 1));
                }
                if (telemetria != null && segmento <= telemetriaSeleccionada.size()) {
                    telemetria.actualizar(telemetriaSeleccionada.get(segmento - 1));
                }
                segmentosGenerados = segmento;
            }

            ultimaClasificacion = clasificacionEnProgreso(
                    resultados, progresoEnSegmentos, segmento);
            if (clasificacionEnVivo != null) {
                clasificacionEnVivo.actualizar(new LiveClassificationFrame(
                        fotograma, totalFotogramas, segmento, SEGMENTOS_EVOLUCION,
                        progreso, ultimaClasificacion));
            }
            if (controlSimulacion != null
                    && !controlSimulacion.completarFotograma(fotograma, totalFotogramas)) {
                break;
            }
        }
        return new EstadoReproduccion(ultimaClasificacion, segmentosGenerados,
                segmentosGenerados == SEGMENTOS_EVOLUCION);
    }

    private <T> List<T> primeros(List<T> muestras, int cantidad) {
        return List.copyOf(muestras.subList(0, Math.min(cantidad, muestras.size())));
    }

    private List<EventOccurrence> eventosHasta(List<EventOccurrence> eventos, int segmento) {
        TrackSector ultimoSector = segmento <= 0
                ? TrackSector.NONE : TrackSector.desdeSegmento(segmento, SEGMENTOS_EVOLUCION);
        return eventos.stream()
                .filter(evento -> evento.sector().ordinal() <= ultimoSector.ordinal())
                .toList();
    }

    private record EstadoReproduccion(List<LapResult> clasificacion,
                                      int segmentosGenerados,
                                      boolean completa) { }

    private List<LapResult> clasificacionEnSegmento(List<LapResult> resultados,
                                                     int segmento) {
        return clasificacionEnProgreso(resultados, segmento, segmento);
    }

    private List<LapResult> clasificacionEnProgreso(List<LapResult> resultados,
                                                     double progresoEnSegmentos,
                                                     int segmentoActual) {
        List<LapResult> parcial = resultados.stream()
                .map(resultado -> resultadoEnProgreso(
                        resultado, progresoEnSegmentos, segmentoActual))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        ordenarParrilla(parcial);
        return List.copyOf(parcial);
    }

    private LapResult resultadoEnProgreso(LapResult resultado,
                                          double progresoEnSegmentos,
                                          int segmentoActual) {
        LapResult parcial = copiarResultado(resultado);
        parcial.setTiempoSegundos(tiempoAcumulado(resultado, progresoEnSegmentos));
        parcial.setTiempoSegundos(parcial.getTiempoSegundos()
                + paradasBoxes.timeLossFor(resultado.getPilotoId())
                + estrategiaNeumaticos.timeAdjustmentFor(
                        resultado.getPilotoId(), resultado.getTiempoSegundos(),
                        segmentoActual, SEGMENTOS_EVOLUCION));
        parcial.setDesgasteEstimado(estrategiaNeumaticos.wearFor(
                resultado.getPilotoId(), resultado.getDesgasteEstimado(),
                segmentoActual, SEGMENTOS_EVOLUCION));

        boolean incidenteAlcanzado = !resultado.isVueltaValida()
                && (resultado.getSectorIncidente() == TrackSector.NONE
                    || resultado.getSectorIncidente().contiene(segmentoActual, SEGMENTOS_EVOLUCION)
                    || TrackSector.desdeSegmento(segmentoActual, SEGMENTOS_EVOLUCION).ordinal()
                        > resultado.getSectorIncidente().ordinal());
        parcial.setEstadoVuelta(incidenteAlcanzado
                ? resultado.getEstadoVuelta() : LapStatus.VALID);
        parcial.setSectorIncidente(incidenteAlcanzado
                ? resultado.getSectorIncidente() : TrackSector.NONE);
        parcial.setEventos(eventosHasta(resultado.getEventos(), segmentoActual));
        return parcial;
    }

    private List<LapResult> resultadosConEstrategia(List<LapResult> resultados) {
        Map<Integer, PitStopRecord> porPiloto = paradasBoxes.history().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PitStopRecord::pilotoId, parada -> parada,
                        (anterior, actual) -> actual));
        List<LapResult> ajustados = resultados.stream()
                .map(this::copiarResultado)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (LapResult resultado : ajustados) {
            PitStopRecord parada = porPiloto.get(resultado.getPilotoId());
            if (!resultado.isVueltaValida()) {
                continue;
            }
            double tiempoBase = resultado.getTiempoSegundos();
            double ajusteNeumatico = estrategiaNeumaticos.timeAdjustmentFor(
                    resultado.getPilotoId(), tiempoBase,
                    SEGMENTOS_EVOLUCION, SEGMENTOS_EVOLUCION);
            resultado.setTiempoSegundos(tiempoBase
                    + (parada == null ? 0 : parada.tiempoPerdidoSegundos())
                    + ajusteNeumatico);
            resultado.setDesgasteEstimado(estrategiaNeumaticos.wearFor(
                    resultado.getPilotoId(), resultado.getDesgasteEstimado(),
                    SEGMENTOS_EVOLUCION, SEGMENTOS_EVOLUCION));
            if (parada != null && resultado.hasSectorTimes()) {
                TrackSector sector = TrackSector.desdeSegmento(
                        parada.segmentoEntrada(), SEGMENTOS_EVOLUCION);
                resultado.setSectorTimes(resultado.getSectorTimes()
                        .conTiempoAdicional(sector, parada.tiempoPerdidoSegundos()));
            }
            if (resultado.hasSectorTimes()) {
                resultado.setSectorTimes(sectoresConAjusteNeumatico(
                        resultado.getSectorTimes(), resultado.getPilotoId(), tiempoBase));
            }
        }
        ordenarParrilla(ajustados);
        paradasBoxes.updateFinalPositions(ajustados);
        return List.copyOf(ajustados);
    }

    /** Distribuye el efecto de cada stint sin romper la suma de los parciales. */
    private SectorTimes sectoresConAjusteNeumatico(
            SectorTimes sectores, int pilotoId, double tiempoBase) {
        double hastaS1 = estrategiaNeumaticos.timeAdjustmentFor(
                pilotoId, tiempoBase, 7, SEGMENTOS_EVOLUCION);
        double hastaS2 = estrategiaNeumaticos.timeAdjustmentFor(
                pilotoId, tiempoBase, 14, SEGMENTOS_EVOLUCION);
        double hastaMeta = estrategiaNeumaticos.timeAdjustmentFor(
                pilotoId, tiempoBase, SEGMENTOS_EVOLUCION, SEGMENTOS_EVOLUCION);
        return sectores
                .conAjusteTiempo(TrackSector.SECTOR_1, hastaS1)
                .conAjusteTiempo(TrackSector.SECTOR_2, hastaS2 - hastaS1)
                .conAjusteTiempo(TrackSector.SECTOR_3, hastaMeta - hastaS2);
    }

    private double tiempoAcumulado(LapResult resultado, double progresoEnSegmentos) {
        double tiempoBase;
        if (!resultado.hasSectorTimes()) {
            tiempoBase = resultado.getTiempoSegundos()
                    * progresoEnSegmentos / SEGMENTOS_EVOLUCION;
        } else {
            var sectores = resultado.getSectorTimes();
            if (progresoEnSegmentos <= 7) {
                tiempoBase = sectores.sector1Seconds() * progresoEnSegmentos / 7.0;
            } else if (progresoEnSegmentos <= 14) {
                tiempoBase = sectores.sector1Seconds()
                        + sectores.sector2Seconds() * (progresoEnSegmentos - 7) / 7.0;
            } else {
                tiempoBase = sectores.sector1Seconds() + sectores.sector2Seconds()
                        + sectores.sector3Seconds() * (progresoEnSegmentos - 14) / 6.0;
            }
        }

        // Tráfico, frenada y tracción cambian el orden en microsectores. La
        // envolvente vale cero en meta, preservando exactamente el resultado final.
        double progreso = progresoEnSegmentos / SEGMENTOS_EVOLUCION;
        double variacionMicrosector = 1.25 * Math.sin(Math.PI * progreso)
                * Math.sin(resultado.getPilotoId() * 1.37 + 4 * Math.PI * progreso);
        return Math.max(0.001, tiempoBase + variacionMicrosector);
    }

    /** Ordena por tiempo, asigna posiciones y calcula la diferencia con la pole. */
    void ordenarParrilla(List<LapResult> resultados) {
        resultados.sort(Comparator
                .comparing((LapResult resultado) -> !resultado.isVueltaValida())
                .thenComparingDouble(LapResult::getTiempoSegundos));
        LapResult poleResult = resultados.stream()
                .filter(LapResult::isVueltaValida)
                .findFirst()
                .orElse(null);
        if (poleResult == null) {
            for (int i = 0; i < resultados.size(); i++) {
                resultados.get(i).setPosicion(i + 1);
                resultados.get(i).setGap(0);
            }
            return;
        }
        double pole = poleResult.getTiempoSegundos();
        for (int i = 0; i < resultados.size(); i++) {
            LapResult resultado = resultados.get(i);
            resultado.setPosicion(i + 1);
            resultado.setGap(resultado.isVueltaValida()
                    ? resultado.getTiempoSegundos() - pole : 0);
        }
    }

    /** Copia estable para no compartir resultados mutables con el hilo JavaFX. */
    private LapResult copiarResultado(LapResult original) {
        LapResult copia = new LapResult(original.getPilotoId(), original.getPiloto(),
                original.getEquipo(), original.getVehiculo(), original.getTiempoSegundos());
        copia.setPosicion(original.getPosicion());
        copia.setGap(original.getGap());
        copia.setConsumoEstimado(original.getConsumoEstimado());
        copia.setDesgasteEstimado(original.getDesgasteEstimado());
        copia.setSectorTimes(original.getSectorTimes());
        copia.setEstadoVuelta(original.getEstadoVuelta());
        copia.setSectorIncidente(original.getSectorIncidente());
        copia.setEventos(original.getEventos());
        return copia;
    }

    private List<Driver> participantesConSeleccionPrimero(int pilotoSeleccionadoId) {
        List<Driver> participantes = new ArrayList<>(pilotos.listar());
        participantes.sort(Comparator
                .comparing((Driver piloto) -> piloto.getId() != pilotoSeleccionadoId)
                .thenComparingInt(Driver::getId));
        return participantes;
    }

    /**
     * Divide la vuelta seleccionada en muestras compartidas por la evolución
     * básica y la telemetría detallada.
     */
    private void emitirMuestras(Driver piloto, Vehicle vehiculo, Circuit circuito,
                                List<WeatherSnapshot> clima, SimulationConfig config,
                                LapResult resultado, double tiempoBase,
                                List<EventOccurrence> eventosVuelta,
                                Evolucion evolucion, Telemetria telemetria) {
        double velocidadMedia = 3600 * circuito.getLongitudKm() / tiempoBase;
        double sumaTiempo = clima.stream().mapToDouble(WeatherSnapshot::factorTiempo).sum();
        double sumaConsumo = clima.stream().mapToDouble(WeatherSnapshot::factorConsumo).sum();
        double sumaDesgaste = clima.stream().mapToDouble(WeatherSnapshot::factorDesgaste).sum();
        double factorTiempoMedio = sumaTiempo / clima.size();
        double tiempoAcumulado = 0;
        double consumoAcumulado = 0;
        double desgasteAcumulado = 0;

        for (int segmento = 1; segmento <= SEGMENTOS_EVOLUCION; segmento++) {
            double progreso = segmento / (double) SEGMENTOS_EVOLUCION;
            WeatherSnapshot climaActual = clima.get(segmento - 1);
            TrackSector sector = TrackSector.desdeSegmento(segmento, SEGMENTOS_EVOLUCION);
            EventImpact impacto = efectosEventos.impactAt(eventosVuelta, sector);
            EventOccurrence eventoActual = efectosEventos.eventAt(
                    eventosVuelta, sector, piloto.getId(), piloto.getNombre(), 1);
            WeatherSnapshot climaTelemetria = impacto.deltaGripPorcentaje() == 0
                    ? climaActual
                    : climaActual.conImpacto(0, impacto.deltaGripPorcentaje());
            boolean invalidada = efectosEventos.invalidatedAtOrBefore(eventosVuelta, sector);
            double velocidad = calcularVelocidad(
                    vehiculo, config, velocidadMedia, factorTiempoMedio,
                    climaActual, progreso, impacto.multiplicadorVelocidad());
            if (invalidada) {
                velocidad = 0;
            } else {
                tiempoAcumulado += tiempoBase * climaActual.factorTiempo() / sumaTiempo;
                tiempoAcumulado += deltaTiempoSegmento(eventosVuelta, sector);
                // Las fracciones suman matemáticamente el total, pero en el
                // último segmento un double puede excederlo por unas millonésimas.
                // Se limita en cada paso para preservar el invariante del snapshot.
                consumoAcumulado = acumularHastaTotal(
                        consumoAcumulado,
                        resultado.getConsumoEstimado()
                                * climaActual.factorConsumo() / sumaConsumo,
                        resultado.getConsumoEstimado());
                desgasteAcumulado = acumularHastaTotal(
                        desgasteAcumulado,
                        resultado.getDesgasteEstimado()
                                * climaActual.factorDesgaste() / sumaDesgaste,
                        resultado.getDesgasteEstimado());
                if (segmento == SEGMENTOS_EVOLUCION) {
                    // La lectura final representa el resultado consolidado,
                    // no una aproximación binaria unas millonésimas inferior.
                    consumoAcumulado = resultado.getConsumoEstimado();
                    desgasteAcumulado = resultado.getDesgasteEstimado();
                }
            }
            if (invalidada) {
                consumoAcumulado = resultado.getConsumoEstimado();
                desgasteAcumulado = resultado.getDesgasteEstimado();
            }

            if (evolucion != null) {
                evolucion.actualizar(new SimulationSnapshot(
                        piloto.getNombre(),
                        vehiculo.getModelo(),
                        segmento,
                        SEGMENTOS_EVOLUCION,
                        velocidad,
                        vehiculo.getVelocidadMaximaKmh(),
                        consumoAcumulado,
                        resultado.getConsumoEstimado(),
                        desgasteAcumulado,
                        resultado.getDesgasteEstimado()));
            }
            if (telemetria != null) {
                telemetria.actualizar(calculadoraTelemetria.calcular(
                        piloto, vehiculo, circuito, climaTelemetria, config, resultado,
                        segmento, SEGMENTOS_EVOLUCION, progreso, velocidad,
                        resultado.getConsumoEstimado() <= 0
                                ? 100
                                : MathUtils.clamp(100 * (1 - consumoAcumulado
                                        / resultado.getConsumoEstimado()), 0, 100),
                        desgasteAcumulado, tiempoAcumulado, tiempoBase,
                        impacto, eventoActual,
                        invalidada ? resultado.getEstadoVuelta() : LapStatus.VALID));
            }
        }
    }

    /**
     * Calcula velocidad instantánea sin confundirla con la media de la vuelta.
     * La punta depende del rendimiento del modo activo y nunca supera el máximo
     * declarado por el vehículo; el perfil conserva zonas lentas y rectas.
     */
    static double calcularVelocidad(Vehicle vehiculo, SimulationConfig config,
                                    double velocidadMedia, double factorTiempoMedio,
                                    WeatherSnapshot clima, double progreso,
                                    double multiplicadorEvento) {
        double mediaDelSegmento = velocidadMedia
                * factorTiempoMedio / clima.factorTiempo();
        double rendimientoActivo = vehiculo.rendimientoDe(config.getModo())
                .getVelocidadPromedioKmh();
        double mejorRendimiento = vehiculo.getRendimiento().values().stream()
                .mapToDouble(Vehicle.Performance::getVelocidadPromedioKmh)
                .max()
                .orElse(rendimientoActivo);
        double factorModo = mejorRendimiento <= 0
                ? 1
                : MathUtils.clamp(rendimientoActivo / mejorRendimiento, 0.75, 1);
        double puntaDisponible = vehiculo.getVelocidadMaximaKmh()
                * factorModo / Math.max(1, clima.factorTiempo());
        puntaDisponible = Math.max(mediaDelSegmento, puntaDisponible);

        // El perfil vale 1 en una recta y presenta valles en las zonas lentas.
        double perfil = MathUtils.clamp(
                0.65 * Math.sin(2 * Math.PI * progreso)
                        - 0.35 * Math.cos(4 * Math.PI * progreso),
                -1, 1);
        double velocidad;
        if (perfil >= 0) {
            velocidad = mediaDelSegmento
                    + (puntaDisponible - mediaDelSegmento) * perfil;
        } else {
            double velocidadLenta = mediaDelSegmento * 0.55;
            velocidad = mediaDelSegmento
                    + (mediaDelSegmento - velocidadLenta) * perfil;
        }
        return MathUtils.clamp(velocidad * multiplicadorEvento,
                0, vehiculo.getVelocidadMaximaKmh());
    }

    private double deltaTiempoSegmento(List<EventOccurrence> eventosVuelta,
                                       TrackSector sector) {
        double delta = eventosVuelta.stream()
                .filter(EventOccurrence::ocurrio)
                .filter(evento -> evento.sector() == sector)
                .mapToDouble(evento -> evento.impacto().deltaTiempoSegundos())
                .sum();
        long segmentos = java.util.stream.IntStream.rangeClosed(1, SEGMENTOS_EVOLUCION)
                .filter(segmento -> sector.contiene(segmento, SEGMENTOS_EVOLUCION))
                .count();
        return segmentos == 0 ? 0 : delta / segmentos;
    }

    private double acumularHastaTotal(double acumulado, double incremento, double total) {
        return MathUtils.clamp(acumulado + incremento, 0, total);
    }

    private String formatoResultado(LapResult resultado) {
        return resultado.isVueltaValida()
                ? FormatUtils.formatLapTime(resultado.getTiempoSegundos())
                : resultado.getEstadoVuelta().getEtiqueta();
    }

    /**
     * Protege la regla de negocio de HU-08 incluso cuando la simulación se
     * inicia fuera de JavaFX: el piloto debe existir y conducir el vehículo.
     */
    Circuit validarSeleccion(SimulationConfig config) {
        if (config == null) {
            throw new ValidationException("La configuración no puede ser nula");
        }
        if (!ValidationUtils.isNotBlank(config.getCircuito())) {
            throw new ValidationException("Debes seleccionar un circuito");
        }
        if (!ValidationUtils.isNotBlank(config.getVehiculo())) {
            throw new ValidationException("Debes seleccionar un vehículo");
        }
        if (config.getModo() == null || config.getAerodinamica() == null
                || config.getPresion() == null || config.getCombustible() == null) {
            throw new ValidationException("Debes completar todos los ajustes del vehículo");
        }

        Circuit circuito = circuitos.porNombre(config.getCircuito())
                .orElseThrow(() -> new ValidationException("El circuito no existe: " + config.getCircuito()));
        Vehicle vehiculo = vehiculos.porModelo(config.getVehiculo())
                .orElseThrow(() -> new ValidationException("El vehículo no existe: " + config.getVehiculo()));

        Integer pilotoId = config.getPilotoId();
        if (pilotoId == null) {
            throw new ValidationException("Debes seleccionar un piloto");
        }
        Driver piloto = pilotos.porId(pilotoId)
                .orElseThrow(() -> new ValidationException("El piloto no existe: " + pilotoId));
        if (!vehiculo.conduce(pilotoId)) {
            throw new ValidationException(
                    piloto.getNombre() + " no conduce el vehículo " + vehiculo.getModelo());
        }

        return circuito;
    }

    /** Guarda la sesión y, con ella, la configuración empleada. */
    public void guardar(QualifyingSession sesion) {
        datos.guardarSesion(sesion);
    }

    public List<QualifyingSession> historial() {
        return datos.sesiones().stream()
                .sorted(Comparator.comparing(QualifyingSession::getFecha,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Notificación de avance durante la simulación. */
    @FunctionalInterface
    public interface Progreso {
        void avanzar(int hechos, int total, String mensaje);
    }

    /** Se invoca en el hilo de simulación; la UI debe despachar al hilo de JavaFX. */
    @FunctionalInterface
    public interface Evolucion {
        void actualizar(SimulationSnapshot muestra);
    }

    /** Se invoca en el hilo de simulación; la UI debe despachar al hilo de JavaFX. */
    @FunctionalInterface
    public interface Telemetria {
        void actualizar(TelemetrySnapshot muestra);
    }

    /** Se invoca tras el paso de cada vehículo por la pista. */
    @FunctionalInterface
    public interface EvolucionPista {
        void actualizar(TrackEvolutionSnapshot muestra);
    }

    /** Clasificación provisional y progreso continuo de la reproducción. */
    @FunctionalInterface
    public interface ClasificacionEnVivo {
        void actualizar(LiveClassificationFrame fotograma);
    }

    /** Evento de cualquier piloto, emitido al comenzar el sector afectado. */
    @FunctionalInterface
    public interface EventosEnVivo {
        void actualizar(EventOccurrence evento);
    }

    /** Cambio de fase de una parada, emitido después de reordenar la parrilla. */
    @FunctionalInterface
    public interface PitStopsEnVivo {
        void actualizar(PitStopRecord parada);
    }

    /** Cambio de compuesto confirmado durante la detención en boxes. */
    @FunctionalInterface
    public interface CambiosNeumaticosEnVivo {
        void actualizar(TireChangeRecord cambio);
    }

    /** Punto de extensión del reloj: permite temporizar o finalizar sin acoplar la UI. */
    @FunctionalInterface
    interface ControlSimulacion {
        default int totalFotogramas(int minimo) {
            return minimo;
        }

        boolean completarFotograma(int fotograma, int totalFotogramas);
    }
}
