package com.formula1.data;

import com.formula1.domain.model.QualifyingSession;

import java.util.List;

/** Puerto de salida JDBC para el historial de sesiones de clasificación. */
public interface SessionPersistencePort {

    List<QualifyingSession> loadSessions();

    void saveSession(QualifyingSession session);

    void deleteSession(String id);
}
