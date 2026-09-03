package com.formula1.data;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;

import java.util.Map;

/**
 * Puerto de salida que expone el catálogo en memoria a los servicios de
 * dominio (pilotos, equipos, vehículos, circuitos), sin atarlos al singleton
 * concreto {@link DataStore}. Fase 2 de la migración a hexagonal: los
 * servicios de catálogo pasan a depender de este contrato en vez de
 * {@code DataStore.getInstance()}.
 */
public interface CatalogPort {

    Map<Integer, Driver> pilotos();

    void guardarPiloto(Driver piloto);

    void eliminarPiloto(int id);

    Map<String, Team> equipos();

    void guardarEquipo(Team equipo);

    void eliminarEquipo(String nombre);

    Map<String, Vehicle> vehiculos();

    void guardarVehiculo(Vehicle vehiculo);

    void eliminarVehiculo(String modelo);

    Map<String, Circuit> circuitos();

    void guardarCircuito(Circuit circuito);

    void eliminarCircuito(String nombre);
}
