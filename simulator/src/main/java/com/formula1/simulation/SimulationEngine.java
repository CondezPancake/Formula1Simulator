package com.formula1.simulation;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Simulation;
import com.formula1.model.SimulationConfig;
import com.formula1.model.Vehicle;
import com.formula1.model.Weather;

/**
 * Orquesta la ejecución de una sesión de clasificación (HU-17/HU-18):
 * calcula tiempos de vuelta con {@link LapCalculator} y rendimiento con
 * {@link PerformanceCalculator}, y produce la clasificación ordenada.
 */
public class SimulationEngine {

    private final LapCalculator lapCalculator;
    private final PerformanceCalculator performanceCalculator;

    public SimulationEngine() {
        this(new LapCalculator(), new PerformanceCalculator());
    }

    public SimulationEngine(LapCalculator lapCalculator, PerformanceCalculator performanceCalculator) {
        this.lapCalculator = lapCalculator;
        this.performanceCalculator = performanceCalculator;
    }

    public Simulation runQualifying(SimulationConfig config, Weather weather, Driver driver, Vehicle vehicle, Circuit circuit) {
        throw new UnsupportedOperationException(
                "TODO: orquestar Q1/Q2/Q3 usando LapCalculator y PerformanceCalculator, y generar la clasificación ordenada");
    }
}
