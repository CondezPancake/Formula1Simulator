package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.Driver;
import com.formula1.util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Alta, consulta, búsqueda, edición y baja de pilotos. */
public class DriverService {

    private final DataStore datos;

    public DriverService() {
        this(DataStore.getInstance());
    }

    public DriverService(DataStore datos) {
        this.datos = datos;
    }

    public List<Driver> listar() {
        return datos.pilotos().values().stream()
                .sorted(Comparator.comparingInt(Driver::getId))
                .collect(Collectors.toList());
    }

    /** Busca por nombre, equipo o rol; sin texto devuelve la lista completa. */
    public List<Driver> buscar(String texto) {
        if (!ValidationUtils.isNotBlank(texto)) {
            return listar();
        }
        String q = texto.trim().toLowerCase();
        return listar().stream()
                .filter(p -> contiene(p.getNombre(), q)
                        || contiene(p.getEquipo(), q)
                        || (p.getRol() != null && contiene(p.getRol().getEtiqueta(), q)))
                .collect(Collectors.toList());
    }

    public List<Driver> porEquipo(String equipo) {
        return listar().stream()
                .filter(p -> p.getEquipo() != null && p.getEquipo().equalsIgnoreCase(equipo))
                .collect(Collectors.toList());
    }

    public Optional<Driver> porId(int id) {
        return Optional.ofNullable(datos.pilotos().get(id));
    }

    public Driver guardar(Driver piloto) {
        validar(piloto);
        datos.guardarPiloto(piloto);
        return piloto;
    }

    public void eliminar(int id) {
        datos.eliminarPiloto(id);
    }

    /** Siguiente identificador libre, para dar de alta pilotos nuevos. */
    public int siguienteId() {
        return datos.pilotos().keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private void validar(Driver piloto) {
        if (piloto == null) {
            throw new ValidationException("El piloto no puede ser nulo");
        }
        Driver existente = datos.pilotos().get(piloto.getId());
        if (piloto.getId() <= 0) {
            throw new ValidationException("El identificador del piloto debe ser mayor que 0");
        }
        if (existente != null && existente != piloto) {
            throw new ValidationException("Ya existe un piloto con el identificador " + piloto.getId());
        }
        if (!ValidationUtils.isPersonName(piloto.getNombre(), 60)) {
            throw new ValidationException(
                    "El nombre del piloto es obligatorio y solo admite letras, espacios, puntos, apóstrofes o guiones");
        }
        if (!ValidationUtils.isNotBlank(piloto.getEquipo())) {
            throw new ValidationException("El piloto debe pertenecer a un equipo");
        }
        if (!datos.equipos().containsKey(piloto.getEquipo())) {
            throw new ValidationException("El equipo no existe: " + piloto.getEquipo());
        }
        if (piloto.getRol() == null) {
            throw new ValidationException("El piloto debe tener un rol");
        }
        if (!ValidationUtils.isInRange(piloto.getExperiencia(), 0, 30)) {
            throw new ValidationException("La experiencia debe estar entre 0 y 30 años");
        }
        if (piloto.getHabilidades() == null) {
            throw new ValidationException("Las habilidades del piloto no pueden estar vacías");
        }
        for (String habilidad : List.of(Driver.HABILIDAD_VELOCIDAD,
                Driver.HABILIDAD_CONSISTENCIA, Driver.HABILIDAD_LLUVIA)) {
            if (!ValidationUtils.isInRange(piloto.getHabilidad(habilidad), 0, 100)) {
                throw new ValidationException("Las habilidades del piloto deben estar entre 0 y 100");
            }
        }
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase().contains(q);
    }
}
