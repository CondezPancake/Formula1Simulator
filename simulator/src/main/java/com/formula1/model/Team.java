package com.formula1.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Team {

    private String id;
    private String nombre;
    private String pais;
    private List<String> driverIds;
    private List<String> vehicleIds;

    public Team() {
        this.driverIds = new ArrayList<>();
        this.vehicleIds = new ArrayList<>();
    }

    public Team(String id, String nombre, String pais) {
        this();
        this.id = id;
        this.nombre = nombre;
        this.pais = pais;
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

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public List<String> getDriverIds() {
        return driverIds;
    }

    public void setDriverIds(List<String> driverIds) {
        this.driverIds = driverIds;
    }

    public List<String> getVehicleIds() {
        return vehicleIds;
    }

    public void setVehicleIds(List<String> vehicleIds) {
        this.vehicleIds = vehicleIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team)) return false;
        Team team = (Team) o;
        return Objects.equals(id, team.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", pais='" + pais + '\'' +
                '}';
    }
}
