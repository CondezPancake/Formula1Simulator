package com.formula1.data;

import com.formula1.application.port.out.PersistencePort;
import com.formula1.application.port.out.PreparedConfigPort;
import com.formula1.application.port.out.QualifyingDataPort;
import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.QualifyingSession;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.Team;
import com.formula1.domain.model.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Almacén en memoria de la aplicación y punto único de acceso a los datos.
 *
 * Los {@code Map} son la fuente de verdad en tiempo de ejecución —la
 * «persistencia temporal» que pide la especificación— y MySQL actúa como
 * persistencia duradera detrás. Al arrancar se intenta cargar mediante el
 * puerto de persistencia; si no responde, se siembra desde {@code seed.json}. Gracias a
 * eso la aplicación arranca y es utilizable aunque no haya servidor.
 *
 * Se usan colecciones concurrentes porque los hilos de fondo leen mientras
 * el hilo de JavaFX escribe.
 */
public final class DataStore implements QualifyingDataPort, PreparedConfigPort {

    private static DataStore instancia;

    private final Map<Integer, Driver> pilotos = new ConcurrentHashMap<>();
    private final Map<String, Team> equipos = new ConcurrentHashMap<>();
    private final Map<String, Vehicle> vehiculos = new ConcurrentHashMap<>();
    private final Map<String, Circuit> circuitos = new ConcurrentHashMap<>();
    private final List<QualifyingSession> sesiones = new CopyOnWriteArrayList<>();

    private final PersistencePort persistence;

    private volatile boolean modoMemoria = true;
    private volatile boolean cargado = false;
    private volatile String estado = "Sin cargar";
    private volatile SimulationConfig configuracionActual;
    private final AtomicLong versionConfiguracion = new AtomicLong();

    private DataStore() {
        this(new MySqlPersistenceAdapter());
    }

    private DataStore(PersistencePort persistence) {
        this.persistence = persistence;
    }

    public static synchronized DataStore getInstance() {
        if (instancia == null) {
            instancia = new DataStore();
        }
        return instancia;
    }

    /**
     * Crea un almacén aislado sembrado desde el JSON, sin abrir JDBC.
     * Se usa en las pruebas y como punto de partida de una sesión limpia.
     */
    public static DataStore enMemoria() {
        DataStore almacen = new DataStore(null);
        almacen.sembrar();
        almacen.modoMemoria = true;
        almacen.estado = "Modo memoria";
        return almacen;
    }

    /**
     * Puebla los mapas. Se ejecuta en segundo plano al arrancar.
     *
     * @return descripción del origen de los datos, para la barra de estado.
     */
    public synchronized String cargar() {
        try {
            if (DatabaseConnection.isAvailable()) {
                modoMemoria = false;
                try {
                    cargarDesdeSql();
                    if (pilotos.isEmpty()) {
                        throw new DataAccessException("La base SQL no contiene pilotos; ejecuta database/SQL/data.sql");
                    } else {
                        estado = "MySQL conectado";
                    }
                    return estado;
                } catch (RuntimeException e) {
                    modoMemoria = true;
                    sembrar();
                    estado = "MySQL falló (" + e.getMessage() + ") — modo memoria";
                    return estado;
                }
            }
            modoMemoria = true;
            sembrar();
            estado = "Sin MySQL — modo memoria";
            return estado;
        } finally {
            cargado = true;
        }
    }

    /** Indica si {@link #cargar()} ya terminó, para no repetir la carga inicial. */
    public boolean estaCargado() {
        return cargado;
    }

    private void cargarDesdeSql() {
        PersistencePort.CatalogData data = persistence.loadCatalogs();
        data.drivers().forEach(p -> pilotos.put(p.getId(), p));
        data.teams().forEach(e -> equipos.put(e.getNombre(), e));
        data.vehicles().forEach(v -> vehiculos.put(v.getModelo(), v));
        data.circuits().forEach(c -> circuitos.put(c.getNombre(), c));
        sesiones.addAll(persistence.loadSessions());
    }

    private void sembrar() {
        SeedLoader.Seed seed = SeedLoader.cargar();
        seed.getPilotos().forEach(p -> pilotos.put(p.getId(), p));
        seed.getEquipos().forEach(e -> equipos.put(e.getNombre(), e));
        seed.getVehiculos().forEach(v -> vehiculos.put(v.getModelo(), v));
        seed.getCircuitos().forEach(c -> circuitos.put(c.getNombre(), c));
    }

