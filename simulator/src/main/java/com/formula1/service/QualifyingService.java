package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.LapResult;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.util.DateUtils;
import com.formula1.util.FormatUtils;
import com.formula1.util.RandomUtils;

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
     * Simula la sesión completa. El vehículo elegido por el usuario corre con
     * su configuración; el resto de la parrilla usa la configuración neutra.
     *
     * @param progreso callback opcional (piloto procesado, total, mensaje).
     */
    public QualifyingSession simular(SimulationConfig config, WeatherCondition clima, Progreso progreso) {
        Circuit circuito = circuitos.porNombre(config.getCircuito())
                .orElseThrow(() -> new ValidationException("El circuito no existe: " + config.getCircuito()));

        List<Driver> parrilla = pilotos.listar();
        List<LapResult> resultados = new ArrayList<>();

        for (int i = 0; i < parrilla.size(); i++) {
            Driver piloto = parrilla.get(i);
            Optional<Vehicle> vehiculo = vehiculos.delPiloto(piloto.getId());
            if (vehiculo.isEmpty()) {
                continue;
            }
            Vehicle coche = vehiculo.get();

            // Solo el coche seleccionado hereda los ajustes del usuario; el
            // resto de la parrilla aprieta, como en una clasificación real.
            SimulationConfig configPiloto = coche.getModelo().equals(config.getVehiculo())
                    ? config
                    : SimulationConfig.paraClasificacion();

            double tiempo = calculadora.calcularTiempo(piloto, coche, circuito, clima, configPiloto);

            LapResult resultado = new LapResult(piloto.getId(), piloto.getNombre(),
                    piloto.getEquipo(), coche.getModelo(), tiempo);
            resultado.setConsumoEstimado(calculadora.consumoPorVuelta(coche, circuito, clima, configPiloto));
            resultado.setDesgasteEstimado(calculadora.desgastePorVuelta(coche, circuito, clima, configPiloto));
            resultados.add(resultado);

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
        return new Task<>() {
            @Override
            protected QualifyingSession call() throws Exception {
                Circuit circuito = circuitos.porNombre(config.getCircuito())
                        .orElseThrow(() -> new ValidationException("El circuito no existe: " + config.getCircuito()));

                WeatherCondition clima = generarClima(circuito);
                updateMessage("Clima de la sesión: " + clima.getEtiqueta());
                Thread.sleep(RITMO_MS * 2);

                QualifyingSession sesion = simular(config, clima, (hecho, total, mensaje) -> {
                    updateProgress(hecho, total);
                    updateMessage(mensaje);
                    dormir();
                });

                updateProgress(1, 1);
                LapResult pole = sesion.getPole();
                updateMessage(pole == null ? "Sesión sin resultados"
                        : "Pole: " + pole.getPiloto() + " — " + FormatUtils.formatLapTime(pole.getTiempoSegundos()));
                return sesion;
            }

            private void dormir() {
                try {
                    Thread.sleep(RITMO_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
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
}
