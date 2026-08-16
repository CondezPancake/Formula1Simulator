package com.formula1.service;

import com.formula1.model.Vehicle;
import com.formula1.repository.VehicleRepository;
import com.formula1.repository.VehicleRepositoryImpl;
import com.formula1.util.ValidationUtils;
import com.formula1.exception.InvalidVehicleConfigurationException;

import java.util.List;
import java.util.Optional;

public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleServiceImpl() {
        this(new VehicleRepositoryImpl());
    }

    public VehicleServiceImpl(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public Vehicle register(Vehicle vehicle) {
        if (vehicle == null || !ValidationUtils.isPositive(vehicle.getVelocidadMaxima())) {
            throw new InvalidVehicleConfigurationException("La velocidad máxima del vehículo debe ser mayor que 0");
        }
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Optional<Vehicle> findById(String id) {
        return vehicleRepository.findById(id);
    }

    @Override
    public List<Vehicle> findAll() {
        try {
            return vehicleRepository.findAll();
        } catch (RuntimeException e) {
            System.err.println("VehicleService.findAll: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Vehicle update(Vehicle vehicle) {
        return vehicleRepository.update(vehicle);
    }

    @Override
    public void delete(String id) {
        vehicleRepository.deleteById(id);
    }
}
