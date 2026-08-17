package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.event.EventContext;
import com.formula1.event.EventContextFactory;
import com.formula1.event.EventEffectService;
import com.formula1.event.EventManager;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.EventImpact;
import com.formula1.model.EventOccurrence;
import com.formula1.model.LapResult;
import com.formula1.model.LapStatus;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SessionStatistics;
import com.formula1.model.SimulationConfig;
import com.formula1.model.SimulationSnapshot;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.TrackSector;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.model.WeatherSnapshot;
import com.formula1.util.DateUtils;
import com.formula1.util.FormatUtils;
import com.formula1.util.MathUtils;
import com.formula1.util.RandomUtils;
import com.formula1.util.ValidationUtils;

import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

/**
 * Ejecuta una sesión de clasificación: genera el clima, calcula el tiempo de
 * los 20 pilotos, ordena la parrilla y guarda el resultado.
 */
public class QualifyingService {

    /**
     * Pausa por piloto. Sin ella la sesión terminaría en microsegundos y no
     * se podría apreciar que la interfaz sigue respondiendo mientras el
     * cálculo corre en otro hilo.
     */
    private static final long RITMO_MS = 80;
    private static final long RITMO_EVOLUCION_MS = 60;
    static final int SEGMENTOS_EVOLUCION = 20;

    private final DataStore datos;
    private final DriverService pilotos;
    private final VehicleService vehiculos;
    private final CircuitService circuitos;
    private final LapTimeCalculator calculadora;
    private final TelemetryCalculator calculadoraTelemetria;
    private final DynamicWeatherService climaDinamico;
    private final EventManager eventos;
    private final EventContextFactory fabricaContextoEventos;
    private final EventEffectService efectosEventos;

    public QualifyingService() {
        this(DataStore.getInstance(), new LapTimeCalculator());
    }

    /** Permite reproducir únicamente la secuencia de eventos de una ejecución. */
    public QualifyingService(long eventSeed) {
        this(DataStore.getInstance(), new LapTimeCalculator(),
                new DynamicWeatherService(), new EventManager(eventSeed));
    }

    public QualifyingService(DataStore datos, LapTimeCalculator calculadora) {
        this(datos, calculadora, new DynamicWeatherService());
    }

    QualifyingService(DataStore datos, LapTimeCalculator calculadora,
                      DynamicWeatherService climaDinamico) {
        this(datos, calculadora, climaDinamico, new EventManager());
    }

    QualifyingService(DataStore datos, LapTimeCalculator calculadora,
                      DynamicWeatherService climaDinamico, EventManager eventos) {
        this.datos = datos;
        this.calculadora = calculadora;
        this.calculadoraTelemetria = new TelemetryCalculator();
        this.climaDinamico = climaDinamico;
        this.eventos = eventos;
        this.fabricaContextoEventos = new EventContextFactory();
        this.efectosEventos = new EventEffectService();
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
        Circuit circuito = validarSeleccion(config);
        if (clima == null) {
            throw new ValidationException("Las condiciones climáticas no pueden ser nulas");
        }

        List<Driver> parrilla = participantesConSeleccionPrimero(config.getPilotoId());
        List<LapResult> resultados = new ArrayList<>();
        List<EventOccurrence> eventosSesion = new ArrayList<>();
        List<WeatherSnapshot> evolucionClimatica = climaDinamico.generar(
                circuito, clima, SEGMENTOS_EVOLUCION);
        eventos.startSession();

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

            double desgastePrevio = calculadora.desgastePorVuelta(
                    coche, circuito, evolucionClimatica, configPiloto);
            EventContext contexto = fabricaContextoEventos.create(
                    i + 1, piloto, coche, configPiloto, evolucionClimatica, desgastePrevio);
            List<EventOccurrence> eventosVuelta = eventos.resolve(contexto);
            evolucionClimatica = efectosEventos.applyGlobalWeather(
                    evolucionClimatica, eventosVuelta);

            double tiempoBase = calculadora.calcularTiempo(
                    piloto, coche, circuito, evolucionClimatica, configPiloto);
            double consumoBase = calculadora.consumoPorVuelta(
                    coche, circuito, evolucionClimatica, configPiloto);
            double desgasteBase = calculadora.desgastePorVuelta(
                    coche, circuito, evolucionClimatica, configPiloto);

            LapResult resultado = new LapResult(piloto.getId(), piloto.getNombre(),
                    piloto.getEquipo(), coche.getModelo(), tiempoBase);
            efectosEventos.applyResult(resultado, tiempoBase, consumoBase,
                    desgasteBase, eventosVuelta);
            resultados.add(resultado);
            eventosVuelta.stream()
                    .filter(EventOccurrence::ocurrio)
                    .forEach(eventosSesion::add);

            if (piloto.getId() == config.getPilotoId()
                    && (evolucion != null || telemetria != null)) {
                emitirMuestras(piloto, coche, circuito, evolucionClimatica, configPiloto,
                        resultado, tiempoBase, eventosVuelta, evolucion, telemetria);
            }

            if (progreso != null) {
                progreso.avanzar(i + 1, parrilla.size(),
                        piloto.getNombre() + " — " + formatoResultado(resultado));
            }
        }

