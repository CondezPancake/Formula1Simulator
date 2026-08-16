package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.Driver;
import com.formula1.model.Team;
import com.formula1.util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Alta, consulta, búsqueda, edición y baja de escuderías. */
public class TeamService {

    private final DataStore datos;

    public TeamService() {
        this(DataStore.getInstance());
    }

    public TeamService(DataStore datos) {
        this.datos = datos;
    }

    public List<Team> listar() {
        return datos.equipos().values().stream()
                .sorted(Comparator.comparing(Team::getNombre))
                .collect(Collectors.toList());
    }

    /** Busca por nombre, país o motor. */
    public List<Team> buscar(String texto) {
        if (!ValidationUtils.isNotBlank(texto)) {
            return listar();
        }
        String q = texto.trim().toLowerCase();
        return listar().stream()
                .filter(e -> contiene(e.getNombre(), q) || contiene(e.getPais(), q) || contiene(e.getMotor(), q))
                .collect(Collectors.toList());
    }

    public Optional<Team> porNombre(String nombre) {
        return Optional.ofNullable(datos.equipos().get(nombre));
    }

    /** Pilotos que corren para el equipo, resolviendo los ids a entidades. */
    public List<Driver> pilotosDe(Team equipo) {
        return equipo.getPilotos().stream()
                .map(id -> datos.pilotos().get(id))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Team guardar(Team equipo) {
        validar(equipo);
        datos.guardarEquipo(equipo);
        return equipo;
    }

    public void eliminar(String nombre) {
        if (!datos.pilotos().values().stream().noneMatch(p -> nombre.equals(p.getEquipo()))) {
            throw new ValidationException("No se puede eliminar " + nombre + ": aún tiene pilotos asignados");
        }
        datos.eliminarEquipo(nombre);
    }

    private void validar(Team equipo) {
        if (equipo == null) {
            throw new ValidationException("El equipo no puede ser nulo");
        }
        if (!ValidationUtils.isNotBlank(equipo.getNombre())) {
            throw new ValidationException("El nombre del equipo no puede estar vacío");
        }
        if (!ValidationUtils.isNotBlank(equipo.getMotor())) {
            throw new ValidationException("El equipo debe indicar su motor");
        }
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase().contains(q);
    }
}
