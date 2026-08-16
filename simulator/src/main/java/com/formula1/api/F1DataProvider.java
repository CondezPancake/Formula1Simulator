package com.formula1.api;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Team;

import java.util.List;

/**
 * Puerto (target) del patrón Adapter: aísla al resto del sistema del
 * formato concreto de la API externa de datos de F1 (OpenF1).
 */
public interface F1DataProvider {

    List<Driver> fetchDrivers();

    List<Team> fetchTeams();

    List<Circuit> fetchCircuits();
}
