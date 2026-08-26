package com.formula1.data;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia (patrón Repository). Mantener esta abstracción
 * es lo que permite que la aplicación siga funcionando cuando MongoDB no
 * está disponible: basta con no invocar al repositorio.
 */

// Define q metodos debe tener una clase q la implemente, pero no como lo hace

public interface CrudRepository<T, ID> {

    T save(T entidad); 

    Optional<T> findById(ID id);  

    List<T> findAll(); 

    void deleteById(ID id); 

    void saveAll(List<T> entidades); 
}
