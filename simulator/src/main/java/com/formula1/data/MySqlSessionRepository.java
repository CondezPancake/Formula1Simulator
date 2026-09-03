package com.formula1.data;

import com.formula1.model.AerodynamicLoad;
import com.formula1.model.DynamicWeatherState;
import com.formula1.model.EventImpact;
import com.formula1.model.EventOccurrence;
import com.formula1.model.EventType;
import com.formula1.model.FuelStrategy;
import com.formula1.model.LapResult;
import com.formula1.model.LapStatus;
import com.formula1.model.PitStopPhase;
import com.formula1.model.PitStopReason;
import com.formula1.model.PitStopRecord;
import com.formula1.model.QualifyingSession;
import com.formula1.model.SectorTimes;
import com.formula1.model.SimulationConfig;
import com.formula1.model.TelemetrySnapshot;
import com.formula1.model.TireChangeRecord;
import com.formula1.model.TireCompound;
import com.formula1.model.TirePressure;
import com.formula1.model.TrackEvolutionSnapshot;
import com.formula1.model.TrackFlag;
import com.formula1.model.TrackSector;
import com.formula1.model.WeatherCondition;
import com.formula1.model.WeatherSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persiste y reconstruye una sesión completa dentro de una transacción. */
final class MySqlSessionRepository {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    void save(Connection connection, QualifyingSession session) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            deleteExisting(connection, session.getId());
            insertConfig(connection, session);
            insertSession(connection, session);
            insertResults(connection, session);
            Map<EventOccurrence, Long> eventIds = insertEvents(connection, session);
            insertWeather(connection, session);
            insertTelemetry(connection, session, eventIds);
            insertTrackEvolution(connection, session);
            insertPitStops(connection, session);
            insertTireChanges(connection, session);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    List<QualifyingSession> loadAll(Connection connection) throws SQLException {
        Map<String, QualifyingSession> sessions = loadHeaders(connection);
        if (sessions.isEmpty()) return List.of();
        loadResults(connection, sessions);
        loadEvents(connection, sessions);
        loadWeather(connection, sessions);
        loadTelemetry(connection, sessions);
        loadTrackEvolution(connection, sessions);
        loadPitStopsAndChanges(connection, sessions);
        return new ArrayList<>(sessions.values());
    }

    void delete(Connection connection, String sessionId) throws SQLException {
        deleteExisting(connection, sessionId);
    }

