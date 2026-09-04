package com.formula1.domain.event;

import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.FuelStrategy;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.TrackSector;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherSnapshot;
import com.formula1.util.MathUtils;

import java.util.List;
import java.util.Objects;

/** Estado de solo lectura usado para decidir qué eventos son válidos. */
public record EventContext(
        int secuencia,
        int vuelta,
        Driver piloto,
        Vehicle vehiculo,
        SimulationConfig configuracion,
        List<WeatherSnapshot> clima,
        double temperaturaNeumaticosC,
        double temperaturaMotorC,
        double desgasteNeumaticos,
        int nivelTrafico,
        int erroresPrevios,
        TrackSector sector) {

    public EventContext {
        if (secuencia < 1 || vuelta < 1) {
            throw new IllegalArgumentException("La secuencia y la vuelta deben ser positivas");
        }
        Objects.requireNonNull(piloto, "El piloto es obligatorio");
        Objects.requireNonNull(vehiculo, "El vehículo es obligatorio");
        Objects.requireNonNull(configuracion, "La configuración es obligatoria");
        if (clima == null || clima.isEmpty()) {
            throw new IllegalArgumentException("La evolución climática es obligatoria");
        }
        clima = List.copyOf(clima);
        if (!Double.isFinite(temperaturaNeumaticosC)
                || !Double.isFinite(temperaturaMotorC)
                || !Double.isFinite(desgasteNeumaticos)) {
            throw new IllegalArgumentException("Las métricas del contexto deben ser finitas");
        }
        if (nivelTrafico < 0 || erroresPrevios < 0) {
            throw new IllegalArgumentException("Tráfico y errores no pueden ser negativos");
        }
        if (sector == null) {
            sector = TrackSector.NONE;
        }
    }

    public EventContext conSector(TrackSector nuevoSector) {
        return new EventContext(secuencia, vuelta, piloto, vehiculo, configuracion,
                clima, temperaturaNeumaticosC, temperaturaMotorC,
                desgasteNeumaticos, nivelTrafico, erroresPrevios, nuevoSector);
    }

    public EventContext conErroresPrevios(int nuevosErrores) {
        return new EventContext(secuencia, vuelta, piloto, vehiculo, configuracion,
                clima, temperaturaNeumaticosC, temperaturaMotorC,
                desgasteNeumaticos, nivelTrafico, nuevosErrores, sector);
    }

    public WeatherSnapshot climaDelSector() {
        if (sector == TrackSector.NONE) {
            return clima.get(clima.size() / 2);
        }
        int indiceSector = sector.ordinal() - 1;
        int inicio = indiceSector * clima.size() / 3;
        int finExclusivo = (indiceSector + 1) * clima.size() / 3;
        return clima.get((inicio + Math.max(inicio + 1, finExclusivo) - 1) / 2);
    }

    public double habilidadNormalizada() {
        double velocidad = piloto.getHabilidad(Driver.HABILIDAD_VELOCIDAD);
        double consistencia = piloto.getHabilidad(Driver.HABILIDAD_CONSISTENCIA);
        double lluvia = piloto.getHabilidad(Driver.HABILIDAD_LLUVIA);
        return MathUtils.clamp((0.35 * velocidad + 0.45 * consistencia + 0.20 * lluvia) / 100, 0, 1);
    }

    /** Un piloto consistente reduce errores; lluvia y estrategia agresiva los elevan. */
    public double multiplicadorRiesgoError() {
        double habilidad = 1.35 - 0.70 * habilidadNormalizada();
        double clima = 1 + (100 - climaDelSector().gripPorcentaje()) / 120;
        return MathUtils.clamp(habilidad * clima * multiplicadorEstrategia(), 0.45, 2.5);
    }

    public double multiplicadorEstrategia() {
        double conduccion = configuracion.getModo() == DrivingMode.AGRESIVA ? 1.35
                : configuracion.getModo() == DrivingMode.AHORRO ? 0.72 : 1.0;
        double combustible = configuracion.getCombustible() == FuelStrategy.AGRESIVA ? 1.15
                : configuracion.getCombustible() == FuelStrategy.AHORRO ? 0.88 : 1.0;
        return conduccion * combustible;
    }

    public double multiplicadorRiesgoVehiculo() {
        double velocidad = vehiculo.getVelocidadMaximaKmh() <= 0
                ? 1.15
                : 340.0 / vehiculo.getVelocidadMaximaKmh();
        double termico = temperaturaMotorC > 108 ? 1.35 : temperaturaMotorC > 102 ? 1.15 : 1;
        return MathUtils.clamp(velocidad * termico * Math.sqrt(multiplicadorEstrategia()), 0.75, 2.0);
    }

    public double multiplicadorRiesgoClimatico() {
        WeatherSnapshot muestra = climaDelSector();
        return MathUtils.clamp(0.75
                + muestra.intensidadLluviaPorcentaje() / 80
                + (100 - muestra.gripPorcentaje()) / 100, 0.6, 2.5);
    }

    public double multiplicadorRiesgoAccidente() {
        double desgaste = 1 + Math.max(0, desgasteNeumaticos - 60) / 60;
        double errores = 1 + Math.min(erroresPrevios, 4) * 0.20;
        return MathUtils.clamp(multiplicadorRiesgoError()
                * multiplicadorRiesgoVehiculo() * desgaste * errores, 0.35, 4.0);
    }
}
