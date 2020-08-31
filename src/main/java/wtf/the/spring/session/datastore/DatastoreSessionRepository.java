package wtf.the.spring.session.datastore;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.Key;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.SerializationUtils.deserialize;
import static org.springframework.util.SerializationUtils.serialize;

public class DatastoreSessionRepository implements SessionRepository<MapSession> {

    private final Datastore datastore;
    private final String kind;

    public DatastoreSessionRepository(Datastore datastore, String kind) {
        this.datastore = requireNonNull(datastore);
        this.kind = requireNonNull(kind);
    }

    private Key key(String id) {
        return datastore.newKeyFactory().setKind(kind).newKey(id);
    }

    private static Timestamp toDatastore(Instant instant) {
        return Timestamp.of(Date.from(instant));
    }

    private static Instant fromDatastore(Timestamp timestamp) {
        return timestamp.toDate().toInstant();
    }

    @Override
    public MapSession createSession() {
        return new MapSession();
    }

    @Override
    public void save(MapSession session) {
        var attrs = Entity.newBuilder();
        for (var attr : session.getAttributeNames()) {
            attrs.set(attr, Blob.copyFrom(requireNonNull(serialize(session.getAttribute(attr)))));
        }
        datastore.put(Entity
            .newBuilder(key(session.getId()))
            .set("ctime", toDatastore(session.getCreationTime()))
            .set("atime", toDatastore(session.getLastAccessedTime()))
            .set("ttl", session.getMaxInactiveInterval().toSeconds())
            .set("attrs", EntityValue.newBuilder(attrs.build()).setExcludeFromIndexes(true).build())
            .build()
        );
    }

    @Override
    public MapSession findById(String id) {
        return Optional
            .ofNullable(datastore.get(key(id)))
            .map(entity -> {
                var session = new MapSession(entity.getKey().getName());
                session.setCreationTime(fromDatastore(entity.getTimestamp("ctime")));
                session.setLastAccessedTime(fromDatastore(entity.getTimestamp("atime")));
                session.setMaxInactiveInterval(Duration.ofSeconds(entity.getLong("ttl")));
                var attrs = entity.getEntity("attrs");
                for (var attr : attrs.getNames()) {
                    session.setAttribute(attr, deserialize(attrs.getBlob(attr).toByteArray()));
                }
                return session;
            })
            .orElse(null);
    }

    @Override
    public void deleteById(String id) {
        datastore.delete(key(id));
    }
}
