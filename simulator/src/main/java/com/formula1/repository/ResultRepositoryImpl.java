package com.formula1.repository;

import com.formula1.database.MongoConnection;
import com.formula1.model.Result;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public class ResultRepositoryImpl implements ResultRepository {

    private static final String COLLECTION_NAME = "results";

    private final MongoCollection<Document> collection;

    public ResultRepositoryImpl() {
        this.collection = MongoConnection.getInstance().getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public Result save(Result entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Result <-> Document en MongoDB");
    }

    @Override
    public Optional<Result> findById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Result <-> Document en MongoDB");
    }

    @Override
    public List<Result> findAll() {
        throw new UnsupportedOperationException("TODO: implementar mapeo Result <-> Document en MongoDB");
    }

    @Override
    public Result update(Result entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Result <-> Document en MongoDB");
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Result <-> Document en MongoDB");
    }
}
