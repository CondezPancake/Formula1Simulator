package com.formula1.service;

import com.formula1.model.Result;
import com.formula1.model.Simulation;
import com.formula1.model.SimulationConfig;

import java.util.List;

/**
 * Contrato de alto nivel para orquestar una sesión de clasificación.
 * Lo implementa {@code simulation.SimulationFacade} (patrón Facade).
 */
public interface SimulationService {

    Simulation startQualifying(SimulationConfig config);

    List<Result> getResults(String simulationId);
}
