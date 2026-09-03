package com.formula1.data;

import com.formula1.domain.model.SimulationConfig;

/**
 * Puerto de salida para la puesta a punto que el usuario deja preparada en
 * la pantalla de configuración antes de lanzar una sesión. Vive solo en
 * memoria (no hay tabla ni JDBC detrás): al reiniciar, la pantalla de
 * clasificación recupera la del último historial guardado.
 *
 * Fase 5 de la migración a hexagonal: {@code ConfigController},
 * {@code HistoryController} y {@code SimulationController} dependían de
 * {@code DataStore.getInstance()} directamente para este dato; ahora
 * dependen de este puerto, que {@link DataStore} sigue implementando sin
 * cambiar su comportamiento.
 */
public interface PreparedConfigPort {

    SimulationConfig configuracionActual();

    void guardarConfiguracion(SimulationConfig config);

    long versionConfiguracion();
}
