package com.formula1.simulation;

import com.formula1.exception.InvalidSimulationException;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Result;
import com.formula1.model.Simulation;
import com.formula1.model.SimulationConfig;
import com.formula1.model.Vehicle;
import com.formula1.model.Weather;
import com.formula1.repository.ResultRepository;
import com.formula1.repository.ResultRepositoryImpl;
import com.formula1.service.CircuitService;
import com.formula1.service.CircuitServiceImpl;
import com.formula1.service.DriverService;
import com.formula1.service.DriverServiceImpl;
import com.formula1.service.SimulationService;
import com.formula1.service.VehicleService;
import com.formula1.service.VehicleServiceImpl;
import com.formula1.service.WeatherService;
import com.formula1.service.WeatherServiceImpl;

import java.util.List;

/**
 * Punto único de entrada para iniciar y consultar una sesión de
 * clasificación (patrón Facade): coordina los servicios de dominio, el
 * clima, el {@link SimulationEngine} y la persistencia de resultados,
 * para que el controller de JavaFX no dependa de ninguno directamente.
 */
public class SimulationFacade implements SimulationService {

    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final CircuitService circuitService;
    private final WeatherService weatherService;
    private final SimulationEngine simulationEngine;
    private final ResultRepository resultRepository;

    public SimulationFacade() {
        this(new DriverServiceImpl(), new VehicleServiceImpl(), new CircuitServiceImpl(),
                new WeatherServiceImpl(), new SimulationEngine(), new ResultRepositoryImpl());
    }

    public SimulationFacade(DriverService driverService, VehicleService vehicleService, CircuitService circuitService,
                             WeatherService weatherService, SimulationEngine simulationEngine, ResultRepository resultRepository) {
        this.driverService = driverService;
        this.vehicleService = vehicleService;
        this.circuitService = circuitService;
        this.weatherService = weatherService;
        this.simulationEngine = simulationEngine;
        this.resultRepository = resultRepository;
    }

    @Override
    public Simulation startQualifying(SimulationConfig config) {
        if (config == null) {
            throw new InvalidSimulationException("La configuración de la simulación no puede ser nula");
        }

        Driver driver = driverService.findById(config.getDriverId())
                .orElseThrow(() -> new InvalidSimulationException("Piloto no encontrado: " + config.getDriverId()));
        Vehicle vehicle = vehicleService.findById(config.getVehicleId())
                .orElseThrow(() -> new InvalidSimulationException("Vehículo no encontrado: " + config.getVehicleId()));
        Circuit circuit = circuitService.findById(config.getCircuitId())
                .orElseThrow(() -> new InvalidSimulationException("Circuito no encontrado: " + config.getCircuitId()));
        Weather weather = weatherService.generateWeather();

        return simulationEngine.runQualifying(config, weather, driver, vehicle, circuit);
    }

    @Override
    public List<Result> getResults(String simulationId) {
        try {
            return resultRepository.findAll();
        } catch (RuntimeException e) {
            System.err.println("SimulationFacade.getResults: " + e.getMessage());
            return List.of();
        }
    }
}
