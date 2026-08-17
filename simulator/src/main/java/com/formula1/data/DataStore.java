package com.formula1.data;

import com.formula1.model.Circuit;
import com.formula1.model.Driver;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SimulationConfig;
import com.formula1.model.Team;
import com.formula1.model.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Almacén en memoria de la aplicación y punto único de acceso a los datos.
 *
 * Los {@code Map} son la fuente de verdad en tiempo de ejecución —la
 * «persistencia temporal» que pide la especificación— y MongoDB actúa como
 * persistencia duradera detrás. Al arrancar se intenta cargar desde Mongo;
 * si no responde o está vacío, se siembra desde {@code seed.json}. Gracias a
 * eso la aplicación arranca y es utilizable aunque no haya servidor.
 *
 * Se usan colecciones concurrentes porque los hilos de fondo leen mientras
 * el hilo de JavaFX escribe.
 */
public final class DataStore {

    private static DataStore instancia;

    private final Map<Integer, Driver> pilotos = new ConcurrentHashMap<>();
    private final Map<String, Team> equipos = new ConcurrentHashMap<>();
    private final Map<String, Vehicle> vehiculos = new ConcurrentHashMap<>();
    private final Map<String, Circuit> circuitos = new ConcurrentHashMap<>();
    private final List<QualifyingSession> sesiones = new CopyOnWriteArrayList<>();

    private final CrudRepository<Driver, Integer> repoPilotos =
            new MongoRepository<>("pilotos", Driver.class, Driver::getId);
    private final CrudRepository<Team, String> repoEquipos =
            new MongoRepository<>("equipos", Team.class, Team::getNombre);
    private final CrudRepository<Vehicle, String> repoVehiculos =
            new MongoRepository<>("vehiculos", Vehicle.class, Vehicle::getModelo);
    private final CrudRepository<Circuit, String> repoCircuitos =
            new MongoRepository<>("circuitos", Circuit.class, Circuit::getNombre);
    private final CrudRepository<QualifyingSession, String> repoSesiones =
            new MongoRepository<>("sesiones", QualifyingSession.class, QualifyingSession::getId);

    private volatile boolean modoMemoria = true;
    private volatile String estado = "Sin cargar";
    private volatile SimulationConfig configuracionActual;

    private DataStore() {
    }

    public static synchronized DataStore getInstance() {
        if (instancia == null) {
            instancia = new DataStore();
        }
        return instancia;
    }

    /**
     * Crea un almacén aislado sembrado desde el JSON, sin tocar MongoDB.
     * Se usa en las pruebas y como punto de partida de una sesión limpia.
     */
    public static DataStore enMemoria() {
        DataStore almacen = new DataStore();
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
        if (MongoConnection.getInstance().isDisponible()) {
            modoMemoria = false;
            try {
                cargarDesdeMongo();
                if (pilotos.isEmpty()) {
                    sembrar();
                    volcarAMongo();
                    estado = "MongoDB conectado — base sembrada con los datos iniciales";
                } else {
                    estado = "MongoDB conectado";
                }
                return estado;
            } catch (RuntimeException e) {
                modoMemoria = true;
                sembrar();
                estado = "MongoDB falló (" + e.getMessage() + ") — modo memoria";
                return estado;
            }
        }
        modoMemoria = true;
        sembrar();
        estado = "Sin MongoDB — modo memoria";
        return estado;
    }

    private void cargarDesdeMongo() {
        repoPilotos.findAll().forEach(p -> pilotos.put(p.getId(), p));
        repoEquipos.findAll().forEach(e -> equipos.put(e.getNombre(), e));
        repoVehiculos.findAll().forEach(v -> vehiculos.put(v.getModelo(), v));
        repoCircuitos.findAll().forEach(c -> circuitos.put(c.getNombre(), c));
        repoSesiones.findAll().forEach(sesiones::add);
    }

