package com.formula1.repository;

import com.formula1.database.MongoConnection;
import com.formula1.model.Driver;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public class DriverRepositoryImpl implements DriverRepository {

    private static final String COLLECTION_NAME = "drivers";

    private final MongoCollection<Document> collection;

    public DriverRepositoryImpl() {
        this.collection = MongoConnection.getInstance().getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public Driver save(Driver entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Driver <-> Document en MongoDB");
    }

    @Override
    public Optional<Driver> findById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Driver <-> Document en MongoDB");
    }

    @Override
    public List<Driver> findAll() {
        throw new UnsupportedOperationException("TODO: implementar mapeo Driver <-> Document en MongoDB");
    }

    @Override
    public Driver update(Driver entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Driver <-> Document en MongoDB");
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Driver <-> Document en MongoDB");
    }
}
