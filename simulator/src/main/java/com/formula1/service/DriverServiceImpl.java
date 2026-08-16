package com.formula1.service;

import com.formula1.model.Driver;
import com.formula1.repository.DriverRepository;
import com.formula1.repository.DriverRepositoryImpl;
import com.formula1.util.ValidationUtils;
import com.formula1.exception.InvalidDriverException;

import java.util.List;
import java.util.Optional;

public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    public DriverServiceImpl() {
        this(new DriverRepositoryImpl());
    }

    public DriverServiceImpl(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Override
    public Driver register(Driver driver) {
        if (driver == null || !ValidationUtils.isNotBlank(driver.getNombre())) {
            throw new InvalidDriverException("El nombre del piloto no puede estar vacío");
        }
        if (!ValidationUtils.isValidDriverNumber(driver.getNumero())) {
            throw new InvalidDriverException("Número de piloto inválido: " + driver.getNumero());
        }
        return driverRepository.save(driver);
    }

    @Override
    public Optional<Driver> findById(String id) {
        return driverRepository.findById(id);
    }

    @Override
    public List<Driver> findAll() {
        try {
            return driverRepository.findAll();
        } catch (RuntimeException e) {
            System.err.println("DriverService.findAll: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Driver update(Driver driver) {
        return driverRepository.update(driver);
    }

    @Override
    public void delete(String id) {
        driverRepository.deleteById(id);
    }
}
