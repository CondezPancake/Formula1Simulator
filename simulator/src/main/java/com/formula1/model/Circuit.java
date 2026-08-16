package com.formula1.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Circuit {

    private String id;
    private String nombre;
    private String pais;
    private double longitud;
    private int vueltas;
    private String descripcion;
    private double recordVuelta;
    private List<String> ganadores;

    public Circuit() {
        this.ganadores = new ArrayList<>();
    }

    public Circuit(String id, String nombre, String pais, double longitud, int vueltas, String descripcion) {
        this();
        this.id = id;
        this.nombre = nombre;
        this.pais = pais;
        this.longitud = longitud;
        this.vueltas = vueltas;
        this.descripcion = descripcion;
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

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public int getVueltas() {
        return vueltas;
    }

    public void setVueltas(int vueltas) {
        this.vueltas = vueltas;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getRecordVuelta() {
        return recordVuelta;
    }

    public void setRecordVuelta(double recordVuelta) {
        this.recordVuelta = recordVuelta;
    }

    public List<String> getGanadores() {
        return ganadores;
    }

    public void setGanadores(List<String> ganadores) {
        this.ganadores = ganadores;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Circuit)) return false;
        Circuit circuit = (Circuit) o;
        return Objects.equals(id, circuit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Circuit{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", pais='" + pais + '\'' +
                ", vueltas=" + vueltas +
                '}';
    }
}
