package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Piloto de la parrilla.
 *
 * El identificador es el entero 1..20 de la especificación, al que apuntan
 * {@code equipos.pilotos}, {@code vehiculos.pilotos} y los ganadores
 * históricos de cada circuito. El equipo se referencia por su nombre, que
 * es también la clave natural de {@link Team}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Driver {

    /** Claves de la tabla de habilidades, en escala 0-100. */
    public static final String HABILIDAD_VELOCIDAD = "velocidad";
    public static final String HABILIDAD_CONSISTENCIA = "consistencia";
    public static final String HABILIDAD_LLUVIA = "lluvia";

    private int id;
    private String nombre;
    private String equipo;
    private DriverRole rol;
    private int experiencia;
    private Map<String, Integer> habilidades;

    public Driver() {
        this.habilidades = new LinkedHashMap<>();
    }

    public Driver(int id, String nombre, String equipo, DriverRole rol, int experiencia) {
        this();
        this.id = id;
        this.nombre = nombre;
        this.equipo = equipo;
        this.rol = rol;
        this.experiencia = experiencia;
    }

    /** Devuelve una habilidad concreta, o 50 (valor medio) si no está definida. */
    public int getHabilidad(String clave) {
        return habilidades.getOrDefault(clave, 50);
    }

    public void setHabilidad(String clave, int valor) {
        this.habilidades.put(clave, valor);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEquipo() {
        return equipo;
    }

    public void setEquipo(String equipo) {
        this.equipo = equipo;
    }

    public DriverRole getRol() {
        return rol;
    }

    public void setRol(DriverRole rol) {
        this.rol = rol;
    }

    public int getExperiencia() {
        return experiencia;
    }

    public void setExperiencia(int experiencia) {
        this.experiencia = experiencia;
    }

    public Map<String, Integer> getHabilidades() {
        return habilidades;
    }

    public void setHabilidades(Map<String, Integer> habilidades) {
        this.habilidades = habilidades;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Driver)) return false;
        return id == ((Driver) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
