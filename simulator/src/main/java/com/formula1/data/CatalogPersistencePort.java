package com.formula1.data;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;

import java.util.List;

/** Puerto de salida JDBC para el catálogo (pilotos, equipos, vehículos, circuitos). */
public interface CatalogPersistencePort {

    CatalogData loadCatalogs();

    void saveDriver(Driver driver);

    void deleteDriver(int id);

    void saveTeam(Team team);

    void deleteTeam(String name);

    void saveVehicle(Vehicle vehicle);

    void deleteVehicle(String model);

    void saveCircuit(Circuit circuit);

    void deleteCircuit(String name);

    record CatalogData(List<Driver> drivers, List<Team> teams,
                        List<Vehicle> vehicles, List<Circuit> circuits) {
    }
}
