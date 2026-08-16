package com.formula1.model;

public class SimulationConfig {

    private String circuitId;
    private String driverId;
    private String vehicleId;
    private DrivingMode drivingMode;
    private AerodynamicLoad aerodynamicLoad;
    private double presionNeumaticos;
    private FuelStrategy fuelStrategy;

    public SimulationConfig() {
    }

    public SimulationConfig(String circuitId, String driverId, String vehicleId, DrivingMode drivingMode,
                             AerodynamicLoad aerodynamicLoad, double presionNeumaticos, FuelStrategy fuelStrategy) {
        this.circuitId = circuitId;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.drivingMode = drivingMode;
        this.aerodynamicLoad = aerodynamicLoad;
        this.presionNeumaticos = presionNeumaticos;
        this.fuelStrategy = fuelStrategy;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public DrivingMode getDrivingMode() {
        return drivingMode;
    }

    public void setDrivingMode(DrivingMode drivingMode) {
        this.drivingMode = drivingMode;
    }

    public AerodynamicLoad getAerodynamicLoad() {
        return aerodynamicLoad;
    }

    public void setAerodynamicLoad(AerodynamicLoad aerodynamicLoad) {
        this.aerodynamicLoad = aerodynamicLoad;
    }

    public double getPresionNeumaticos() {
        return presionNeumaticos;
    }

    public void setPresionNeumaticos(double presionNeumaticos) {
        this.presionNeumaticos = presionNeumaticos;
    }

    public FuelStrategy getFuelStrategy() {
        return fuelStrategy;
    }

    public void setFuelStrategy(FuelStrategy fuelStrategy) {
        this.fuelStrategy = fuelStrategy;
    }

    @Override
    public String toString() {
        return "SimulationConfig{" +
                "circuitId='" + circuitId + '\'' +
                ", driverId='" + driverId + '\'' +
                ", vehicleId='" + vehicleId + '\'' +
                ", drivingMode=" + drivingMode +
                ", aerodynamicLoad=" + aerodynamicLoad +
                ", fuelStrategy=" + fuelStrategy +
                '}';
    }
}
