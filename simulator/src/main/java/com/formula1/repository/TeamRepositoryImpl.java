package com.formula1.repository;

import com.formula1.database.MongoConnection;
import com.formula1.model.Team;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

public class TeamRepositoryImpl implements TeamRepository {

    private static final String COLLECTION_NAME = "teams";

    private final MongoCollection<Document> collection;

    public TeamRepositoryImpl() {
        this.collection = MongoConnection.getInstance().getDatabase().getCollection(COLLECTION_NAME);
    }

    @Override
    public Team save(Team entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Team <-> Document en MongoDB");
    }

    @Override
    public Optional<Team> findById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Team <-> Document en MongoDB");
    }

    @Override
    public List<Team> findAll() {
        throw new UnsupportedOperationException("TODO: implementar mapeo Team <-> Document en MongoDB");
    }

    @Override
    public Team update(Team entity) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Team <-> Document en MongoDB");
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("TODO: implementar mapeo Team <-> Document en MongoDB");
    }
}
