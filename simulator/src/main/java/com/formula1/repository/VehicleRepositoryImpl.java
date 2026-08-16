package com.formula1.repository;

import com.formula1.database.MongoConnection;
import com.formula1.model.Vehicle;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public class VehicleRepositoryImpl implements VehicleRepository {

    private static final String COLLECTION_NAME = "vehicles";

    private final MongoCollection<Document> collection;

    public VehicleRepositoryImpl() {
        this.collection = MongoConnection.getInstance().getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public Vehicle save(Vehicle entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Vehicle <-> Document en MongoDB");
    }

    @Override
    public Optional<Vehicle> findById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Vehicle <-> Document en MongoDB");
    }

    @Override
    public List<Vehicle> findAll() {
        throw new UnsupportedOperationException("TODO: implementar mapeo Vehicle <-> Document en MongoDB");
    }

    @Override
    public Vehicle update(Vehicle entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Vehicle <-> Document en MongoDB");
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Vehicle <-> Document en MongoDB");
    }
}
