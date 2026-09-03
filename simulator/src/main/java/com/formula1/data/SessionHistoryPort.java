package com.formula1.data;

import com.formula1.domain.model.QualifyingSession;

import java.util.List;

/**
 * Puerto de salida para el historial de sesiones de clasificación, usado por
 * el motor de simulación ({@code QualifyingService}) sin depender del
 * singleton concreto {@link DataStore}.
 */
public interface SessionHistoryPort {

    List<QualifyingSession> sesiones();

    void guardarSesion(QualifyingSession sesion);
}
