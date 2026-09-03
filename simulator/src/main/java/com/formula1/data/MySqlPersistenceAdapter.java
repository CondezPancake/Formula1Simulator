package com.formula1.data;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.QualifyingSession;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/** Adaptador JDBC que implementa el puerto de persistencia con MySQL. */
public final class MySqlPersistenceAdapter implements PersistencePort {

    private final MySqlCatalogRepository catalogs = new MySqlCatalogRepository();
    private final MySqlSessionRepository sessions = new MySqlSessionRepository();

    @Override
    public CatalogData loadCatalogs() {
        return execute(catalogs::load);
    }

    @Override
    public List<QualifyingSession> loadSessions() {
        return execute(sessions::loadAll);
    }

    @Override
    public void saveDriver(Driver driver) {
        transaction(connection -> catalogs.saveDriver(connection, driver));
    }

    @Override
    public void deleteDriver(int id) {
        transaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM piloto WHERE piloto_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void saveTeam(Team team) {
        transaction(connection -> catalogs.saveTeam(connection, team));
    }

    @Override
    public void deleteTeam(String name) {
        transaction(connection -> catalogs.deleteByNaturalKey(connection, "equipo", "nombre", name));
    }

    @Override
    public void saveVehicle(Vehicle vehicle) {
        transaction(connection -> catalogs.saveVehicle(connection, vehicle));
    }

    @Override
    public void deleteVehicle(String model) {
        transaction(connection -> catalogs.deleteByNaturalKey(connection, "vehiculo", "modelo", model));
    }

    @Override
    public void saveCircuit(Circuit circuit) {
        transaction(connection -> catalogs.saveCircuit(connection, circuit));
    }

    @Override
    public void deleteCircuit(String name) {
        transaction(connection -> catalogs.deleteByNaturalKey(connection, "circuito", "nombre", name));
    }

    @Override
    public void saveSession(QualifyingSession session) {
        execute(connection -> {
            sessions.save(connection, session);
            return null;
        });
    }

    @Override
    public void deleteSession(String id) {
        transaction(connection -> sessions.delete(connection, id));
    }

    private <T> T execute(SqlFunction<T> operation) {
        try (Connection connection = DatabaseConnection.open()) {
            return operation.apply(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Falló la operación SQL", e);
        }
    }

    private void transaction(SqlConsumer operation) {
        execute(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                operation.accept(connection);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
            return null;
        });
    }

    @FunctionalInterface
    private interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
