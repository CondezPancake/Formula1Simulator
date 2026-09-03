package com.formula1.data;

/**
 * Puerto compuesto para {@code QualifyingService}: el motor de clasificación
 * necesita tanto leer el catálogo (para construir sus servicios internos de
 * pilotos, vehículos y circuitos) como guardar el resultado en el historial
 * de sesiones. Se declara explícito en vez de dejar que el motor dependa de
 * {@link DataStore} completo, que además expone ciclo de vida (carga inicial,
 * modo memoria) que no es asunto de un caso de uso de dominio.
 */
public interface QualifyingDataPort extends CatalogPort, SessionHistoryPort {
}
