package com.formula1.data;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

/**
 * Conexión única a MongoDB (Singleton).
 *
 * El driver trae un {@code serverSelectionTimeout} de 30 s por defecto y
 * {@code MongoClients.create} es perezoso: no falla al construirse, sino en
 * la primera operación. Sin recortar ese tiempo, la aplicación se quedaría
 * bloqueada medio minuto en cada llamada cuando no hay servidor. Por eso se
 * baja a {@value #TIMEOUT_SEGUNDOS} s y se expone {@link #isDisponible()}.
 */
public final class MongoConnection {

    private static final String URI_POR_DEFECTO = "mongodb://localhost:27017";
    private static final String BD_POR_DEFECTO = "formula1simulator";
    private static final int TIMEOUT_SEGUNDOS = 2;

    private static MongoConnection instancia;

    private final MongoClient cliente;
    private final MongoDatabase base;

    private MongoConnection() {
        String uri = System.getenv().getOrDefault("MONGO_URI", URI_POR_DEFECTO);
        String nombreBase = System.getenv().getOrDefault("MONGO_DATABASE", BD_POR_DEFECTO);
        try {
            MongoClientSettings ajustes = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .applyToClusterSettings(b -> b.serverSelectionTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS))
                    .applyToSocketSettings(b -> b.connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS))
                    .build();
            this.cliente = MongoClients.create(ajustes);
            this.base = cliente.getDatabase(nombreBase);
        } catch (RuntimeException e) {
            throw new DataAccessException("No se pudo preparar la conexión con MongoDB", e);
        }
    }

    public static synchronized MongoConnection getInstance() {
        if (instancia == null) {
            instancia = new MongoConnection();
        }
        return instancia;
    }

    /**
     * Comprueba con un {@code ping} si el servidor responde. Nunca lanza:
     * devolver {@code false} es una respuesta válida y significa que la
     * aplicación debe seguir en memoria.
     */
    public boolean isDisponible() {
        try {
            base.runCommand(new Document("ping", 1));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public MongoDatabase getBase() {
        return base;
    }

    public static synchronized void cerrar() {
        if (instancia != null) {
            instancia.cliente.close();
            instancia = null;
        }
    }
}
