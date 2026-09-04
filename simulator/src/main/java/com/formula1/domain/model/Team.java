package com.formula1.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Escudería. El nombre es la clave natural: es único en la parrilla y es a
 * lo que apuntan {@link Driver#getEquipo()} y {@link Vehicle#getEquipo()}.
 *
 * <p>Los campos de ficha —base, jefes, palmarés y año de debut— son los que
 * publica formula1.com en la página de cada equipo. No intervienen en la
 * simulación: existen para que la ficha de escudería tenga algo que contar.
 * Un equipo dado de alta desde Gestión los deja vacíos, así que todo lo que
 * los pinte tiene que tolerar el {@code null} y el cero.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {

    private String nombre;
    private String pais;
    private String motor;
    private List<Integer> pilotos;
    private String imagen;

    // --- ficha (datos de catálogo, no de simulación) ---
    @JsonProperty("nombre_completo")
    private String nombreCompleto;
    private String base;
    @JsonProperty("jefe_equipo")
    private String jefeEquipo;
    @JsonProperty("jefe_tecnico")
    private String jefeTecnico;
    @JsonProperty("piloto_reserva")
    private String pilotoReserva;
    @JsonProperty("primera_participacion")
    private int primeraParticipacion;
    private int campeonatos;
    @JsonProperty("gran_premios")
    private int granPremios;
    private int victorias;
    private int podios;
    private int poles;
    private String descripcion;

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

    /** Denominación comercial completa, con patrocinador title. */
    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    /** Sede de la fábrica, como la publica la F1: ciudad y país. */
    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getJefeEquipo() {
        return jefeEquipo;
    }

    public void setJefeEquipo(String jefeEquipo) {
        this.jefeEquipo = jefeEquipo;
    }

    public String getJefeTecnico() {
        return jefeTecnico;
    }

    public void setJefeTecnico(String jefeTecnico) {
        this.jefeTecnico = jefeTecnico;
    }

    public String getPilotoReserva() {
        return pilotoReserva;
    }

    public void setPilotoReserva(String pilotoReserva) {
        this.pilotoReserva = pilotoReserva;
    }

    /** Año de su primer Gran Premio; 0 si no se conoce. */
    public int getPrimeraParticipacion() {
        return primeraParticipacion;
    }

    public void setPrimeraParticipacion(int primeraParticipacion) {
        this.primeraParticipacion = primeraParticipacion;
    }

    /** Mundiales de constructores. */
    public int getCampeonatos() {
        return campeonatos;
    }

    public void setCampeonatos(int campeonatos) {
        this.campeonatos = campeonatos;
    }

    public int getGranPremios() {
        return granPremios;
    }

    public void setGranPremios(int granPremios) {
        this.granPremios = granPremios;
    }

    public int getVictorias() {
        return victorias;
    }

    public void setVictorias(int victorias) {
        this.victorias = victorias;
    }

    public int getPodios() {
        return podios;
    }

    public void setPodios(int podios) {
        this.podios = podios;
    }

    public int getPoles() {
        return poles;
    }

    public void setPoles(int poles) {
        this.poles = poles;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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
