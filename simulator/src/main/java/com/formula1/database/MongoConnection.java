package com.formula1.database;

import com.formula1.exception.DatabaseException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Punto único de acceso a la conexión de MongoDB (patrón Singleton).
 */
public final class MongoConnection {

    private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE_NAME = "formula1simulator";

    private static MongoConnection instance;

    private final MongoClient client;
    private final MongoDatabase database;

    private MongoConnection() {
        try {
            String uri = System.getenv().getOrDefault("MONGO_URI", DEFAULT_MONGO_URI);
            String databaseName = System.getenv().getOrDefault("MONGO_DATABASE", DEFAULT_DATABASE_NAME);
            this.client = MongoClients.create(uri);
            this.database = client.getDatabase(databaseName);
        } catch (Exception e) {
            throw new DatabaseException("No se pudo establecer conexión con MongoDB", e);
        }
    }

    public static synchronized MongoConnection getInstance() {
        if (instance == null) {
            instance = new MongoConnection();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        if (client != null) {
            client.close();
        }
        instance = null;
    }
}
