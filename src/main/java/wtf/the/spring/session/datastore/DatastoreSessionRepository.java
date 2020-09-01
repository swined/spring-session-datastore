package wtf.the.spring.session.datastore;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ValueType;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.SerializationUtils.deserialize;
import static org.springframework.util.SerializationUtils.serialize;

public class DatastoreSessionRepository implements SessionRepository<MapSession> {

    private final Datastore datastore;
    private final String kind;
    private final Duration ttl;

    public DatastoreSessionRepository(Datastore datastore, String kind, Duration ttl) {
        this.datastore = requireNonNull(datastore);
        this.kind = requireNonNull(kind);
        this.ttl = requireNonNull(ttl);
    }

    private Key key(String id) {
        return datastore.newKeyFactory().setKind(kind).newKey(id);
    }

    @Override
    public MapSession createSession() {
        var session = new MapSession();
        session.setMaxInactiveInterval(ttl);
        return session;
    }

    @Override
    public void save(MapSession session) {
        datastore.put(Entity
            .newBuilder(key(session.getId()))
            .set("data", BlobValue
                .newBuilder(Blob.copyFrom(requireNonNull(serialize(session))))
                .setExcludeFromIndexes(true)
                .build()
            )
            .set("expire", Timestamp.of(Date.from(session.getLastAccessedTime().plus(session.getMaxInactiveInterval()))))
            .build()
        );
    }

    @Override
    public MapSession findById(String id) {
        return Optional
            .ofNullable(datastore.get(key(id)))
            .filter(entity -> entity.contains("data") && entity.getValue("data").getType() == ValueType.BLOB)
            .map(entity -> (MapSession)deserialize(entity.getBlob("data").toByteArray()))
            .filter(Predicate.not(Session::isExpired))
            .orElse(null);
    }

    @Override
    public void deleteById(String id) {
        datastore.delete(key(id));
    }
}
