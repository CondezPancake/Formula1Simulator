package com.formula1.data;

import com.formula1.domain.model.Circuit;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DriverRole;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.Team;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherCondition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mapea los catálogos normalizados de MySQL al modelo de dominio. */
final class MySqlCatalogRepository {

    CatalogPersistencePort.CatalogData load(Connection connection) throws SQLException {
        return new CatalogPersistencePort.CatalogData(
                loadDrivers(connection), loadTeams(connection),
                loadVehicles(connection), loadCircuits(connection));
    }

    private List<Team> loadTeams(Connection connection) throws SQLException {
        String sql = """
                SELECT e.nombre, p.nombre pais, m.nombre motor, e.imagen_url,
                       e.nombre_completo, e.base, e.jefe_equipo, e.jefe_tecnico,
                       e.piloto_reserva, e.primera_participacion, e.campeonatos,
                       e.gran_premios, e.victorias, e.podios, e.poles, e.descripcion
                  FROM equipo e
                  JOIN pais p ON p.pais_id=e.pais_id
                  JOIN fabricante_motor m ON m.fabricante_motor_id=e.fabricante_motor_id
                 ORDER BY e.equipo_id
                """;
        Map<String, Team> teams = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Team team = new Team(rs.getString("nombre"), rs.getString("pais"), rs.getString("motor"));
                team.setImagen(rs.getString("imagen_url"));
                team.setNombreCompleto(rs.getString("nombre_completo"));
                team.setBase(rs.getString("base"));
                team.setJefeEquipo(rs.getString("jefe_equipo"));
                team.setJefeTecnico(rs.getString("jefe_tecnico"));
                team.setPilotoReserva(rs.getString("piloto_reserva"));
                team.setPrimeraParticipacion(rs.getInt("primera_participacion"));
                team.setCampeonatos(rs.getInt("campeonatos"));
                team.setGranPremios(rs.getInt("gran_premios"));
                team.setVictorias(rs.getInt("victorias"));
                team.setPodios(rs.getInt("podios"));
                team.setPoles(rs.getInt("poles"));
                team.setDescripcion(rs.getString("descripcion"));
                teams.put(team.getNombre(), team);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT e.nombre, p.piloto_id FROM piloto p JOIN equipo e ON e.equipo_id=p.equipo_id ORDER BY p.piloto_id")) {
            while (rs.next()) {
                Team team = teams.get(rs.getString(1));
                if (team != null) team.getPilotos().add(rs.getInt(2));
            }
        }
        return new ArrayList<>(teams.values());
    }

    private List<Driver> loadDrivers(Connection connection) throws SQLException {
        String sql = """
                SELECT p.piloto_id, p.nombre, e.nombre equipo, p.rol_codigo,
                       p.experiencia_anios, p.imagen_url, p.numero, p.codigo_tv,
                       n.nombre nacionalidad, p.victorias, p.campeonatos,
                       p.fecha_nacimiento, p.lugar_nacimiento, p.biografia
                  FROM piloto p
                  JOIN equipo e ON e.equipo_id=p.equipo_id
                  JOIN nacionalidad n ON n.nacionalidad_id=p.nacionalidad_id
                 ORDER BY p.piloto_id
                """;
        Map<Integer, Driver> drivers = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Driver driver = new Driver(rs.getInt("piloto_id"), rs.getString("nombre"),
                        rs.getString("equipo"), DriverRole.valueOf(rs.getString("rol_codigo")),
                        rs.getInt("experiencia_anios"));
                driver.setImagen(rs.getString("imagen_url"));
                driver.setNumero(rs.getInt("numero"));
                driver.setCodigo(rs.getString("codigo_tv"));
                driver.setNacionalidad(rs.getString("nacionalidad"));
                driver.setVictorias(rs.getInt("victorias"));
                driver.setCampeonatos(rs.getInt("campeonatos"));
                driver.setFechaNacimiento(rs.getString("fecha_nacimiento"));
                driver.setLugarNacimiento(rs.getString("lugar_nacimiento"));
                driver.setBiografia(rs.getString("biografia"));
                drivers.put(driver.getId(), driver);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT piloto_id, habilidad_codigo, valor FROM piloto_habilidad ORDER BY piloto_id")) {
            while (rs.next()) {
                Driver driver = drivers.get(rs.getInt(1));
                if (driver != null) driver.setHabilidad(rs.getString(2), rs.getInt(3));
            }
        }
        return new ArrayList<>(drivers.values());
    }

    private List<Vehicle> loadVehicles(Connection connection) throws SQLException {
        String sql = """
                SELECT v.vehiculo_id, v.modelo, e.nombre equipo, m.nombre motor,
                       v.velocidad_maxima_kmh, v.aceleracion_0_100, v.imagen_url
                  FROM vehiculo v
                  JOIN equipo e ON e.equipo_id=v.equipo_id
                  JOIN fabricante_motor m ON m.fabricante_motor_id=v.fabricante_motor_id
                 ORDER BY v.vehiculo_id
                """;
        Map<Integer, Vehicle> vehicles = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Vehicle vehicle = new Vehicle(rs.getString("modelo"), rs.getString("equipo"),
                        rs.getString("motor"), rs.getInt("velocidad_maxima_kmh"),
                        rs.getDouble("aceleracion_0_100"));
                vehicle.setImagen(rs.getString("imagen_url"));
                vehicles.put(rs.getInt("vehiculo_id"), vehicle);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT vehiculo_id, piloto_id FROM vehiculo_piloto ORDER BY vehiculo_id, piloto_id")) {
            while (rs.next()) vehicles.get(rs.getInt(1)).getPilotos().add(rs.getInt(2));
        }
        String performanceSql = """
                SELECT r.vehiculo_id, r.modo_codigo, r.velocidad_promedio_kmh,
                       c.condicion_codigo, c.consumo_combustible, c.desgaste_neumaticos
                  FROM vehiculo_rendimiento r
                  JOIN vehiculo_rendimiento_clima c
                    ON c.vehiculo_id=r.vehiculo_id AND c.modo_codigo=r.modo_codigo
                 ORDER BY r.vehiculo_id, r.modo_codigo
                """;
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(performanceSql)) {
            while (rs.next()) {
                Vehicle vehicle = vehicles.get(rs.getInt("vehiculo_id"));
                DrivingMode mode = DrivingMode.desdeClave(rs.getString("modo_codigo"));
                Vehicle.Performance performance = vehicle.getRendimiento().computeIfAbsent(mode, ignored -> {
                    Vehicle.Performance value = new Vehicle.Performance();
                    value.setVelocidadPromedioKmh(rsIntUnchecked(rs, "velocidad_promedio_kmh"));
                    return value;
                });
                WeatherCondition weather = WeatherCondition.desdeClave(rs.getString("condicion_codigo"));
                performance.getConsumo().put(weather, rs.getDouble("consumo_combustible"));
                performance.getDesgaste().put(weather, rs.getDouble("desgaste_neumaticos"));
            }
        }
        return new ArrayList<>(vehicles.values());
    }

    private static int rsIntUnchecked(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo leer el rendimiento del vehículo", e);
        }
    }

    private List<Circuit> loadCircuits(Connection connection) throws SQLException {
        String sql = """
                SELECT c.circuito_id, c.nombre, p.nombre pais, c.longitud_km, c.vueltas,
                       c.descripcion, c.factor_tecnico, c.factor_consumo,
                       c.factor_desgaste, c.imagen_url, r.tiempo_segundos,
                       r.titular_nombre, r.anio
                  FROM circuito c
                  JOIN pais p ON p.pais_id=c.pais_id
                  LEFT JOIN circuito_record_vuelta r ON r.circuito_id=c.circuito_id
                 ORDER BY c.circuito_id
                """;
        Map<Integer, Circuit> circuits = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Circuit circuit = new Circuit(rs.getString("nombre"), rs.getString("pais"),
                        rs.getDouble("longitud_km"), rs.getInt("vueltas"));
                circuit.setDescripcion(rs.getString("descripcion"));
                circuit.setFactorTecnico(rs.getDouble("factor_tecnico"));
                circuit.setFactorConsumo(rs.getDouble("factor_consumo"));
                circuit.setFactorDesgaste(rs.getDouble("factor_desgaste"));
                circuit.setImagen(rs.getString("imagen_url"));
                if (rs.getObject("tiempo_segundos") != null) {
                    circuit.setRecordVuelta(new Circuit.LapRecord(rs.getDouble("tiempo_segundos"),
                            rs.getString("titular_nombre"), rs.getInt("anio")));
                }
                circuits.put(rs.getInt("circuito_id"), circuit);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT circuito_id, condicion_codigo, probabilidad FROM circuito_probabilidad_clima ORDER BY circuito_id")) {
            while (rs.next()) circuits.get(rs.getInt(1)).getProbabilidadClima().put(
                    WeatherCondition.desdeClave(rs.getString(2)), rs.getDouble(3));
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT circuito_id, temporada, piloto_id FROM circuito_ganador ORDER BY circuito_id, temporada")) {
            while (rs.next()) circuits.get(rs.getInt(1)).getGanadores().add(
                    new Circuit.Winner(rs.getInt(2), rs.getInt(3)));
        }
        return new ArrayList<>(circuits.values());
    }

    void saveTeam(Connection c, Team team) throws SQLException {
        int countryId = lookupId(c, "pais", "pais_id", team.getPais());
        int engineId = lookupId(c, "fabricante_motor", "fabricante_motor_id", team.getMotor());
        String sql = """
                INSERT INTO equipo(nombre,pais_id,fabricante_motor_id,imagen_url,nombre_completo,base,
                    jefe_equipo,jefe_tecnico,piloto_reserva,primera_participacion,campeonatos,
                    gran_premios,victorias,podios,poles,descripcion)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) AS nuevo
                ON DUPLICATE KEY UPDATE pais_id=nuevo.pais_id,fabricante_motor_id=nuevo.fabricante_motor_id,
                    imagen_url=nuevo.imagen_url,nombre_completo=nuevo.nombre_completo,base=nuevo.base,
                    jefe_equipo=nuevo.jefe_equipo,jefe_tecnico=nuevo.jefe_tecnico,
                    piloto_reserva=nuevo.piloto_reserva,primera_participacion=nuevo.primera_participacion,
                    campeonatos=nuevo.campeonatos,gran_premios=nuevo.gran_premios,
                    victorias=nuevo.victorias,podios=nuevo.podios,poles=nuevo.poles,descripcion=nuevo.descripcion
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, team.getNombre()); ps.setInt(i++, countryId); ps.setInt(i++, engineId);
            ps.setString(i++, team.getImagen()); ps.setString(i++, team.getNombreCompleto());
            ps.setString(i++, team.getBase()); ps.setString(i++, team.getJefeEquipo());
            ps.setString(i++, team.getJefeTecnico()); ps.setString(i++, team.getPilotoReserva());
            ps.setInt(i++, team.getPrimeraParticipacion()); ps.setInt(i++, team.getCampeonatos());
            ps.setInt(i++, team.getGranPremios()); ps.setInt(i++, team.getVictorias());
            ps.setInt(i++, team.getPodios()); ps.setInt(i++, team.getPoles());
            ps.setString(i, team.getDescripcion()); ps.executeUpdate();
        }
    }

    void saveDriver(Connection c, Driver driver) throws SQLException {
        int teamId = existingId(c, "equipo", "equipo_id", "nombre", driver.getEquipo());
        int nationalityId = lookupId(c, "nacionalidad", "nacionalidad_id", driver.getNacionalidad());
        String sql = """
                INSERT INTO piloto(piloto_id,equipo_id,rol_codigo,nacionalidad_id,nombre,
                    experiencia_anios,numero,codigo_tv,victorias,campeonatos,imagen_url,
                    fecha_nacimiento,lugar_nacimiento,biografia)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) AS nuevo
                ON DUPLICATE KEY UPDATE equipo_id=nuevo.equipo_id,rol_codigo=nuevo.rol_codigo,
                    nacionalidad_id=nuevo.nacionalidad_id,nombre=nuevo.nombre,
                    experiencia_anios=nuevo.experiencia_anios,numero=nuevo.numero,
                    codigo_tv=nuevo.codigo_tv,victorias=nuevo.victorias,
                    campeonatos=nuevo.campeonatos,imagen_url=nuevo.imagen_url,
                    fecha_nacimiento=nuevo.fecha_nacimiento,lugar_nacimiento=nuevo.lugar_nacimiento,
                    biografia=nuevo.biografia
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, driver.getId()); ps.setInt(i++, teamId); ps.setString(i++, driver.getRol().name());
            ps.setInt(i++, nationalityId); ps.setString(i++, driver.getNombre());
            ps.setInt(i++, driver.getExperiencia()); ps.setInt(i++, driver.getNumero());
            ps.setString(i++, driver.getCodigo()); ps.setInt(i++, driver.getVictorias());
            ps.setInt(i++, driver.getCampeonatos()); ps.setString(i++, driver.getImagen());
            ps.setString(i++, driver.getFechaNacimiento()); ps.setString(i++, driver.getLugarNacimiento());
            ps.setString(i, driver.getBiografia()); ps.executeUpdate();
        }
        try (PreparedStatement delete = c.prepareStatement("DELETE FROM piloto_habilidad WHERE piloto_id=?")) {
            delete.setInt(1, driver.getId()); delete.executeUpdate();
        }
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO piloto_habilidad(piloto_id,habilidad_codigo,valor) VALUES(?,?,?)")) {
            for (Map.Entry<String, Integer> skill : driver.getHabilidades().entrySet()) {
                insert.setInt(1, driver.getId()); insert.setString(2, skill.getKey());
                insert.setInt(3, skill.getValue()); insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    void saveVehicle(Connection c, Vehicle vehicle) throws SQLException {
        int teamId = existingId(c, "equipo", "equipo_id", "nombre", vehicle.getEquipo());
        int engineId = lookupId(c, "fabricante_motor", "fabricante_motor_id", vehicle.getMotor());
        String sql = """
                INSERT INTO vehiculo(equipo_id,fabricante_motor_id,modelo,velocidad_maxima_kmh,aceleracion_0_100,imagen_url)
                VALUES(?,?,?,?,?,?) AS nuevo ON DUPLICATE KEY UPDATE equipo_id=nuevo.equipo_id,
                    fabricante_motor_id=nuevo.fabricante_motor_id,velocidad_maxima_kmh=nuevo.velocidad_maxima_kmh,
                    aceleracion_0_100=nuevo.aceleracion_0_100,imagen_url=nuevo.imagen_url
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId); ps.setInt(2, engineId); ps.setString(3, vehicle.getModelo());
            ps.setInt(4, vehicle.getVelocidadMaximaKmh()); ps.setDouble(5, vehicle.getAceleracion0100());
            ps.setString(6, vehicle.getImagen()); ps.executeUpdate();
        }
        int vehicleId = existingId(c, "vehiculo", "vehiculo_id", "modelo", vehicle.getModelo());
        deleteChildren(c, vehicleId, "vehiculo_piloto", "vehiculo_id");
        deleteChildren(c, vehicleId, "vehiculo_rendimiento", "vehiculo_id");
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO vehiculo_piloto(vehiculo_id,piloto_id) VALUES(?,?)")) {
            for (Integer driverId : vehicle.getPilotos()) {
                ps.setInt(1, vehicleId); ps.setInt(2, driverId); ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement base = c.prepareStatement(
                     "INSERT INTO vehiculo_rendimiento(vehiculo_id,modo_codigo,velocidad_promedio_kmh) VALUES(?,?,?)");
             PreparedStatement weather = c.prepareStatement("""
                     INSERT INTO vehiculo_rendimiento_clima(vehiculo_id,modo_codigo,condicion_codigo,
                         consumo_combustible,desgaste_neumaticos) VALUES(?,?,?,?,?)
                     """)) {
            for (Map.Entry<DrivingMode, Vehicle.Performance> entry : vehicle.getRendimiento().entrySet()) {
                base.setInt(1, vehicleId); base.setString(2, entry.getKey().getClave());
                base.setInt(3, entry.getValue().getVelocidadPromedioKmh()); base.addBatch();
                for (WeatherCondition condition : WeatherCondition.values()) {
                    weather.setInt(1, vehicleId); weather.setString(2, entry.getKey().getClave());
                    weather.setString(3, condition.getClave());
                    weather.setDouble(4, entry.getValue().consumoCon(condition));
                    weather.setDouble(5, entry.getValue().desgasteCon(condition)); weather.addBatch();
                }
            }
            base.executeBatch(); weather.executeBatch();
        }
    }

    void saveCircuit(Connection c, Circuit circuit) throws SQLException {
        int countryId = lookupId(c, "pais", "pais_id", circuit.getPais());
        String sql = """
                INSERT INTO circuito(pais_id,nombre,longitud_km,vueltas,descripcion,factor_tecnico,
                    factor_consumo,factor_desgaste,imagen_url) VALUES(?,?,?,?,?,?,?,?,?) AS nuevo
                ON DUPLICATE KEY UPDATE pais_id=nuevo.pais_id,longitud_km=nuevo.longitud_km,
                    vueltas=nuevo.vueltas,descripcion=nuevo.descripcion,factor_tecnico=nuevo.factor_tecnico,
                    factor_consumo=nuevo.factor_consumo,factor_desgaste=nuevo.factor_desgaste,imagen_url=nuevo.imagen_url
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, countryId); ps.setString(2, circuit.getNombre());
            ps.setDouble(3, circuit.getLongitudKm()); ps.setInt(4, circuit.getVueltas());
            ps.setString(5, circuit.getDescripcion()); ps.setDouble(6, circuit.getFactorTecnico());
            ps.setDouble(7, circuit.getFactorConsumo()); ps.setDouble(8, circuit.getFactorDesgaste());
            ps.setString(9, circuit.getImagen()); ps.executeUpdate();
        }
        int circuitId = existingId(c, "circuito", "circuito_id", "nombre", circuit.getNombre());
        deleteChildren(c, circuitId, "circuito_record_vuelta", "circuito_id");
        deleteChildren(c, circuitId, "circuito_probabilidad_clima", "circuito_id");
        deleteChildren(c, circuitId, "circuito_ganador", "circuito_id");
        if (circuit.getRecordVuelta() != null) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO circuito_record_vuelta VALUES(?,?,?,?)")) {
                ps.setInt(1, circuitId); ps.setDouble(2, circuit.getRecordVuelta().getTiempoSegundos());
                ps.setString(3, circuit.getRecordVuelta().getPiloto());
                ps.setInt(4, circuit.getRecordVuelta().getAnio()); ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO circuito_probabilidad_clima VALUES(?,?,?)")) {
            for (Map.Entry<WeatherCondition, Double> entry : circuit.getProbabilidadClima().entrySet()) {
                ps.setInt(1, circuitId); ps.setString(2, entry.getKey().getClave());
                ps.setDouble(3, entry.getValue()); ps.addBatch();
            }
            ps.executeBatch();
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO circuito_ganador VALUES(?,?,?)")) {
            for (Circuit.Winner winner : circuit.getGanadores()) {
                ps.setInt(1, circuitId); ps.setInt(2, winner.getTemporada());
                ps.setInt(3, winner.getPilotoId()); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    void deleteByNaturalKey(Connection c, String table, String keyColumn, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE " + keyColumn + "=?")) {
            ps.setString(1, value); ps.executeUpdate();
        }
    }

    private int lookupId(Connection c, String table, String idColumn, String value) throws SQLException {
        try {
            return existingId(c, table, idColumn, "nombre", value);
        } catch (DataAccessException missing) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO " + table + "(nombre) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, value); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
            throw missing;
        }
    }

    private int existingId(Connection c, String table, String idColumn,
                           String keyColumn, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + idColumn + " FROM " + table + " WHERE " + keyColumn + "=?")) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new DataAccessException("No existe " + table + " para " + value);
    }

    private void deleteChildren(Connection c, int id, String table, String column) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE " + column + "=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }
}
