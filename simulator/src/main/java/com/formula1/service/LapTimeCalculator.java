package com.formula1.service;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.SimulationConfig;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;

import java.util.Random;

/**
 * Calcula el tiempo de vuelta, el consumo y el desgaste a partir de los
 * datos que aporta la especificación.
 *
 * <p>Punto de partida: el tiempo que se tardaría en recorrer el circuito a
 * la velocidad media del modo de conducción elegido. Sobre él se aplican
 * factores multiplicativos, cada uno con su contrapartida, de modo que
 * ninguna configuración sea gratis:</p>
 *
 * <pre>
 * t_base   = 3600 · longitud_km / velocidad_promedio(modo)
 * t_vuelta = t_base · factorTecnico · f_clima · f_aero · f_presion
 *                   · f_combustible · f_piloto · (1 + ε)
 * </pre>
 *
 * <p>{@code factorTecnico} no es un número inventado: cada circuito lo
 * deriva de su récord de vuelta real (ver {@link Circuit#calcularFactorTecnico()}),
 * lo que sitúa a Mónaco en ~1,98 y a Monza en ~1,32.</p>
 *
 * <p>La clase no tiene estado mutable compartido, así que es segura de usar
 * desde cualquier hilo.</p>
 */
public class LapTimeCalculator {

    /** Variación aleatoria máxima, ±0,5 %. */
    private static final double VARIACION = 0.005;

    /** Peso de la habilidad y de la experiencia sobre el tiempo. */
    private static final double PESO_HABILIDAD = 0.040;
    private static final double PESO_EXPERIENCIA = 0.015;
    private static final int EXPERIENCIA_TOPE = 10;

    private final Random aleatorio;

    public LapTimeCalculator() {
        this(null);
    }

    /** Con una semilla fija el resultado es determinista (se usa en pruebas). */
    public LapTimeCalculator(Random aleatorio) {
        this.aleatorio = aleatorio;
    }

    public double calcularTiempo(Driver piloto, Vehicle vehiculo, Circuit circuito,
                                  WeatherCondition clima, SimulationConfig config) {
        Vehicle.Performance rendimiento = vehiculo.rendimientoDe(config.getModo());
        double velocidad = rendimiento.getVelocidadPromedioKmh();
        if (velocidad <= 0) {
            throw new ValidationException("El vehículo " + vehiculo.getModelo() + " no tiene velocidad definida");
        }

        double tiempoBase = 3600.0 * circuito.getLongitudKm() / velocidad;

        double tiempo = tiempoBase
                * circuito.getFactorTecnico()
                * clima.getFactorTiempo()
                * config.getAerodinamica().getFactorTiempo()
                * config.getPresion().getFactorTiempo()
                * config.getCombustible().getFactorTiempo()
                * factorPiloto(piloto, clima);

        return tiempo * (1 + variacion());
    }

    /**
     * Un piloto rápido y veterano rebaja el tiempo hasta un 5,5 %. En mojado
     * pesa su habilidad específica con lluvia.
     */
    double factorPiloto(Driver piloto, WeatherCondition clima) {
        double velocidad = piloto.getHabilidad(Driver.HABILIDAD_VELOCIDAD);
        double consistencia = piloto.getHabilidad(Driver.HABILIDAD_CONSISTENCIA);
        double lluvia = piloto.getHabilidad(Driver.HABILIDAD_LLUVIA);

        double habilidad = clima == WeatherCondition.SECO
                ? 0.70 * velocidad + 0.30 * consistencia
                : 0.50 * velocidad + 0.20 * consistencia + 0.30 * lluvia;

        double experiencia = Math.min(piloto.getExperiencia(), EXPERIENCIA_TOPE) / (double) EXPERIENCIA_TOPE;

        return 1 - PESO_HABILIDAD * (habilidad / 100.0) - PESO_EXPERIENCIA * experiencia;
    }

    /** Combustible gastado por vuelta, incluyendo el efecto de la pista. */
    public double consumoPorVuelta(Vehicle vehiculo, Circuit circuito, WeatherCondition clima, SimulationConfig config) {
        return vehiculo.rendimientoDe(config.getModo()).consumoCon(clima)
                * circuito.getFactorConsumo()
                * config.getAerodinamica().getFactorConsumo()
                * config.getCombustible().getFactorConsumo();
    }

    /** Desgaste de neumáticos por vuelta, incluyendo el efecto de la pista. */
    public double desgastePorVuelta(Vehicle vehiculo, Circuit circuito, WeatherCondition clima, SimulationConfig config) {
        return vehiculo.rendimientoDe(config.getModo()).desgasteCon(clima)
                * circuito.getFactorDesgaste()
                * config.getAerodinamica().getFactorDesgaste()
                * config.getPresion().getFactorDesgaste();
    }

    private double variacion() {
        if (aleatorio == null) {
            return com.formula1.util.RandomUtils.randomDouble(-VARIACION, VARIACION);
        }
        return (aleatorio.nextDouble() * 2 - 1) * VARIACION;
    }
}
