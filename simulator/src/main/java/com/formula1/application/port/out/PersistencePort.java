package com.formula1.application.port.out;

import com.formula1.adapter.out.memory.DataStore;
import com.formula1.adapter.out.mysql.MySqlPersistenceAdapter;

/**
 * Puerto que mantiene JDBC fuera del almacén de aplicación y del dominio.
 *
 * Fase 2 de la migración a hexagonal: el contrato se dividió por capacidad en
 * {@link CatalogPersistencePort} y {@link SessionPersistencePort}. Esta
 * interfaz queda como fachada de compatibilidad —la sigue implementando
 * {@link MySqlPersistenceAdapter} y la sigue consumiendo {@link DataStore}
 * como un único adaptador—. Un nuevo adaptador que solo necesite una de las
 * dos capacidades puede implementar el puerto correspondiente sin arrastrar
 * el otro.
 */
public interface PersistencePort extends CatalogPersistencePort, SessionPersistencePort {
}
