package com.formula1.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Driver {

    private String id;
    private String nombre;
    private int numero;
    private String teamId;
    private DriverRole rol;
    private int experiencia;
    private Map<String, Integer> habilidades;

    public Driver() {
        this.habilidades = new HashMap<>();
    }

    public Driver(String id, String nombre, int numero, String teamId, DriverRole rol, int experiencia) {
        this();
        this.id = id;
        this.nombre = nombre;
        this.numero = numero;
        this.teamId = teamId;
        this.rol = rol;
        this.experiencia = experiencia;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
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
        Driver driver = (Driver) o;
        return Objects.equals(id, driver.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Driver{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", numero=" + numero +
                ", teamId='" + teamId + '\'' +
                ", rol=" + rol +
                '}';
    }
}
