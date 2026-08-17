package com.formula1.event;

import com.formula1.model.EventCategory;
import com.formula1.model.EventOccurrence;
import com.formula1.model.EventProbabilityConfig;
import com.formula1.model.EventScope;
import com.formula1.model.EventType;
import com.formula1.model.TrackSector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Coordina la decisión en dos etapas, compatibilidad, cooldown y límite por vuelta.
 * Una resolución produce como máximo un evento individual y uno global.
 */
public final class EventManager {

    private static final Set<EventCategory> ERROR_CATEGORIES = EnumSet.of(
            EventCategory.MINOR_NEGATIVE,
            EventCategory.MAJOR_NEGATIVE,
            EventCategory.EXCEPTIONAL);

    private final EventProbabilityConfig probabilities;
    private final RandomGenerator random;
    private final List<SimulationEvent> catalog;
    private final WeightedEventSelector selector;
    private final Map<String, Integer> lastOccurrence = new HashMap<>();
    private final Map<Integer, Integer> errorsByDriver = new HashMap<>();

    public EventManager() {
        this(EventProbabilityConfig.standard(), new Random());
    }

    public EventManager(long seed) {
        this(EventProbabilityConfig.standard(), new Random(seed));
    }

    public EventManager(EventProbabilityConfig probabilities, RandomGenerator random) {
        this(probabilities, random, EventCatalog.defaultEvents());
    }

    /**
     * Permite sustituir el catálogo sin cambiar el coordinador. Es útil para
     * extensiones del simulador y para pruebas deterministas de cada evento.
     */
    public EventManager(EventProbabilityConfig probabilities, RandomGenerator random,
                        List<SimulationEvent> catalog) {
        this.probabilities = probabilities;
        this.random = random;
        this.catalog = List.copyOf(catalog);
        this.selector = new WeightedEventSelector();
    }

    public List<EventOccurrence> resolve(EventContext originalContext) {
        int accumulatedErrors = Math.max(originalContext.erroresPrevios(),
                errorsByDriver.getOrDefault(originalContext.piloto().getId(), 0));
        EventContext context = originalContext.conErroresPrevios(accumulatedErrors);
        EventCategory category = selectCategory(context);
        if (category == EventCategory.NO_EVENT) {
            return List.of(EventOccurrence.noEvent(
                    context.piloto().getId(), context.piloto().getNombre(), context.vuelta()));
        }

        TrackSector sector = randomSector();
        EventContext sectorContext = context.conSector(sector);
        Optional<SimulationEvent> selected = selectEvent(category, sectorContext, null);
        if (selected.isEmpty()) {
            return List.of(EventOccurrence.noEvent(
                    context.piloto().getId(), context.piloto().getNombre(), context.vuelta()));
        }

        List<EventOccurrence> occurrences = new ArrayList<>(2);
        EventOccurrence primary = createOccurrence(selected.get(), sectorContext);
        occurrences.add(primary);
        remember(primary, sectorContext);

        if (primary.tipo() != EventType.CRASH
                && random.nextDouble() < probabilities.coexistenciaGlobal()) {
            EventScope complementaryScope = primary.alcance() == EventScope.GLOBAL
                    ? EventScope.INDIVIDUAL : EventScope.GLOBAL;
            selectComplementary(sectorContext, complementaryScope, primary.tipo())
                    .map(event -> createOccurrence(event, sectorContext))
                    .ifPresent(companion -> {
                        occurrences.add(companion);
                        remember(companion, sectorContext);
                    });
        }
        return List.copyOf(occurrences);
    }

    public int catalogSize() {
        return catalog.size();
    }

    public void startSession() {
        lastOccurrence.clear();
        errorsByDriver.clear();
    }

    private EventCategory selectCategory(EventContext context) {
        List<EventCategory> categories = Arrays.asList(EventCategory.values());
        return selector.select(categories,
                        category -> probabilities.probabilidad(category)
                                * categoryModifier(category, context), random)
                .orElse(EventCategory.NO_EVENT);
    }

    private double categoryModifier(EventCategory category, EventContext context) {
        return switch (category) {
            case NO_EVENT -> 1;
            case POSITIVE -> 0.8 + 0.4 * context.habilidadNormalizada();
            case MINOR_NEGATIVE -> context.multiplicadorRiesgoError();
            case MAJOR_NEGATIVE -> 0.65 * context.multiplicadorRiesgoError()
                    + 0.35 * context.multiplicadorRiesgoVehiculo();
            case WEATHER_TRACK -> context.multiplicadorRiesgoClimatico();
            case EXCEPTIONAL -> Math.min(1.5,
                    0.65 + 0.35 * context.multiplicadorRiesgoAccidente());
        };
    }

    private Optional<SimulationEvent> selectEvent(EventCategory category,
                                                   EventContext context,
                                                   EventScope requiredScope) {
        List<SimulationEvent> valid = catalog.stream()
                .filter(event -> event.type().getCategoria() == category)
                .filter(event -> requiredScope == null || event.type().getAlcance() == requiredScope)
                .filter(event -> event.isCompatible(context))
                .filter(event -> !isCoolingDown(event.type(), context))
                .toList();
        return selector.select(valid, event -> event.weight(context), random);
    }

    private Optional<SimulationEvent> selectComplementary(EventContext context,
                                                           EventScope scope,
                                                           EventType primaryType) {
        List<SimulationEvent> valid = catalog.stream()
                .filter(event -> event.type().getAlcance() == scope)
                .filter(event -> event.type() != primaryType)
                .filter(event -> event.type() != EventType.CRASH)
                .filter(event -> event.type() != EventType.RED_FLAG)
                .filter(event -> event.isCompatible(context))
                .filter(event -> !isCoolingDown(event.type(), context))
                .toList();
        return selector.select(valid, event -> event.weight(context), random);
    }

    private EventOccurrence createOccurrence(SimulationEvent event, EventContext context) {
        return new EventOccurrence(event.type(), context.piloto().getId(),
                context.piloto().getNombre(), context.vuelta(), context.sector(),
                event.createImpact(context, random));
    }

    private void remember(EventOccurrence occurrence, EventContext context) {
        lastOccurrence.put(cooldownKey(occurrence.tipo(), context), context.secuencia());
        if (ERROR_CATEGORIES.contains(occurrence.categoria())
                && occurrence.tipo() != EventType.RED_FLAG) {
            errorsByDriver.merge(context.piloto().getId(), 1, Integer::sum);
        }
    }

    private boolean isCoolingDown(EventType type, EventContext context) {
        Integer previous = lastOccurrence.get(cooldownKey(type, context));
        return previous != null
                && context.secuencia() - previous <= type.getCooldownVueltas();
    }

    private String cooldownKey(EventType type, EventContext context) {
        String owner = type.getAlcance() == EventScope.GLOBAL
                ? "GLOBAL" : String.valueOf(context.piloto().getId());
        return owner + ':' + type.name();
    }

    private TrackSector randomSector() {
        return switch (random.nextInt(3)) {
            case 0 -> TrackSector.SECTOR_1;
            case 1 -> TrackSector.SECTOR_2;
            default -> TrackSector.SECTOR_3;
        };
    }
}
