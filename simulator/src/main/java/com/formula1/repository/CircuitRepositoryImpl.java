package com.formula1.repository;

import com.formula1.database.MongoConnection;
import com.formula1.model.Circuit;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public class CircuitRepositoryImpl implements CircuitRepository {

    private static final String COLLECTION_NAME = "circuits";

    private final MongoCollection<Document> collection;

    public CircuitRepositoryImpl() {
        this.collection = MongoConnection.getInstance().getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public Circuit save(Circuit entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Circuit <-> Document en MongoDB");
    }

    @Override
    public Optional<Circuit> findById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Circuit <-> Document en MongoDB");
    }

    @Override
    public List<Circuit> findAll() {
        throw new UnsupportedOperationException("TODO: implementar mapeo Circuit <-> Document en MongoDB");
    }

    @Override
    public Circuit update(Circuit entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Circuit <-> Document en MongoDB");
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Circuit <-> Document en MongoDB");
    }
}
