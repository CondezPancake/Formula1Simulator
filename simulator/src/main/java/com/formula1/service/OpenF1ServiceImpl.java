package com.formula1.service;

import com.formula1.api.F1DataProvider;
import com.formula1.api.OpenF1ApiAdapter;
import com.formula1.exception.OpenF1ConnectionException;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Team;

import java.util.List;

/**
 * Envuelve al {@link F1DataProvider} (Adapter) y traduce cualquier fallo de
 * origen en una {@link OpenF1ConnectionException} para que la capa de UI
 * pueda informar al usuario (RF-21).
 */
public class OpenF1ServiceImpl implements OpenF1Service {

    private final F1DataProvider f1DataProvider;

    public OpenF1ServiceImpl() {
        this(new OpenF1ApiAdapter());
    }

    public OpenF1ServiceImpl(F1DataProvider f1DataProvider) {
        this.f1DataProvider = f1DataProvider;
    }

    @Override
    public List<Driver> syncDrivers() {
        try {
            return f1DataProvider.fetchDrivers();
        } catch (RuntimeException e) {
            throw new OpenF1ConnectionException("No se pudieron obtener los pilotos desde OpenF1", e);
        }
    }

    @Override
    public List<Team> syncTeams() {
        try {
            return f1DataProvider.fetchTeams();
        } catch (RuntimeException e) {
            throw new OpenF1ConnectionException("No se pudieron obtener los equipos desde OpenF1", e);
        }
    }

    @Override
    public List<Circuit> syncCircuits() {
        try {
            return f1DataProvider.fetchCircuits();
        } catch (RuntimeException e) {
            throw new OpenF1ConnectionException("No se pudieron obtener los circuitos desde OpenF1", e);
        }
    }
}
