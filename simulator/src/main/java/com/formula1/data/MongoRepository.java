package com.formula1.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.mongodb.client.model.Filters.eq;

/**
 * Repositorio genérico sobre MongoDB.
 *
 * Una sola clase sirve a todas las entidades: el mapeo entidad↔{@code Document}
 * se delega en Jackson, que ya sabe leer el formato de la especificación
 * gracias a las anotaciones del modelo. Sustituye a las cinco parejas
 * interfaz + implementación que había antes.
 */
public class MongoRepository<T, ID> implements CrudRepository<T, ID> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String coleccion;
    private final Class<T> tipo;
    private final Function<T, ID> extractorId;

    public MongoRepository(String coleccion, Class<T> tipo, Function<T, ID> extractorId) {
        this.coleccion = coleccion;
        this.tipo = tipo;
        this.extractorId = extractorId;
    }

    private MongoCollection<Document> coleccion() {
        return MongoConnection.getInstance().getBase().getCollection(coleccion);
    }

    private Document aDocumento(T entidad) {
        try {
            Document documento = Document.parse(MAPPER.writeValueAsString(entidad));
            documento.put("_id", String.valueOf(extractorId.apply(entidad)));
            return documento;
        } catch (Exception e) {
            throw new DataAccessException("No se pudo serializar " + tipo.getSimpleName(), e);
        }
    }

    private T aEntidad(Document documento) {
        try {
            // El _id es artefacto de Mongo; el modelo ya ignora campos desconocidos,
            // pero se retira para que el JSON sea idéntico al del seed.
            Document copia = new Document(documento);
            copia.remove("_id");
            return MAPPER.readValue(copia.toJson(), tipo);
        } catch (Exception e) {
            throw new DataAccessException("No se pudo leer " + tipo.getSimpleName() + " desde MongoDB", e);
        }
    }

    @Override
    public T save(T entidad) {
        Document documento = aDocumento(entidad);
        coleccion().replaceOne(eq("_id", documento.get("_id")), documento, new ReplaceOptions().upsert(true));
        return entidad;
    }

    @Override
    public void saveAll(List<T> entidades) {
        for (T entidad : entidades) {
            save(entidad);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        Document documento = coleccion().find(eq("_id", String.valueOf(id))).first();
        return documento == null ? Optional.empty() : Optional.of(aEntidad(documento));
    }

    @Override
    public List<T> findAll() {
        List<T> entidades = new ArrayList<>();
        for (Document documento : coleccion().find()) {
            entidades.add(aEntidad(documento));
        }
        return entidades;
    }

    @Override
    public void deleteById(ID id) {
        coleccion().deleteOne(eq("_id", String.valueOf(id)));
    }
}
