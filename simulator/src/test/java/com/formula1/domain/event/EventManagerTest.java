package com.formula1.domain.event;

import com.formula1.data.DataStore;
import com.formula1.domain.model.AerodynamicLoad;
import com.formula1.domain.model.Driver;
import com.formula1.domain.model.DrivingMode;
import com.formula1.domain.model.DynamicWeatherState;
import com.formula1.domain.model.EventCategory;
import com.formula1.domain.model.EventOccurrence;
import com.formula1.domain.model.EventProbabilityConfig;
import com.formula1.domain.model.EventScope;
import com.formula1.domain.model.EventType;
import com.formula1.domain.model.FuelStrategy;
import com.formula1.domain.model.SimulationConfig;
import com.formula1.domain.model.TirePressure;
import com.formula1.domain.model.TrackSector;
import com.formula1.domain.model.Vehicle;
import com.formula1.domain.model.WeatherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventManagerTest {

    private Driver driver;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        DataStore data = DataStore.enMemoria();
        driver = data.pilotos().get(1);
        vehicle = data.vehiculos().get("RB20");
    }

    @Test
    void catalogContainsEveryRequiredEventAndNoDuplicates() {
        List<SimulationEvent> catalog = EventCatalog.defaultEvents();
        Set<EventType> actual = EnumSet.noneOf(EventType.class);
        catalog.forEach(event -> actual.add(event.type()));

        Set<EventType> expected = EnumSet.allOf(EventType.class);
        expected.remove(EventType.NO_EVENT);
        assertEquals(28, catalog.size());
        assertEquals(expected, actual);
    }

    @Test
    void standardDistributionKeepsNoEventAsTheNormalOutcome() {
        EventManager manager = new EventManager(12_345L);
        int noEvent = 0;
        int crashes = 0;
        int resolutions = 30_000;

        for (int sequence = 1; sequence <= resolutions; sequence++) {
            List<EventOccurrence> occurrences = manager.resolve(context(
                    sequence, driver, DrivingMode.NORMAL, dryWeather(), 82, 98, 18, 2));
            assertTrue(occurrences.size() <= 2);
            assertTrue(occurrences.stream().filter(EventOccurrence::ocurrio)
                    .filter(event -> event.alcance() == EventScope.INDIVIDUAL).count() <= 1);
            assertTrue(occurrences.stream().filter(EventOccurrence::ocurrio)
                    .filter(event -> event.alcance() == EventScope.GLOBAL).count() <= 1);
            if (!occurrences.get(0).ocurrio()) {
                noEvent++;
            }
            crashes += occurrences.stream().anyMatch(event -> event.tipo() == EventType.CRASH) ? 1 : 0;
        }

        double noEventRate = noEvent / (double) resolutions;
        double crashRate = crashes / (double) resolutions;
        assertTrue(noEventRate > 0.65 && noEventRate < 0.85,
                "Tasa sin evento fuera del rango realista: " + noEventRate);
        assertTrue(crashRate < 0.01,
                "El accidente debe seguir siendo excepcional: " + crashRate);
    }

    @Test
    void sameSeedProducesTheSameEventSequenceAndImpact() {
        EventManager first = new EventManager(91_827L);
        EventManager second = new EventManager(91_827L);

        for (int sequence = 1; sequence <= 250; sequence++) {
            EventContext context = context(sequence, driver, DrivingMode.AGRESIVA,
                    wetWeather(), 94, 109, 45, 4);
            assertEquals(first.resolve(context), second.resolve(context));
        }
    }

    @Test
    void cooldownPreventsImmediateRepetitionForTheSameDriver() {
        SimulationEvent perfectLap = find(EventType.PERFECT_LAP);
        EventProbabilityConfig onlyPositive = new EventProbabilityConfig(
                0, 1, 0, 0, 0, 0, 0);
        EventManager manager = new EventManager(onlyPositive, new Random(8), List.of(perfectLap));

        assertEquals(EventType.PERFECT_LAP, manager.resolve(context(
                1, driver, DrivingMode.NORMAL, dryWeather(), 82, 98, 10, 0)).get(0).tipo());
        assertEquals(EventType.NO_EVENT, manager.resolve(context(
                2, driver, DrivingMode.NORMAL, dryWeather(), 82, 98, 10, 0)).get(0).tipo());
        assertEquals(EventType.NO_EVENT, manager.resolve(context(
                5, driver, DrivingMode.NORMAL, dryWeather(), 82, 98, 10, 0)).get(0).tipo());
        assertEquals(EventType.PERFECT_LAP, manager.resolve(context(
                6, driver, DrivingMode.NORMAL, dryWeather(), 82, 98, 10, 0)).get(0).tipo());
    }

    @Test
    void compatibilityUsesTrafficTemperatureRainAndGrip() {
        EventContext dry = context(1, driver, DrivingMode.NORMAL,
                dryWeather(), 82, 98, 15, 0).conSector(TrackSector.SECTOR_2);
        EventContext wet = context(1, driver, DrivingMode.NORMAL,
                wetWeather(), 65, 108, 70, 4).conSector(TrackSector.SECTOR_2);
        EventContext drying = context(1, driver, DrivingMode.NORMAL,
                dryingWeather(), 72, 100, 25, 1).conSector(TrackSector.SECTOR_2);

        assertFalse(find(EventType.HEAVY_TRAFFIC).isCompatible(dry));
        assertFalse(find(EventType.TYRE_OVERHEATING).isCompatible(dry));
        assertFalse(find(EventType.RAIN_INTENSIFIES).isCompatible(dry));
        assertFalse(find(EventType.WHEELSPIN).isCompatible(dry));

        assertTrue(find(EventType.HEAVY_TRAFFIC).isCompatible(wet));
        assertTrue(find(EventType.TYRE_TOO_COLD).isCompatible(wet));
        assertTrue(find(EventType.RAIN_INTENSIFIES).isCompatible(wet));
        assertTrue(find(EventType.WHEELSPIN).isCompatible(wet));
        assertTrue(find(EventType.TRACK_DRYING).isCompatible(drying));
    }

    @Test
    void attackAndLowConsistencyIncreaseTheErrorRate() {
        Driver lowSkill = copyDriver(41);
        Driver highSkill = copyDriver(98);
        EventManager attack = new EventManager(4_321L);
        EventManager conserve = new EventManager(4_321L);
        int attackErrors = 0;
        int conserveErrors = 0;

        for (int sequence = 1; sequence <= 12_000; sequence++) {
            attackErrors += countErrors(attack.resolve(context(sequence, lowSkill,
                    DrivingMode.AGRESIVA, wetWeather(), 98, 112, 78, 3)));
            conserveErrors += countErrors(conserve.resolve(context(sequence, highSkill,
                    DrivingMode.AHORRO, wetWeather(), 75, 96, 25, 3)));
        }

        assertTrue(attackErrors > conserveErrors * 1.25,
                "ATTACK y baja consistencia deben elevar el riesgo: "
                        + attackErrors + " frente a " + conserveErrors);
    }

    @Test
    void weightedSelectorRespectsRelativeWeights() {
        WeightedEventSelector selector = new WeightedEventSelector();
        Random random = new Random(55);
        int heavy = 0;
        for (int i = 0; i < 20_000; i++) {
            if (selector.select(List.of("light", "heavy"),
                    value -> value.equals("heavy") ? 9 : 1, random).orElseThrow().equals("heavy")) {
                heavy++;
            }
        }
        assertTrue(heavy > 17_500 && heavy < 18_500, "Selección 9:1 inesperada: " + heavy);
    }

    private int countErrors(List<EventOccurrence> occurrences) {
        return (int) occurrences.stream()
                .filter(EventOccurrence::ocurrio)
                .filter(event -> event.categoria() == EventCategory.MINOR_NEGATIVE
                        || event.categoria() == EventCategory.MAJOR_NEGATIVE
                        || event.tipo() == EventType.CRASH)
                .count();
    }

    private Driver copyDriver(int skill) {
        Driver copy = new Driver(driver.getId(), driver.getNombre(), driver.getEquipo(),
                driver.getRol(), driver.getExperiencia());
        copy.setHabilidad(Driver.HABILIDAD_VELOCIDAD, skill);
        copy.setHabilidad(Driver.HABILIDAD_CONSISTENCIA, skill);
        copy.setHabilidad(Driver.HABILIDAD_LLUVIA, skill);
        return copy;
    }

    private EventContext context(int sequence, Driver currentDriver, DrivingMode mode,
                                 List<WeatherSnapshot> weather, double tyreTemperature,
                                 double engineTemperature, double wear, int traffic) {
        SimulationConfig config = new SimulationConfig("Circuito de Monza", currentDriver.getId(),
                vehicle.getModelo(), mode, AerodynamicLoad.MEDIA, TirePressure.ESTANDAR,
                mode == DrivingMode.AGRESIVA ? FuelStrategy.AGRESIVA
                        : mode == DrivingMode.AHORRO ? FuelStrategy.AHORRO : FuelStrategy.BALANCEADA);
        return new EventContext(sequence, 1, currentDriver, vehicle, config, weather,
                tyreTemperature, engineTemperature, wear, traffic, 0, TrackSector.NONE);
    }

    private List<WeatherSnapshot> dryWeather() {
        return weather(DynamicWeatherState.SECO, 55, 0, 96);
    }

    private List<WeatherSnapshot> wetWeather() {
        return weather(DynamicWeatherState.LLUVIA, 95, 48, 58);
    }

    private List<WeatherSnapshot> dryingWeather() {
        return weather(DynamicWeatherState.LLUVIA_LIGERA, 65, 20, 72);
    }

    private List<WeatherSnapshot> weather(DynamicWeatherState state, double rainProbability,
                                           double rainIntensity, double grip) {
        return java.util.stream.IntStream.rangeClosed(1, 20)
                .mapToObj(segment -> new WeatherSnapshot(segment, 20, state,
                        22, 70, rainProbability, rainIntensity, 27,
                        grip, grip - 2, grip - 3))
                .toList();
    }

    private SimulationEvent find(EventType type) {
        return EventCatalog.defaultEvents().stream()
                .filter(event -> event.type() == type)
                .findFirst()
                .orElseThrow();
    }
}
