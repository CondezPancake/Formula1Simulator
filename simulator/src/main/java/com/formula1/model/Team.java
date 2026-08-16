package com.formula1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Escudería. El nombre es la clave natural: es único en la parrilla y es a
 * lo que apuntan {@link Driver#getEquipo()} y {@link Vehicle#getEquipo()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {

    private String nombre;
    private String pais;
    private String motor;
    private List<Integer> pilotos;
    private String imagen;

    public Team() {
        this.pilotos = new ArrayList<>();
    }

    public Team(String nombre, String pais, String motor) {
        this();
        this.nombre = nombre;
        this.pais = pais;
        this.motor = motor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getMotor() {
        return motor;
    }

    public void setMotor(String motor) {
        this.motor = motor;
    }

    public List<Integer> getPilotos() {
        return pilotos;
    }

    public void setPilotos(List<Integer> pilotos) {
        this.pilotos = pilotos;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team)) return false;
        return Objects.equals(nombre, ((Team) o).nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
