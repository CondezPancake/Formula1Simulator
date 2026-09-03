package com.formula1.data;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.QualifyingSession;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;

import java.util.List;

/** Puerto que mantiene JDBC fuera del almacén de aplicación y del dominio. */
public interface PersistencePort {

    CatalogData loadCatalogs();

    List<QualifyingSession> loadSessions();

    void saveDriver(Driver driver);

    void deleteDriver(int id);

    void saveTeam(Team team);

    void deleteTeam(String name);

    void saveVehicle(Vehicle vehicle);

    void deleteVehicle(String model);

    void saveCircuit(Circuit circuit);

    void deleteCircuit(String name);

    void saveSession(QualifyingSession session);

    void deleteSession(String id);

    record CatalogData(List<Driver> drivers, List<Team> teams,
                       List<Vehicle> vehicles, List<Circuit> circuits) {
    }
}
