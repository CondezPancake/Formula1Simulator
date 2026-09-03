package com.formula1.service;

import com.formula1.data.CatalogPort;
import com.formula1.data.DataStore;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.Team;
import com.formula1.util.ValidationUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Alta, consulta, búsqueda, edición y baja de escuderías. */
public class TeamService {

    private final CatalogPort datos;

    public TeamService() {
        this(DataStore.getInstance());
    }

    public TeamService(CatalogPort datos) {
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
        if (!ValidationUtils.isIdentifier(equipo.getNombre(), 60)) {
            throw new ValidationException("El nombre del equipo contiene caracteres no válidos o supera 60 caracteres");
        }
        Team existente = datos.equipos().get(equipo.getNombre());
        if (existente != null && existente != equipo) {
            throw new ValidationException("Ya existe un equipo llamado " + equipo.getNombre());
        }
        if (!ValidationUtils.isPersonName(equipo.getPais(), 50)) {
            throw new ValidationException("El país es obligatorio y solo admite texto");
        }
        if (!ValidationUtils.isIdentifier(equipo.getMotor(), 50)) {
            throw new ValidationException("El motor es obligatorio y contiene caracteres no válidos");
        }
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase().contains(q);
    }
}
