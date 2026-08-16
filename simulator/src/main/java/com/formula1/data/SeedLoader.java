package com.formula1.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga los datos iniciales desde {@code /data/seed.json}, que reproduce la
 * estructura de datos de la especificación (20 pilotos, 10 equipos, 10
 * vehículos y 7 circuitos).
 */
public final class SeedLoader {

    private static final String RECURSO = "/data/seed.json";

    private SeedLoader() {
    }

    public static Seed cargar() {
        try (InputStream entrada = SeedLoader.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new DataAccessException("No se encontró el recurso " + RECURSO);
            }
            return new ObjectMapper().readValue(entrada, Seed.class);
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new DataAccessException("No se pudieron cargar los datos iniciales", e);
        }
    }

    /** Contenido del archivo de datos iniciales. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seed {

        private List<Driver> pilotos = new ArrayList<>();
        private List<Team> equipos = new ArrayList<>();
        private List<Vehicle> vehiculos = new ArrayList<>();
        private List<Circuit> circuitos = new ArrayList<>();

        public List<Driver> getPilotos() {
            return pilotos;
        }

        public void setPilotos(List<Driver> pilotos) {
            this.pilotos = pilotos;
        }

        public List<Team> getEquipos() {
            return equipos;
        }

        public void setEquipos(List<Team> equipos) {
            this.equipos = equipos;
        }

        public List<Vehicle> getVehiculos() {
            return vehiculos;
        }

        public void setVehiculos(List<Vehicle> vehiculos) {
            this.vehiculos = vehiculos;
        }

        public List<Circuit> getCircuitos() {
            return circuitos;
        }

        public void setCircuitos(List<Circuit> circuitos) {
            this.circuitos = circuitos;
        }
    }
}