    private Map<String, QualifyingSession> loadHeaders(Connection c) throws SQLException {
        String sql = """
                SELECT s.sesion_id,s.condicion_inicial_codigo,s.fecha,
                       cfg.configuracion_id,cfg.circuito_nombre_snapshot,cfg.piloto_id,
                       cfg.vehiculo_modelo_snapshot,cfg.modo_codigo,cfg.carga_codigo,
                       cfg.presion_codigo,cfg.compuesto_inicial_codigo,
                       cfg.estrategia_combustible_codigo,cfg.duracion_segundos,cfg.guardado_en
                  FROM sesion_clasificacion s
                  JOIN configuracion_simulacion cfg ON cfg.configuracion_id=s.configuracion_id
                 ORDER BY s.fecha
                """;
        Map<String, QualifyingSession> sessions = new LinkedHashMap<>();
        try (Statement statement = c.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                SimulationConfig config = new SimulationConfig();
                config.setId(rs.getString("configuracion_id"));
                config.setCircuito(rs.getString("circuito_nombre_snapshot"));
                config.setPilotoId(rs.getInt("piloto_id"));
                config.setVehiculo(rs.getString("vehiculo_modelo_snapshot"));
                config.setModo(com.formula1.model.DrivingMode.desdeClave(rs.getString("modo_codigo")));
                config.setAerodinamica(AerodynamicLoad.desdeClave(rs.getString("carga_codigo")));
                config.setPresion(TirePressure.desdeClave(rs.getString("presion_codigo")));
                config.setCompuestoInicial(compound(rs.getString("compuesto_inicial_codigo")));
                config.setCombustible(FuelStrategy.desdeClave(rs.getString("estrategia_combustible_codigo")));
                config.setDuracionSegundos(rs.getInt("duracion_segundos"));
                Timestamp saved = rs.getTimestamp("guardado_en");
                config.setGuardadoEn(saved == null ? null : saved.toLocalDateTime().format(DATE_FORMAT));

                QualifyingSession session = new QualifyingSession(
                        config.getCircuito(), WeatherCondition.desdeClave(rs.getString("condicion_inicial_codigo")), config);
                session.setId(rs.getString("sesion_id"));
                session.setFecha(rs.getTimestamp("fecha").toLocalDateTime().format(DATE_FORMAT));
                sessions.put(session.getId(), session);
            }
        }
        return sessions;
    }

    private void loadResults(Connection c, Map<String, QualifyingSession> sessions) throws SQLException {
        String sql = """
                SELECT r.*, s.sector_codigo, s.tiempo_segundos sector_tiempo
                  FROM resultado_vuelta r
                  LEFT JOIN tiempo_sector s ON s.sesion_id=r.sesion_id AND s.piloto_id=r.piloto_id
                 ORDER BY r.sesion_id,r.posicion,s.sector_codigo
                """;
        Map<String, Map<Integer, LapResult>> bySession = new HashMap<>();
        Map<String, double[]> sectors = new HashMap<>();
        try (Statement statement = c.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                String sessionId = rs.getString("sesion_id");
                if (!sessions.containsKey(sessionId)) continue;
                Map<Integer, LapResult> results = bySession.computeIfAbsent(sessionId, ignored -> new LinkedHashMap<>());
                int driverId = rs.getInt("piloto_id");
                LapResult result = results.computeIfAbsent(driverId, ignored -> {
                    try {
                        LapResult value = new LapResult(driverId, rs.getString("piloto_nombre_snapshot"),
                                rs.getString("equipo_nombre_snapshot"), rs.getString("vehiculo_modelo_snapshot"),
                                rs.getDouble("tiempo_segundos"));
                        value.setPosicion(rs.getInt("posicion")); value.setGap(rs.getDouble("gap_segundos"));
                        value.setConsumoEstimado(rs.getDouble("consumo_estimado"));
                        value.setDesgasteEstimado(rs.getDouble("desgaste_estimado"));
                        value.setEstadoVuelta(LapStatus.valueOf(rs.getString("estado_codigo")));
                        value.setSectorIncidente(TrackSector.valueOf(rs.getString("sector_incidente_codigo")));
                        value.setEventos(new ArrayList<>());
                        return value;
                    } catch (SQLException e) {
                        throw new DataAccessException("No se pudo reconstruir un resultado", e);
                    }
                });
                String sectorCode = rs.getString("sector_codigo");
                if (sectorCode != null) {
                    double[] values = sectors.computeIfAbsent(sessionId + ':' + driverId, ignored -> new double[3]);
                    TrackSector sector = TrackSector.valueOf(sectorCode);
                    if (sector != TrackSector.NONE) values[sector.ordinal() - 1] = rs.getDouble("sector_tiempo");
                }
            }
        }
        bySession.forEach((sessionId, results) -> {
            results.forEach((driverId, result) -> {
                double[] values = sectors.get(sessionId + ':' + driverId);
                if (values != null && values[0] > 0 && values[1] > 0 && values[2] > 0) {
                    result.setSectorTimes(new SectorTimes(values[0], values[1], values[2]));
                }
            });
            sessions.get(sessionId).setResultados(new ArrayList<>(results.values()));
        });
    }

    private void loadEvents(Connection c, Map<String, QualifyingSession> sessions) throws SQLException {
        String sql = "SELECT * FROM evento_sesion ORDER BY sesion_id,evento_sesion_id";
        try (Statement statement = c.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                QualifyingSession session = sessions.get(rs.getString("sesion_id"));
                if (session == null) continue;
                EventOccurrence event = eventFrom(rs);
                session.getEventos().add(event);
                session.getResultados().stream()
                        .filter(result -> result.getPilotoId() == event.pilotoId())
                        .findFirst().ifPresent(result -> result.getEventos().add(event));
            }
        }
    }

    private void loadWeather(Connection c, Map<String, QualifyingSession> sessions) throws SQLException {
        String sql = """
                SELECT w.*,s.total_segmentos FROM clima_snapshot w
                JOIN sesion_clasificacion s ON s.sesion_id=w.sesion_id
                ORDER BY w.sesion_id,w.segmento
                """;
        try (Statement statement = c.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                QualifyingSession session = sessions.get(rs.getString("sesion_id"));
                if (session != null) session.getEvolucionClimatica().add(weatherFrom(rs));
            }
        }
    }

    private void loadTelemetry(Connection c, Map<String, QualifyingSession> sessions) throws SQLException {
        String sql = """
                SELECT t.*,s.total_segmentos,w.estado_clima_codigo,w.temperatura_c,w.humedad_porcentaje,
                       w.probabilidad_lluvia_porcentaje,w.intensidad_lluvia_porcentaje,w.temperatura_pista_c,
                       w.grip_porcentaje,w.traccion_porcentaje,w.frenado_porcentaje,
                       e.tipo_evento_codigo,e.piloto_id evento_piloto_id,e.piloto_nombre_snapshot,
                       e.numero_vuelta,e.sector_codigo evento_sector,e.delta_tiempo_segundos,
                       e.multiplicador_velocidad,e.delta_grip_porcentaje,e.delta_desgaste,
                       e.delta_temperatura_neumaticos_c,e.delta_temperatura_motor_c,
                       e.delta_intensidad_lluvia_porcentaje,e.vuelta_invalidada,e.piloto_fuera,e.bandera_codigo
                  FROM telemetria_snapshot t
                  JOIN sesion_clasificacion s ON s.sesion_id=t.sesion_id
                  JOIN clima_snapshot w ON w.sesion_id=t.sesion_id AND w.segmento=t.segmento
                  LEFT JOIN evento_sesion e ON e.evento_sesion_id=t.evento_sesion_id
                 ORDER BY t.sesion_id,t.segmento
                """;
        try (Statement statement = c.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                QualifyingSession session = sessions.get(rs.getString("sesion_id"));
                if (session == null) continue;
                EventOccurrence event = rs.getString("tipo_evento_codigo") == null
                        ? EventOccurrence.noEvent(session.getConfig().getPilotoId(), driverName(c, session.getConfig().getPilotoId()), 1)
                        : eventFrom(rs, "evento_");
                List<TelemetrySnapshot> samples = new ArrayList<>(session.getEvolucionVuelta());
                samples.add(new TelemetrySnapshot(
                        driverName(c, session.getConfig().getPilotoId()), session.getConfig().getVehiculo(),
                        rs.getInt("segmento"), rs.getInt("total_segmentos"),
                        rs.getDouble("velocidad_kmh"), rs.getDouble("velocidad_maxima_kmh"),
                        rs.getInt("rpm"), rs.getDouble("combustible_restante_porcentaje"),
                        rs.getDouble("desgaste_neumaticos_porcentaje"), rs.getDouble("temperatura_neumaticos_c"),
                        rs.getDouble("temperatura_motor_c"), TrackSector.valueOf(rs.getString("sector_codigo")).ordinal(),
                        rs.getDouble("tiempo_vuelta_segundos"), rs.getDouble("delta_segundos"),
                        weatherFrom(rs), LapStatus.valueOf(rs.getString("estado_vuelta_codigo")), event));
                session.setEvolucionVuelta(samples);
            }
        }
    }

    private void loadTrackEvolution(Connection c, Map<String, QualifyingSession> sessions) throws SQLException {
        try (Statement statement = c.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM evolucion_pista ORDER BY sesion_id,numero_vuelta")) {
            while (rs.next()) {
                QualifyingSession session = sessions.get(rs.getString("sesion_id"));
                if (session != null) {
                    List<TrackEvolutionSnapshot> values = new ArrayList<>(session.getEvolucionPista());
                    values.add(new TrackEvolutionSnapshot(
                        rs.getInt("numero_vuelta"), rs.getString("piloto_nombre_snapshot"),
                        rs.getDouble("grip_inicial_porcentaje"), rs.getDouble("grip_final_porcentaje"),
                        rs.getDouble("goma_inicial_porcentaje"), rs.getDouble("goma_final_porcentaje"),
                        rs.getDouble("lluvia_promedio_porcentaje")));
                    session.setEvolucionPista(values);
                }
            }
        }
    }

    private void loadPitStopsAndChanges(Connection c, Map<String, QualifyingSession> sessions) throws SQLException {
        Map<String, String> stopSessions = new HashMap<>();
        try (Statement statement = c.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM pit_stop ORDER BY sesion_id,segmento_entrada")) {
            while (rs.next()) {
                QualifyingSession session = sessions.get(rs.getString("sesion_id"));
                if (session == null) continue;
                String stopId = rs.getString("pit_stop_id");
                List<PitStopRecord> stops = new ArrayList<>(session.getParadasBoxes());
                stops.add(new PitStopRecord(stopId, rs.getInt("piloto_id"),
                        rs.getString("piloto_nombre_snapshot"), rs.getInt("numero_vuelta"),
                        rs.getInt("segmento_entrada"), rs.getInt("segmento_actual"),
                        PitStopPhase.valueOf(rs.getString("fase_codigo")),
                        PitStopReason.valueOf(rs.getString("motivo_codigo")),
                        rs.getDouble("tiempo_detenido_segundos"), rs.getDouble("tiempo_perdido_segundos"),
                        rs.getInt("posicion_entrada"), rs.getInt("posicion_actual")));
                session.setParadasBoxes(stops);
                stopSessions.put(stopId, session.getId());
            }
        }
        String sql = """
                SELECT c.*,p.piloto_id,p.piloto_nombre_snapshot,p.numero_vuelta,p.motivo_codigo
                  FROM cambio_neumatico c JOIN pit_stop p ON p.pit_stop_id=c.pit_stop_id
                 ORDER BY p.sesion_id,c.segmento
                """;
        try (Statement statement = c.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                QualifyingSession session = sessions.get(stopSessions.get(rs.getString("pit_stop_id")));
                if (session != null) {
                    List<TireChangeRecord> changes = new ArrayList<>(session.getCambiosNeumaticos());
                    changes.add(new TireChangeRecord(
                        rs.getString("pit_stop_id"), rs.getInt("piloto_id"),
                        rs.getString("piloto_nombre_snapshot"), rs.getInt("numero_vuelta"),
                        rs.getInt("segmento"), compound(rs.getString("compuesto_anterior_codigo")),
                        compound(rs.getString("compuesto_nuevo_codigo")),
                        PitStopReason.valueOf(rs.getString("motivo_codigo"))));
                    session.setCambiosNeumaticos(changes);
                }
            }
        }
    }

    private void insertConfig(Connection c, QualifyingSession session) throws SQLException {
        SimulationConfig config = session.getConfig();
        String sql = """
                INSERT INTO configuracion_simulacion(configuracion_id,circuito_id,piloto_id,vehiculo_id,
                    modo_codigo,carga_codigo,presion_codigo,compuesto_inicial_codigo,
                    estrategia_combustible_codigo,duracion_segundos,circuito_nombre_snapshot,
                    piloto_nombre_snapshot,vehiculo_modelo_snapshot,guardado_en)
                SELECT ?,ci.circuito_id,?,v.vehiculo_id,?,?,?,?,?,?,?,?,?,?
                  FROM circuito ci JOIN vehiculo v ON v.modelo=? WHERE ci.nombre=?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, config.getId()); ps.setInt(i++, config.getPilotoId());
            ps.setString(i++, config.getModo().getClave()); ps.setString(i++, config.getAerodinamica().getClave());
            ps.setString(i++, config.getPresion().getClave()); ps.setString(i++, config.getCompuestoInicial().getCodigo());
            ps.setString(i++, config.getCombustible().getClave()); ps.setInt(i++, config.getDuracionSegundos());
            ps.setString(i++, config.getCircuito()); ps.setString(i++, driverName(c, config.getPilotoId()));
            ps.setString(i++, config.getVehiculo()); setTimestamp(ps, i++, config.getGuardadoEn());
            ps.setString(i++, config.getVehiculo()); ps.setString(i, config.getCircuito());
            if (ps.executeUpdate() != 1) throw new DataAccessException("La configuración referencia catálogos inexistentes");
        }
    }

    private void insertSession(Connection c, QualifyingSession session) throws SQLException {
        int total = !session.getEvolucionClimatica().isEmpty()
                ? session.getEvolucionClimatica().get(0).totalSegmentos()
                : Math.max(1, session.getEvolucionVuelta().size());
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO sesion_clasificacion VALUES(?,?,?,?,?)")) {
            ps.setString(1, session.getId()); ps.setString(2, session.getConfig().getId());
            ps.setString(3, session.getClima().getClave()); ps.setInt(4, total);
            setTimestamp(ps, 5, session.getFecha()); ps.executeUpdate();
        }
    }

    private void insertResults(Connection c, QualifyingSession session) throws SQLException {
        String resultSql = """
                INSERT INTO resultado_vuelta
                SELECT ?,?,v.vehiculo_id,?,?,?,?,?,?,?,?,?,?
                  FROM vehiculo v WHERE v.modelo=?
                """;
        try (PreparedStatement result = c.prepareStatement(resultSql);
             PreparedStatement sector = c.prepareStatement(
                     "INSERT INTO tiempo_sector VALUES(?,?,?,?)")) {
            for (LapResult lap : session.getResultados()) {
                int i = 1;
                result.setString(i++, session.getId()); result.setInt(i++, lap.getPilotoId());
                result.setInt(i++, lap.getPosicion()); result.setDouble(i++, lap.getTiempoSegundos());
                result.setDouble(i++, lap.getGap()); result.setDouble(i++, lap.getConsumoEstimado());
                result.setDouble(i++, lap.getDesgasteEstimado()); result.setString(i++, lap.getPiloto());
                result.setString(i++, lap.getEquipo()); result.setString(i++, lap.getVehiculo());
                result.setString(i++, lap.getEstadoVuelta().name()); result.setString(i++, lap.getSectorIncidente().name());
                result.setString(i, lap.getVehiculo()); result.addBatch();
                if (lap.hasSectorTimes()) {
                    for (TrackSector trackSector : List.of(TrackSector.SECTOR_1, TrackSector.SECTOR_2, TrackSector.SECTOR_3)) {
                        sector.setString(1, session.getId()); sector.setInt(2, lap.getPilotoId());
                        sector.setString(3, trackSector.name()); sector.setDouble(4, lap.getSectorTimes().tiempoDe(trackSector));
                        sector.addBatch();
                    }
                }
            }
            result.executeBatch(); sector.executeBatch();
        }
    }

    private Map<EventOccurrence, Long> insertEvents(Connection c, QualifyingSession session) throws SQLException {
        Map<EventOccurrence, Long> ids = new HashMap<>();
        String sql = """
                INSERT INTO evento_sesion(sesion_id,tipo_evento_codigo,piloto_id,piloto_nombre_snapshot,
                    numero_vuelta,sector_codigo,delta_tiempo_segundos,multiplicador_velocidad,
                    delta_grip_porcentaje,delta_desgaste,delta_temperatura_neumaticos_c,
                    delta_temperatura_motor_c,delta_intensidad_lluvia_porcentaje,vuelta_invalidada,
                    piloto_fuera,bandera_codigo) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (EventOccurrence event : session.getEventos()) {
                if (!event.ocurrio()) continue;
                EventImpact impact = event.impacto();
                int i = 1;
                ps.setString(i++, session.getId()); ps.setString(i++, event.tipo().name());
                if (event.pilotoId() > 0) ps.setInt(i++, event.pilotoId()); else ps.setNull(i++, java.sql.Types.INTEGER);
                ps.setString(i++, event.piloto()); ps.setInt(i++, event.vuelta()); ps.setString(i++, event.sector().name());
                ps.setDouble(i++, impact.deltaTiempoSegundos()); ps.setDouble(i++, impact.multiplicadorVelocidad());
                ps.setDouble(i++, impact.deltaGripPorcentaje()); ps.setDouble(i++, impact.deltaDesgaste());
                ps.setDouble(i++, impact.deltaTemperaturaNeumaticosC()); ps.setDouble(i++, impact.deltaTemperaturaMotorC());
                ps.setDouble(i++, impact.deltaIntensidadLluviaPorcentaje()); ps.setBoolean(i++, impact.vueltaInvalidada());
                ps.setBoolean(i++, impact.pilotoFuera()); ps.setString(i, impact.bandera().name()); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) ids.put(event, keys.getLong(1));
                }
            }
        }
        return ids;
    }

    private void insertWeather(Connection c, QualifyingSession session) throws SQLException {
        String sql = "INSERT INTO clima_snapshot VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (WeatherSnapshot weather : session.getEvolucionClimatica()) {
                int i = 1;
                ps.setString(i++, session.getId()); ps.setInt(i++, weather.segmento());
                ps.setString(i++, weather.estado().name()); ps.setDouble(i++, weather.temperaturaC());
                ps.setDouble(i++, weather.humedadPorcentaje()); ps.setDouble(i++, weather.probabilidadLluviaPorcentaje());
                ps.setDouble(i++, weather.intensidadLluviaPorcentaje()); ps.setDouble(i++, weather.temperaturaPistaC());
                ps.setDouble(i++, weather.gripPorcentaje()); ps.setDouble(i++, weather.traccionPorcentaje());
                ps.setDouble(i, weather.frenadoPorcentaje()); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertTelemetry(Connection c, QualifyingSession session,
                                 Map<EventOccurrence, Long> eventIds) throws SQLException {
        String sql = "INSERT INTO telemetria_snapshot VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (TelemetrySnapshot sample : session.getEvolucionVuelta()) {
                int i = 1;
                ps.setString(i++, session.getId()); ps.setInt(i++, sample.segmento());
                ps.setDouble(i++, sample.velocidadKmh()); ps.setDouble(i++, sample.velocidadMaximaKmh());
                ps.setInt(i++, sample.rpm()); ps.setDouble(i++, sample.combustibleRestantePorcentaje());
                ps.setDouble(i++, sample.desgasteNeumaticosPorcentaje());
                ps.setDouble(i++, sample.temperaturaNeumaticosC()); ps.setDouble(i++, sample.temperaturaMotorC());
                ps.setString(i++, TrackSector.values()[sample.sectorActual()].name());
                ps.setDouble(i++, sample.tiempoVueltaSegundos()); ps.setDouble(i++, sample.deltaSegundos());
                ps.setString(i++, sample.estadoVuelta().name());
                Long eventId = eventIds.get(sample.evento());
                if (eventId == null) ps.setNull(i, java.sql.Types.BIGINT); else ps.setLong(i, eventId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertTrackEvolution(Connection c, QualifyingSession session) throws SQLException {
        String sql = """
                INSERT INTO evolucion_pista
                SELECT ?,?,?,?, ?,?,?,?,? FROM piloto p WHERE p.nombre=?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (TrackEvolutionSnapshot value : session.getEvolucionPista()) {
                int driverId = driverId(c, value.piloto());
                int i = 1;
                ps.setString(i++, session.getId()); ps.setInt(i++, value.vuelta()); ps.setInt(i++, driverId);
                ps.setString(i++, value.piloto()); ps.setDouble(i++, value.gripInicialPorcentaje());
                ps.setDouble(i++, value.gripFinalPorcentaje()); ps.setDouble(i++, value.gomaInicialPorcentaje());
                ps.setDouble(i++, value.gomaFinalPorcentaje()); ps.setDouble(i++, value.lluviaPromedioPorcentaje());
                ps.setString(i, value.piloto()); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertPitStops(Connection c, QualifyingSession session) throws SQLException {
        String sql = "INSERT INTO pit_stop VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (PitStopRecord stop : session.getParadasBoxes()) {
                int i = 1;
                ps.setString(i++, stop.id()); ps.setString(i++, session.getId()); ps.setInt(i++, stop.pilotoId());
                ps.setString(i++, stop.piloto()); ps.setInt(i++, stop.vuelta()); ps.setInt(i++, stop.segmentoEntrada());
                ps.setInt(i++, stop.segmentoActual()); ps.setString(i++, stop.fase().name());
                ps.setString(i++, stop.motivo().name()); ps.setDouble(i++, stop.tiempoDetenidoSegundos());
                ps.setDouble(i++, stop.tiempoPerdidoSegundos()); ps.setInt(i++, stop.posicionEntrada());
                ps.setInt(i, stop.posicionActual()); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertTireChanges(Connection c, QualifyingSession session) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO cambio_neumatico VALUES(?,?,?,?)")) {
            for (TireChangeRecord change : session.getCambiosNeumaticos()) {
                ps.setString(1, change.pitStopId()); ps.setInt(2, change.segmento());
                ps.setString(3, change.anterior().getCodigo()); ps.setString(4, change.nuevo().getCodigo());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteExisting(Connection c, String sessionId) throws SQLException {
        String configId = null;
        try (PreparedStatement find = c.prepareStatement(
                "SELECT configuracion_id FROM sesion_clasificacion WHERE sesion_id=?")) {
            find.setString(1, sessionId);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) configId = rs.getString(1);
            }
        }
        if (configId == null) return;
        try (PreparedStatement deleteSession = c.prepareStatement(
                     "DELETE FROM sesion_clasificacion WHERE sesion_id=?");
             PreparedStatement deleteConfig = c.prepareStatement(
                     "DELETE FROM configuracion_simulacion WHERE configuracion_id=?")) {
            deleteSession.setString(1, sessionId); deleteSession.executeUpdate();
            deleteConfig.setString(1, configId); deleteConfig.executeUpdate();
        }
    }

    private WeatherSnapshot weatherFrom(ResultSet rs) throws SQLException {
        return new WeatherSnapshot(rs.getInt("segmento"), rs.getInt("total_segmentos"),
                DynamicWeatherState.valueOf(rs.getString("estado_clima_codigo")),
                rs.getDouble("temperatura_c"), rs.getDouble("humedad_porcentaje"),
                rs.getDouble("probabilidad_lluvia_porcentaje"), rs.getDouble("intensidad_lluvia_porcentaje"),
                rs.getDouble("temperatura_pista_c"), rs.getDouble("grip_porcentaje"),
                rs.getDouble("traccion_porcentaje"), rs.getDouble("frenado_porcentaje"));
    }

    private EventOccurrence eventFrom(ResultSet rs) throws SQLException {
        return eventFrom(rs, "");
    }

    private EventOccurrence eventFrom(ResultSet rs, String aliasPrefix) throws SQLException {
        String pilotIdColumn = aliasPrefix.isEmpty() ? "piloto_id" : aliasPrefix + "piloto_id";
        String pilotNameColumn = aliasPrefix.isEmpty() ? "piloto_nombre_snapshot" : "piloto_nombre_snapshot";
        String sectorColumn = aliasPrefix.isEmpty() ? "sector_codigo" : aliasPrefix + "sector";
        int pilotId = rs.getInt(pilotIdColumn);
        EventImpact impact = new EventImpact(rs.getDouble("delta_tiempo_segundos"),
                rs.getDouble("multiplicador_velocidad"), rs.getDouble("delta_grip_porcentaje"),
                rs.getDouble("delta_desgaste"), rs.getDouble("delta_temperatura_neumaticos_c"),
                rs.getDouble("delta_temperatura_motor_c"), rs.getDouble("delta_intensidad_lluvia_porcentaje"),
                rs.getBoolean("vuelta_invalidada"), rs.getBoolean("piloto_fuera"),
                TrackFlag.valueOf(rs.getString("bandera_codigo")));
        return new EventOccurrence(EventType.valueOf(rs.getString("tipo_evento_codigo")), pilotId,
                rs.getString(pilotNameColumn), rs.getInt("numero_vuelta"),
                TrackSector.valueOf(rs.getString(sectorColumn)), impact);
    }

    private String driverName(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT nombre FROM piloto WHERE piloto_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        throw new DataAccessException("No existe el piloto " + id);
    }

    private int driverId(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT piloto_id FROM piloto WHERE nombre=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new DataAccessException("No existe el piloto " + name);
    }

    private static TireCompound compound(String code) {
        for (TireCompound value : TireCompound.values()) {
            if (value.getCodigo().equalsIgnoreCase(code)) return value;
        }
        throw new DataAccessException("Compuesto desconocido: " + code);
    }

    private static void setTimestamp(PreparedStatement ps, int index, String text) throws SQLException {
        if (text == null || text.isBlank()) ps.setNull(index, java.sql.Types.TIMESTAMP);
        else ps.setTimestamp(index, Timestamp.valueOf(LocalDateTime.parse(text, DATE_FORMAT)));
    }
}
