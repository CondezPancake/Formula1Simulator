package com.formula1.adapter.out.mysql;

import com.formula1.application.port.out.CatalogPersistencePort;
import com.formula1.application.port.out.PersistencePort;
import com.formula1.application.port.out.SessionPersistencePort;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.QualifyingSession;
import com.formula1.domain.model.Team;
import com.formula1.domain.model.Vehicle;

import java.util.List;

/**
 * Adaptador JDBC que implementa el puerto de persistencia con MySQL.
 *
 * Fase 3 de la migración a hexagonal: deja de contener la lógica JDBC
 * directamente y pasa a componer {@link MySqlCatalogPersistenceAdapter} y
 * {@link MySqlSessionPersistenceAdapter} —uno por puerto de la Fase 2—, cada
 * uno sustituible o probable por separado. Esta clase sigue siendo la única
 * que {@link DataStore} conoce, así que el comportamiento no cambia.
 */
public final class MySqlPersistenceAdapter implements PersistencePort {

    private final CatalogPersistencePort catalogAdapter = new MySqlCatalogPersistenceAdapter();
    private final SessionPersistencePort sessionAdapter = new MySqlSessionPersistenceAdapter();

    @Override
    public CatalogData loadCatalogs() {
        return catalogAdapter.loadCatalogs();
    }

    @Override
    public void saveDriver(Driver driver) {
        catalogAdapter.saveDriver(driver);
    }

    @Override
    public void deleteDriver(int id) {
        catalogAdapter.deleteDriver(id);
    }

    @Override
    public void saveTeam(Team team) {
        catalogAdapter.saveTeam(team);
    }

    @Override
    public void deleteTeam(String name) {
        catalogAdapter.deleteTeam(name);
    }

    @Override
    public void saveVehicle(Vehicle vehicle) {
        catalogAdapter.saveVehicle(vehicle);
    }

    @Override
    public void deleteVehicle(String model) {
        catalogAdapter.deleteVehicle(model);
    }

    @Override
    public void saveCircuit(Circuit circuit) {
        catalogAdapter.saveCircuit(circuit);
    }

    @Override
    public void deleteCircuit(String name) {
        catalogAdapter.deleteCircuit(name);
    }

    @Override
    public List<QualifyingSession> loadSessions() {
        return sessionAdapter.loadSessions();
    }

    @Override
    public void saveSession(QualifyingSession session) {
        sessionAdapter.saveSession(session);
    }

    @Override
    public void deleteSession(String id) {
        sessionAdapter.deleteSession(id);
    }
}
