package com.formula1.service;

import com.formula1.model.Circuit;

import java.util.List;
import java.util.Optional;

public interface CircuitService {

    Circuit register(Circuit circuit);

    Optional<Circuit> findById(String id);

    List<Circuit> findAll();

    Circuit update(Circuit circuit);

    void delete(String id);
}
