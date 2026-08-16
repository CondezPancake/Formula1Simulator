package com.formula1.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.Team;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

/**
 * Adapta la API REST de OpenF1 (https://openf1.org) al contrato
 * {@link F1DataProvider} usado por el resto del sistema (patrón Adapter).
 */
public class OpenF1ApiAdapter implements F1DataProvider {

    private static final String BASE_URL = "https://api.openf1.org/v1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenF1ApiAdapter() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Driver> fetchDrivers() {
        throw new UnsupportedOperationException("TODO: implementar consumo de " + BASE_URL + "/drivers y mapeo a Driver");
    }

    @Override
    public List<Team> fetchTeams() {
        throw new UnsupportedOperationException("TODO: implementar consumo de OpenF1 y mapeo a Team");
    }

    @Override
    public List<Circuit> fetchCircuits() {
        throw new UnsupportedOperationException("TODO: implementar consumo de " + BASE_URL + "/meetings y mapeo a Circuit");
    }

    protected HttpRequest buildRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET()
                .build();
    }
}
