package com.formula1.domain.model;

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
    private String imagen;
    private int numero;
    private String codigo;
    private String nacionalidad;
    private int victorias;
    private int campeonatos;
    private String fechaNacimiento;
    private String lugarNacimiento;
    private String biografia;

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

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    /** Dorsal del monoplaza (1, 44, 81…), independiente del {@link #getId()}. */
    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    /** Abreviatura de tres letras que usa la señal de TV (VER, HAM, LEC). */
    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    public int getVictorias() {
        return victorias;
    }

    public void setVictorias(int victorias) {
        this.victorias = victorias;
    }

    public int getCampeonatos() {
        return campeonatos;
    }

    public void setCampeonatos(int campeonatos) {
        this.campeonatos = campeonatos;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getLugarNacimiento() {
        return lugarNacimiento;
    }

    public void setLugarNacimiento(String lugarNacimiento) {
        this.lugarNacimiento = lugarNacimiento;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
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