        ordenarParrilla(resultados);

        QualifyingSession sesion = new QualifyingSession(circuito.getNombre(), clima, config);
        sesion.setResultados(resultados);
        sesion.setEvolucionClimatica(evolucionClimatica);
        sesion.setEventos(eventosSesion);
        sesion.setFecha(DateUtils.format(DateUtils.now()));
        config.setGuardadoEn(sesion.getFecha());
        return sesion;
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

    /**
     * Envuelve la simulación en un {@link Task} para ejecutarla fuera del
     * hilo de JavaFX. El {@code Task} ya publica progreso y mensajes en el
     * hilo de interfaz, así que no hace falta {@code Platform.runLater}.
     */
    public Task<QualifyingSession> crearTarea(SimulationConfig config) {
        return crearTarea(config, null);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config, Evolucion evolucion) {
        return crearTarea(config, evolucion, null);
    }

    public Task<QualifyingSession> crearTarea(SimulationConfig config, Evolucion evolucion,
                                              Telemetria telemetria) {
        return new Task<>() {
            @Override
            protected QualifyingSession call() throws Exception {
                Circuit circuito = validarSeleccion(config);

                WeatherCondition clima = generarClima(circuito);
                updateMessage("Clima de la sesión: " + clima.getEtiqueta());
                Thread.sleep(RITMO_MS * 2);

                Evolucion evolucionConRitmo = evolucion == null ? null : muestra -> {
                    evolucion.actualizar(muestra);
                    if (telemetria == null) {
                        dormir(RITMO_EVOLUCION_MS);
                    }
                };
                Telemetria telemetriaConRitmo = telemetria == null ? null : muestra -> {
                    telemetria.actualizar(muestra);
                    // Ambas lecturas corresponden al mismo segmento; se pausa una sola vez.
                    dormir(RITMO_EVOLUCION_MS);
                };

                QualifyingSession sesion = simular(
                        config,
                        clima,
                        (hecho, total, mensaje) -> {
                            updateProgress(hecho, total);
                            updateMessage(mensaje);
                            dormir(RITMO_MS);
                        },
                        evolucionConRitmo,
                        telemetriaConRitmo);

                updateProgress(1, 1);
                LapResult pole = sesion.getPole();
                updateMessage(pole == null ? "Sesión sin resultados"
                        : "Pole: " + pole.getPiloto() + " — " + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
                return sesion;
            }

            private void dormir(long milisegundos) {
                try {
                    Thread.sleep(milisegundos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
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
            double variacionVelocidad = 1
                    + 0.08 * Math.sin(2 * Math.PI * progreso)
                    - 0.03 * Math.cos(4 * Math.PI * progreso);
            double velocidad = MathUtils.clamp(
                    velocidadMedia * variacionVelocidad
                            * factorTiempoMedio / climaActual.factorTiempo()
                            * impacto.multiplicadorVelocidad(),
                    0, vehiculo.getVelocidadMaximaKmh());
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
    private Circuit validarSeleccion(SimulationConfig config) {
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

    /** Calcula el resumen que consume la vista estadística de HU-21. */
    public SessionStatistics calcularEstadisticas(QualifyingSession sesion) {
        if (sesion == null) {
            throw new ValidationException("La sesión no puede ser nula");
        }
        List<LapResult> resultados = sesion.getResultados() == null
                ? List.of()
                : sesion.getResultados().stream().filter(LapResult::isVueltaValida).toList();
        if (resultados.isEmpty()) {
            return SessionStatistics.vacias();
        }

        DoubleSummaryStatistics tiempos = resultados.stream()
                .mapToDouble(LapResult::getTiempoSegundos)
                .summaryStatistics();
        double diferenciaMaxima = resultados.stream()
                .mapToDouble(LapResult::getGap)
                .max()
                .orElse(0);
        double consumoPromedio = resultados.stream()
                .mapToDouble(LapResult::getConsumoEstimado)
                .average()
                .orElse(0);
        double desgastePromedio = resultados.stream()
                .mapToDouble(LapResult::getDesgasteEstimado)
                .average()
                .orElse(0);

        return new SessionStatistics(
                resultados.size(),
                tiempos.getMin(),
                tiempos.getAverage(),
                diferenciaMaxima,
                consumoPromedio,
                desgastePromedio);
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
}
