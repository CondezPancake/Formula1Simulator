package com.formula1.data;

import com.formula1.application.port.out.CatalogPersistencePort;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.Team;
import com.formula1.domain.model.Vehicle;

/** Adaptador JDBC que implementa únicamente el puerto de catálogo. */
final class MySqlCatalogPersistenceAdapter implements CatalogPersistencePort {

    private final MySqlCatalogRepository catalogs = new MySqlCatalogRepository();

    @Override
    public CatalogData loadCatalogs() {
        return JdbcTransactionSupport.execute(catalogs::load);
    }

    @Override
    public void saveDriver(Driver driver) {
        JdbcTransactionSupport.transaction(connection -> catalogs.saveDriver(connection, driver));
    }

    @Override
    public void deleteDriver(int id) {
        JdbcTransactionSupport.transaction(connection -> {
            try (var ps = connection.prepareStatement("DELETE FROM piloto WHERE piloto_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void saveTeam(Team team) {
        JdbcTransactionSupport.transaction(connection -> catalogs.saveTeam(connection, team));
    }

    @Override
    public void deleteTeam(String name) {
        JdbcTransactionSupport.transaction(connection -> catalogs.deleteByNaturalKey(connection, "equipo", "nombre", name));
    }

    @Override
    public void saveVehicle(Vehicle vehicle) {
        JdbcTransactionSupport.transaction(connection -> catalogs.saveVehicle(connection, vehicle));
    }

    @Override
    public void deleteVehicle(String model) {
        JdbcTransactionSupport.transaction(connection -> catalogs.deleteByNaturalKey(connection, "vehiculo", "modelo", model));
    }

    @Override
    public void saveCircuit(Circuit circuit) {
        JdbcTransactionSupport.transaction(connection -> catalogs.saveCircuit(connection, circuit));
    }

    @Override
    public void deleteCircuit(String name) {
        JdbcTransactionSupport.transaction(connection -> catalogs.deleteByNaturalKey(connection, "circuito", "nombre", name));
    }
}
