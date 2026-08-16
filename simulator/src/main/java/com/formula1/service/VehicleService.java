package com.formula1.service;

import com.formula1.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleService {

    Vehicle register(Vehicle vehicle);

    Optional<Vehicle> findById(String id);

    List<Vehicle> findAll();

    Vehicle update(Vehicle vehicle);

    void delete(String id);
}
