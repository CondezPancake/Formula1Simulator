package com.formula1.application.usecase;

import com.formula1.domain.service.ValidationException;

import com.formula1.application.port.out.CatalogPort;
import com.formula1.data.DataStore;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherCondition;
import com.formula1.util.ValidationUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Alta, consulta, búsqueda y estadísticas de circuitos. */
public class CircuitService {

    private final CatalogPort datos;

    public CircuitService() {
        this(DataStore.getInstance());
    }

    public CircuitService(CatalogPort datos) {
        this.datos = datos;
    }

    public List<Circuit> listar() {
        return datos.circuitos().values().stream()
                .sorted(Comparator.comparing(Circuit::getNombre))
                .collect(Collectors.toList());
    }

    /** Busca por nombre o ubicación, tal como pide la especificación. */
    public List<Circuit> buscar(String texto) {
        if (!ValidationUtils.isNotBlank(texto)) {
            return listar();
        }
        String q = texto.trim().toLowerCase();
        return listar().stream()
                .filter(c -> contiene(c.getNombre(), q) || contiene(c.getPais(), q))
                .collect(Collectors.toList());
    }

    public Optional<Circuit> porNombre(String nombre) {
        return Optional.ofNullable(datos.circuitos().get(nombre));
    }

    /** Ganadores históricos con el nombre del piloto ya resuelto. */
    public List<String> ganadoresDe(Circuit circuito) {
        return circuito.getGanadores().stream()
                .sorted(Comparator.comparingInt(Circuit.Winner::getTemporada))
                .map(g -> {
                    Driver piloto = datos.pilotos().get(g.getPilotoId());
                    return g.getTemporada() + " — " + (piloto != null ? piloto.getNombre() : "Piloto " + g.getPilotoId());
                })
                .collect(Collectors.toList());
    }

    /**
     * Impacto del circuito sobre el rendimiento de un vehículo: consumo y
     * desgaste estimados por vuelta en cada modo de conducción.
     */
    public Map<DrivingMode, double[]> impactoSobre(Circuit circuito, Vehicle vehiculo, WeatherCondition clima) {
        Map<DrivingMode, double[]> impacto = new LinkedHashMap<>();
        for (DrivingMode modo : DrivingMode.values()) {
            Vehicle.Performance rendimiento = vehiculo.rendimientoDe(modo);
            impacto.put(modo, new double[]{
                    rendimiento.consumoCon(clima) * circuito.getFactorConsumo(),
                    rendimiento.desgasteCon(clima) * circuito.getFactorDesgaste()
            });
        }
        return impacto;
    }

    public Circuit guardar(Circuit circuito) {
        validar(circuito);
        if (circuito.getRecordVuelta() != null) {
            circuito.setFactorTecnico(circuito.calcularFactorTecnico());
        }
        datos.guardarCircuito(circuito);
        return circuito;
    }

    public void eliminar(String nombre) {
        datos.eliminarCircuito(nombre);
    }

    private void validar(Circuit circuito) {
        if (circuito == null) {
            throw new ValidationException("El circuito no puede ser nulo");
        }
        if (!ValidationUtils.isIdentifier(circuito.getNombre(), 80)) {
            throw new ValidationException("El nombre del circuito es obligatorio o contiene caracteres no válidos");
        }
        Circuit existente = datos.circuitos().get(circuito.getNombre());
        if (existente != null && existente != circuito) {
            throw new ValidationException("Ya existe un circuito llamado " + circuito.getNombre());
        }
        if (!ValidationUtils.isPersonName(circuito.getPais(), 50)) {
            throw new ValidationException("El país es obligatorio y solo admite texto");
        }
        if (!ValidationUtils.isInRange(circuito.getLongitudKm(), 0.1, 30.0)) {
            throw new ValidationException("La longitud debe estar entre 0.1 y 30 km");
        }
        if (!ValidationUtils.isInRange(circuito.getVueltas(), 1, 200)) {
            throw new ValidationException("El número de vueltas debe estar entre 1 y 200");
        }
        if (!ValidationUtils.hasLength(circuito.getDescripcion(), 1, 500)) {
            throw new ValidationException("La descripción es obligatoria y no puede superar 500 caracteres");
        }
        if (!ValidationUtils.isInRange(circuito.getFactorConsumo(), 0.1, 5.0)
                || !ValidationUtils.isInRange(circuito.getFactorDesgaste(), 0.1, 5.0)) {
            throw new ValidationException("Los factores de consumo y desgaste deben estar entre 0.1 y 5");
        }
        if (circuito.getRecordVuelta() != null) {
            if (!ValidationUtils.isInRange(circuito.getRecordVuelta().getTiempoSegundos(), 10, 600)) {
                throw new ValidationException("El récord de vuelta debe estar entre 0:10.000 y 9:59.999");
            }
            if (!ValidationUtils.isPersonName(circuito.getRecordVuelta().getPiloto(), 60)) {
                throw new ValidationException("El autor del récord debe ser un nombre válido");
            }
        }
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase().contains(q);
    }
}
