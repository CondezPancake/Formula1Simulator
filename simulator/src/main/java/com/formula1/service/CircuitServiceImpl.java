package com.formula1.service;

import com.formula1.model.Circuit;
import com.formula1.repository.CircuitRepository;
import com.formula1.repository.CircuitRepositoryImpl;
import com.formula1.util.ValidationUtils;
import com.formula1.exception.InvalidSimulationException;

import java.util.List;
import java.util.Optional;

public class CircuitServiceImpl implements CircuitService {

    private final CircuitRepository circuitRepository;

    public CircuitServiceImpl() {
        this(new CircuitRepositoryImpl());
    }

    public CircuitServiceImpl(CircuitRepository circuitRepository) {
        this.circuitRepository = circuitRepository;
    }

    @Override
    public Circuit register(Circuit circuit) {
        if (circuit == null || !ValidationUtils.isPositive(circuit.getLongitudKm()) || circuit.getVueltas() <= 0) {
            throw new InvalidSimulationException("El circuito debe tener longitud y número de vueltas mayores que 0");
        }
        return circuitRepository.save(circuit);
    }

    @Override
    public Optional<Circuit> findById(String id) {
        return circuitRepository.findById(id);
    }

    @Override
    public List<Circuit> findAll() {
        try {
            return circuitRepository.findAll();
        } catch (RuntimeException e) {
            System.err.println("CircuitService.findAll: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Circuit update(Circuit circuit) {
        return circuitRepository.update(circuit);
    }

    @Override
    public void delete(String id) {
        circuitRepository.deleteById(id);
    }
}
