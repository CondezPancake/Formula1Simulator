package com.formula1.adapter.out.mysql;

import com.formula1.adapter.out.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ejecuta operaciones JDBC contra una conexión de corta vida, con
 * transacción manual opcional (commit al terminar, rollback y relanzamiento
 * ante cualquier fallo, restaurando siempre el autocommit previo).
 *
 * Fase 3 de la migración a hexagonal: antes vivía duplicado dentro de
 * {@code MySqlPersistenceAdapter}; se extrae aquí para que el adaptador de
 * catálogo y el de sesiones (uno por puerto JDBC de la Fase 2) lo compartan
 * sin repetir el manejo de commit/rollback.
 */
final class JdbcTransactionSupport {

    private JdbcTransactionSupport() {
    }

    static <T> T execute(SqlFunction<T> operation) {
        try (Connection connection = DatabaseConnection.open()) {
            return operation.apply(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Falló la operación SQL", e);
        }
    }

    static void transaction(SqlConsumer operation) {
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
    interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
