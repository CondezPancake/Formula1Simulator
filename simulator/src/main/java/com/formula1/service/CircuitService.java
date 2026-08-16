package com.formula1.service;

import com.formula1.data.DataStore;
import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.DrivingMode;
import com.formula1.model.Vehicle;
import com.formula1.model.WeatherCondition;
import com.formula1.util.ValidationUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Alta, consulta, búsqueda y estadísticas de circuitos. */
public class CircuitService {

    private final DataStore datos;

    public CircuitService() {
        this(DataStore.getInstance());
    }

    public CircuitService(DataStore datos) {
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
        if (!ValidationUtils.isNotBlank(circuito.getNombre())) {
            throw new ValidationException("El nombre del circuito no puede estar vacío");
        }
        if (!ValidationUtils.isPositive(circuito.getLongitudKm())) {
            throw new ValidationException("La longitud debe ser mayor que 0");
        }
        if (circuito.getVueltas() <= 0) {
            throw new ValidationException("El número de vueltas debe ser mayor que 0");
        }
    }

    private boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase().contains(q);
    }
}
