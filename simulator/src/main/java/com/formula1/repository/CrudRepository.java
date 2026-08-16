package com.formula1.repository;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de persistencia (patrón Repository) reutilizado por
 * todos los repositorios concretos del proyecto.
 */
public interface CrudRepository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    T update(T entity);

    void deleteById(ID id);
}
