package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.SimulationSnapshot;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.util.DateUtils;
import com.formula1.util.FormatUtils;
import com.formula1.util.MathUtils;
import com.formula1.util.RandomUtils;
import com.formula1.util.ValidationUtils;

import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.Comparator;
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

    public QualifyingService() {
        this(DataStore.getInstance(), new LapTimeCalculator());
    }

    public QualifyingService(DataStore datos, LapTimeCalculator calculadora) {
        this.datos = datos;
        this.calculadora = calculadora;
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
        Circuit circuito = validarSeleccion(config);
        if (clima == null) {
            throw new ValidationException("Las condiciones climáticas no pueden ser nulas");
        }

        List<Driver> parrilla = participantesConSeleccionPrimero(config.getPilotoId());
        List<LapResult> resultados = new ArrayList<>();

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

            double tiempo = calculadora.calcularTiempo(piloto, coche, circuito, clima, configPiloto);

            LapResult resultado = new LapResult(piloto.getId(), piloto.getNombre(),
                    piloto.getEquipo(), coche.getModelo(), tiempo);
            resultado.setConsumoEstimado(calculadora.consumoPorVuelta(coche, circuito, clima, configPiloto));
            resultado.setDesgasteEstimado(calculadora.desgastePorVuelta(coche, circuito, clima, configPiloto));
            resultados.add(resultado);

            if (piloto.getId() == config.getPilotoId() && evolucion != null) {
                emitirEvolucion(piloto, coche, circuito, resultado, evolucion);
            }

            if (progreso != null) {
                progreso.avanzar(i + 1, parrilla.size(),
                        piloto.getNombre() + " — " + FormatUtils.formatLapTime(tiempo));
            }
        }

        ordenarParrilla(resultados);

        QualifyingSession sesion = new QualifyingSession(circuito.getNombre(), clima, config);
        sesion.setResultados(resultados);
        sesion.setFecha(DateUtils.format(DateUtils.now()));
        config.setGuardadoEn(sesion.getFecha());
        return sesion;
    }

    /** Ordena por tiempo, asigna posiciones y calcula la diferencia con la pole. */
    void ordenarParrilla(List<LapResult> resultados) {
        resultados.sort(Comparator.comparingDouble(LapResult::getTiempoSegundos));
        if (resultados.isEmpty()) {
            return;
        }
        double pole = resultados.get(0).getTiempoSegundos();
        for (int i = 0; i < resultados.size(); i++) {
            LapResult resultado = resultados.get(i);
            resultado.setPosicion(i + 1);
            resultado.setGap(resultado.getTiempoSegundos() - pole);
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
        return new Task<>() {
            @Override
            protected QualifyingSession call() throws Exception {
                Circuit circuito = validarSeleccion(config);

                WeatherCondition clima = generarClima(circuito);
                updateMessage("Clima de la sesión: " + clima.getEtiqueta());
                Thread.sleep(RITMO_MS * 2);

                Evolucion evolucionConRitmo = evolucion == null ? null : muestra -> {
                    evolucion.actualizar(muestra);
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
                        evolucionConRitmo);

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
     * Divide la vuelta del piloto seleccionado en muestras. El consumo y el
     * desgaste convergen exactamente a los valores guardados en el resultado.
     */
    private void emitirEvolucion(Driver piloto, Vehicle vehiculo, Circuit circuito,
                                 LapResult resultado, Evolucion evolucion) {
        double velocidadMedia = 3600 * circuito.getLongitudKm() / resultado.getTiempoSegundos();

        for (int segmento = 1; segmento <= SEGMENTOS_EVOLUCION; segmento++) {
            double progreso = segmento / (double) SEGMENTOS_EVOLUCION;
            double variacionVelocidad = 1
                    + 0.08 * Math.sin(2 * Math.PI * progreso)
                    - 0.03 * Math.cos(4 * Math.PI * progreso);
            double velocidad = MathUtils.clamp(
                    velocidadMedia * variacionVelocidad, 0, vehiculo.getVelocidadMaximaKmh());

            evolucion.actualizar(new SimulationSnapshot(
                    piloto.getNombre(),
                    vehiculo.getModelo(),
                    segmento,
                    SEGMENTOS_EVOLUCION,
                    velocidad,
                    vehiculo.getVelocidadMaximaKmh(),
                    resultado.getConsumoEstimado() * progreso,
                    resultado.getConsumoEstimado(),
                    resultado.getDesgasteEstimado() * progreso,
                    resultado.getDesgasteEstimado()));
        }
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
}
