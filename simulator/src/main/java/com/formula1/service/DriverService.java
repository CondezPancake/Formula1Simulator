package com.formula1.service;

import com.formula1.model.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverService {

    Driver register(Driver driver);

    Optional<Driver> findById(String id);

    List<Driver> findAll();

    Driver update(Driver driver);

    void delete(String id);
}
