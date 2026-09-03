package com.formula1.service;

import com.formula1.application.port.out.CatalogPort;
import com.formula1.data.DataStore;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.Vehicle;
import com.formula1.util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Alta, consulta, búsqueda, comparación y asignación de pilotos a vehículos. */
public class VehicleService {

    private final CatalogPort datos;

    public VehicleService() {
        this(DataStore.getInstance());
    }

    public VehicleService(CatalogPort datos) {
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
        if (!ValidationUtils.isIdentifier(vehiculo.getModelo(), 40)) {
            throw new ValidationException("El modelo es obligatorio y contiene caracteres no válidos");
        }
        Vehicle existente = datos.vehiculos().get(vehiculo.getModelo());
        if (existente != null && existente != vehiculo) {
            throw new ValidationException("Ya existe un vehículo con el modelo " + vehiculo.getModelo());
        }
        if (!ValidationUtils.isNotBlank(vehiculo.getEquipo())
                || !datos.equipos().containsKey(vehiculo.getEquipo())) {
            throw new ValidationException("El vehículo debe pertenecer a un equipo existente");
        }
        if (!ValidationUtils.isIdentifier(vehiculo.getMotor(), 50)) {
            throw new ValidationException("El motor es obligatorio y contiene caracteres no válidos");
        }
        if (!ValidationUtils.isInRange(vehiculo.getVelocidadMaximaKmh(), 100, 400)) {
            throw new ValidationException("La velocidad máxima debe estar entre 100 y 400 km/h");
        }
        if (!ValidationUtils.isInRange(vehiculo.getAceleracion0100(), 1.0, 10.0)) {
            throw new ValidationException("La aceleración 0-100 debe estar entre 1 y 10 segundos");
        }
        if (vehiculo.getRendimiento() == null) {
            throw new ValidationException("El rendimiento del vehículo no puede estar vacío");
        }
        for (var entrada : vehiculo.getRendimiento().entrySet()) {
            Vehicle.Performance rendimiento = entrada.getValue();
            if (rendimiento == null || !ValidationUtils.isInRange(rendimiento.getVelocidadPromedioKmh(), 50, 400)) {
                throw new ValidationException("La velocidad media debe estar entre 50 y 400 km/h");
            }
            for (Double consumo : rendimiento.getConsumo().values()) {
                if (consumo == null || !ValidationUtils.isInRange(consumo, 0.01, 20)) {
                    throw new ValidationException("El consumo debe estar entre 0.01 y 20 por vuelta");
                }
            }
            for (Double desgaste : rendimiento.getDesgaste().values()) {
                if (desgaste == null || !ValidationUtils.isInRange(desgaste, 0.01, 100)) {
                    throw new ValidationException("El desgaste debe estar entre 0.01 y 100 por vuelta");
                }
            }
        }
        if (vehiculo.getPilotos() == null) {
            throw new ValidationException("La asignación de pilotos no puede ser nula");
        }
        for (Integer pilotoId : vehiculo.getPilotos()) {
            Driver piloto = datos.pilotos().get(pilotoId);
            if (piloto == null || !Objects.equals(piloto.getEquipo(), vehiculo.getEquipo())) {
                throw new ValidationException("Todos los pilotos asignados deben pertenecer al equipo del vehículo");
            }
        }
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto.trim().toLowerCase());
    }
}
