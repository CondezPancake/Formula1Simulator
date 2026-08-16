package com.formula1.simulation;

import com.formula1.model.AerodynamicLoad;
import com.formula1.model.FuelStrategy;
import com.formula1.model.TrackStatus;
import com.formula1.model.Vehicle;

/**
 * Calcula el rendimiento de un vehículo (HU-15): velocidad, desgaste de
 * neumáticos y consumo de combustible según la configuración elegida.
 */
public class PerformanceCalculator {

    public double calculateTopSpeed(Vehicle vehicle, AerodynamicLoad aerodynamicLoad) {
        throw new UnsupportedOperationException("TODO: implementar cálculo de velocidad máxima efectiva");
    }

    public double calculateTireWear(Vehicle vehicle, double presionNeumaticos, TrackStatus estadoPista) {
        throw new UnsupportedOperationException("TODO: implementar cálculo de desgaste de neumáticos");
    }

    public double calculateFuelConsumption(Vehicle vehicle, FuelStrategy fuelStrategy) {
        throw new UnsupportedOperationException("TODO: implementar cálculo de consumo de combustible");
    }
}
