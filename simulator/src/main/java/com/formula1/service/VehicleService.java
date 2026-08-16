package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.Driver;
import com.formula1.model.Vehicle;
import com.formula1.util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Alta, consulta, búsqueda, comparación y asignación de pilotos a vehículos. */
public class VehicleService {

    private final DataStore datos;

    public VehicleService() {
        this(DataStore.getInstance());
    }

    public VehicleService(DataStore datos) {
        this.datos = datos;
    }

    public List<Vehicle> listar() {
        return datos.vehiculos().values().stream()
                .sorted(Comparator.comparing(Vehicle::getModelo))
                .collect(Collectors.toList());
    }

    /**
     * Búsqueda por características: texto libre sobre modelo/equipo/motor y,
     * opcionalmente, una velocidad máxima mínima.
     */
    public List<Vehicle> buscar(String texto, Integer velocidadMinima) {
        return listar().stream()
                .filter(v -> !ValidationUtils.isNotBlank(texto)
                        || contiene(v.getModelo(), texto) || contiene(v.getEquipo(), texto) || contiene(v.getMotor(), texto))
                .filter(v -> velocidadMinima == null || v.getVelocidadMaximaKmh() >= velocidadMinima)
                .collect(Collectors.toList());
    }

    public Optional<Vehicle> porModelo(String modelo) {
        return Optional.ofNullable(datos.vehiculos().get(modelo));
    }

    /** Vehículo con el que compite un piloto concreto. */
    public Optional<Vehicle> delPiloto(int pilotoId) {
        return listar().stream().filter(v -> v.conduce(pilotoId)).findFirst();
    }

    public Optional<Vehicle> delEquipo(String equipo) {
        return listar().stream()
                .filter(v -> v.getEquipo() != null && v.getEquipo().equalsIgnoreCase(equipo))
                .findFirst();
    }

    public List<Driver> pilotosDe(Vehicle vehiculo) {
        return vehiculo.getPilotos().stream()
                .map(id -> datos.pilotos().get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Asigna pilotos a un vehículo. La especificación exige que sea «según
     * su equipo», así que se rechaza cualquier piloto de otra escudería.
     */
    public Vehicle asignarPilotos(Vehicle vehiculo, List<Integer> pilotoIds) {
        for (Integer id : pilotoIds) {
            Driver piloto = datos.pilotos().get(id);
            if (piloto == null) {
                throw new ValidationException("El piloto " + id + " no existe");
            }
            if (!Objects.equals(piloto.getEquipo(), vehiculo.getEquipo())) {
                throw new ValidationException(
                        piloto.getNombre() + " corre para " + piloto.getEquipo()
                                + ", no puede pilotar un " + vehiculo.getEquipo());
            }
        }
        vehiculo.setPilotos(List.copyOf(pilotoIds));
        datos.guardarVehiculo(vehiculo);
        return vehiculo;
    }

    public Vehicle guardar(Vehicle vehiculo) {
        validar(vehiculo);
        datos.guardarVehiculo(vehiculo);
        return vehiculo;
    }

    public void eliminar(String modelo) {
        datos.eliminarVehiculo(modelo);
    }

    private void validar(Vehicle vehiculo) {
        if (vehiculo == null) {
            throw new ValidationException("El vehículo no puede ser nulo");
        }
        if (!ValidationUtils.isNotBlank(vehiculo.getModelo())) {
            throw new ValidationException("El modelo del vehículo no puede estar vacío");
        }
        if (!ValidationUtils.isNotBlank(vehiculo.getEquipo())
                || !datos.equipos().containsKey(vehiculo.getEquipo())) {
            throw new ValidationException("El vehículo debe pertenecer a un equipo existente");
        }
        if (!ValidationUtils.isPositive(vehiculo.getVelocidadMaximaKmh())) {
            throw new ValidationException("La velocidad máxima debe ser mayor que 0");
        }
        if (!ValidationUtils.isPositive(vehiculo.getAceleracion0100())) {
            throw new ValidationException("La aceleración debe ser mayor que 0");
        }
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto.trim().toLowerCase());
    }
}