    /**
     * Ejecuta una escritura SQL sin propagar el fallo: los mapas ya
     * se actualizaron y la interfaz no debe bloquearse ni revertir.
     */
    private void persistir(Runnable escritura) {
        if (modoMemoria) {
            return;
        }
        try {
            escritura.run();
        } catch (RuntimeException e) {
            estado = "Error al escribir en MySQL — los cambios siguen en memoria";
        }
    }

    // --- pilotos ---------------------------------------------------------

    @Override
    public Map<Integer, Driver> pilotos() {
        return pilotos;
    }

    @Override
    public void guardarPiloto(Driver piloto) {
        pilotos.put(piloto.getId(), piloto);
        sincronizarPilotosDeEquipos();
        persistir(() -> persistence.saveDriver(piloto));
    }

    @Override
    public void eliminarPiloto(int id) {
        pilotos.remove(id);
        sincronizarPilotosDeEquipos();
        persistir(() -> persistence.deleteDriver(id));
    }

    /** Mantiene la relación Equipo-Pilotos derivada de la fuente de verdad del piloto. */
    private void sincronizarPilotosDeEquipos() {
        equipos.values().forEach(equipo -> {
            if (equipo.getPilotos() == null) {
                equipo.setPilotos(new java.util.ArrayList<>());
            } else {
                equipo.getPilotos().clear();
            }
        });
        pilotos.values().forEach(piloto -> {
            Team equipo = equipos.get(piloto.getEquipo());
            if (equipo != null && !equipo.getPilotos().contains(piloto.getId())) {
                equipo.getPilotos().add(piloto.getId());
            }
        });
        equipos.values().forEach(equipo -> equipo.getPilotos().sort(Integer::compareTo));
    }

    // --- equipos ---------------------------------------------------------

    @Override
    public Map<String, Team> equipos() {
        return equipos;
    }

    @Override
    public void guardarEquipo(Team equipo) {
        equipos.put(equipo.getNombre(), equipo);
        persistir(() -> persistence.saveTeam(equipo));
    }

    @Override
    public void eliminarEquipo(String nombre) {
        equipos.remove(nombre);
        persistir(() -> persistence.deleteTeam(nombre));
    }

    // --- vehículos -------------------------------------------------------

    @Override
    public Map<String, Vehicle> vehiculos() {
        return vehiculos;
    }

    @Override
    public void guardarVehiculo(Vehicle vehiculo) {
        vehiculos.put(vehiculo.getModelo(), vehiculo);
        persistir(() -> persistence.saveVehicle(vehiculo));
    }

    @Override
    public void eliminarVehiculo(String modelo) {
        vehiculos.remove(modelo);
        persistir(() -> persistence.deleteVehicle(modelo));
    }

    // --- circuitos -------------------------------------------------------

    @Override
    public Map<String, Circuit> circuitos() {
        return circuitos;
    }

    @Override
    public void guardarCircuito(Circuit circuito) {
        circuitos.put(circuito.getNombre(), circuito);
        persistir(() -> persistence.saveCircuit(circuito));
    }

    @Override
    public void eliminarCircuito(String nombre) {
        circuitos.remove(nombre);
        persistir(() -> persistence.deleteCircuit(nombre));
    }

    // --- sesiones --------------------------------------------------------

    @Override
    public List<QualifyingSession> sesiones() {
        return sesiones;
    }

    @Override
    public void guardarSesion(QualifyingSession sesion) {
        sesiones.add(sesion);
        persistir(() -> persistence.saveSession(sesion));
    }

    // --- configuración en curso ------------------------------------------

    /**
     * Ajustes que el usuario dejó preparados en la pantalla de configuración,
     * a la espera de lanzar la sesión. Vive solo en memoria: al reiniciar, la
     * pantalla de clasificación recupera los de la última sesión guardada.
     */
    @Override
    public SimulationConfig configuracionActual() {
        return configuracionActual;
    }

    @Override
    public void guardarConfiguracion(SimulationConfig config) {
        this.configuracionActual = config;
        versionConfiguracion.incrementAndGet();
    }

    /** Identifica si Carrera ya aplicó el último ajuste preparado. */
    @Override
    public long versionConfiguracion() {
        return versionConfiguracion.get();
    }

    public boolean isModoMemoria() {
        return modoMemoria;
    }

    public String getEstado() {
        return estado;
    }
}
