package com.formula1.data;

import com.formula1.model.QualifyingSession;

import java.util.List;

/** Adaptador JDBC que implementa únicamente el puerto de historial de sesiones. */
final class MySqlSessionPersistenceAdapter implements SessionPersistencePort {

    private final MySqlSessionRepository sessions = new MySqlSessionRepository();

    @Override
    public List<QualifyingSession> loadSessions() {
        return JdbcTransactionSupport.execute(sessions::loadAll);
    }

    @Override
    public void saveSession(QualifyingSession session) {
        JdbcTransactionSupport.execute(connection -> {
            sessions.save(connection, session);
            return null;
        });
    }

    @Override
    public void deleteSession(String id) {
        JdbcTransactionSupport.transaction(connection -> sessions.delete(connection, id));
    }
}