    private void sembrar() {
        SeedLoader.Seed seed = SeedLoader.cargar();
        seed.getPilotos().forEach(p -> pilotos.put(p.getId(), p));
        seed.getEquipos().forEach(e -> equipos.put(e.getNombre(), e));
        seed.getVehiculos().forEach(v -> vehiculos.put(v.getModelo(), v));
        seed.getCircuitos().forEach(c -> circuitos.put(c.getNombre(), c));
    }

    private void volcarAMongo() {
        repoPilotos.saveAll(List.copyOf(pilotos.values()));
        repoEquipos.saveAll(List.copyOf(equipos.values()));
        repoVehiculos.saveAll(List.copyOf(vehiculos.values()));
        repoCircuitos.saveAll(List.copyOf(circuitos.values()));
    }

    /**
     * Ejecuta una escritura en MongoDB sin propagar el fallo: los mapas ya
     * se actualizaron y la interfaz no debe bloquearse ni revertir.
     */
    private void persistir(Runnable escritura) {
        if (modoMemoria) {
            return;
        }
        try {
            escritura.run();
        } catch (RuntimeException e) {
            estado = "Error al escribir en MongoDB — los cambios siguen en memoria";
        }
    }

    // --- pilotos ---------------------------------------------------------

    public Map<Integer, Driver> pilotos() {
        return pilotos;
    }

    public void guardarPiloto(Driver piloto) {
        pilotos.put(piloto.getId(), piloto);
        persistir(() -> repoPilotos.save(piloto));
    }

    public void eliminarPiloto(int id) {
        pilotos.remove(id);
        persistir(() -> repoPilotos.deleteById(id));
    }

    // --- equipos ---------------------------------------------------------

    public Map<String, Team> equipos() {
        return equipos;
    }

    public void guardarEquipo(Team equipo) {
        equipos.put(equipo.getNombre(), equipo);
        persistir(() -> repoEquipos.save(equipo));
    }

    public void eliminarEquipo(String nombre) {
        equipos.remove(nombre);
        persistir(() -> repoEquipos.deleteById(nombre));
    }

    // --- vehículos -------------------------------------------------------

    public Map<String, Vehicle> vehiculos() {
        return vehiculos;
    }

    public void guardarVehiculo(Vehicle vehiculo) {
        vehiculos.put(vehiculo.getModelo(), vehiculo);
        persistir(() -> repoVehiculos.save(vehiculo));
    }

    public void eliminarVehiculo(String modelo) {
        vehiculos.remove(modelo);
        persistir(() -> repoVehiculos.deleteById(modelo));
    }

    // --- circuitos -------------------------------------------------------

    public Map<String, Circuit> circuitos() {
        return circuitos;
    }

    public void guardarCircuito(Circuit circuito) {
        circuitos.put(circuito.getNombre(), circuito);
        persistir(() -> repoCircuitos.save(circuito));
    }

    public void eliminarCircuito(String nombre) {
        circuitos.remove(nombre);
        persistir(() -> repoCircuitos.deleteById(nombre));
    }

    // --- sesiones --------------------------------------------------------

    public List<QualifyingSession> sesiones() {
        return sesiones;
    }

    public void guardarSesion(QualifyingSession sesion) {
        sesiones.add(sesion);
        persistir(() -> repoSesiones.save(sesion));
    }

    // --- configuración en curso ------------------------------------------

    /**
     * Ajustes que el usuario dejó preparados en la pantalla de configuración,
     * a la espera de lanzar la sesión. Vive solo en memoria: al reiniciar, la
     * pantalla de clasificación recupera los de la última sesión guardada.
     */
    public SimulationConfig configuracionActual() {
        return configuracionActual;
    }

    public void guardarConfiguracion(SimulationConfig config) {
        this.configuracionActual = config;
    }

    public boolean isModoMemoria() {
        return modoMemoria;
    }

    public String getEstado() {
        return estado;
    }
}
