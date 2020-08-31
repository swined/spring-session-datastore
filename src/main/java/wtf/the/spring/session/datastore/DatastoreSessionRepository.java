package wtf.the.spring.session.datastore;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

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
        var attrs = Entity.newBuilder();
        for (var attr : session.getAttributeNames()) {
            attrs.set(attr, BlobValue
                .newBuilder(Blob.copyFrom(requireNonNull(serialize(session.getAttribute(attr)))))
                .setExcludeFromIndexes(true)
                .build()
            );
        }
        datastore.put(Entity
            .newBuilder(key(session.getId()))
            .set("ctime", Timestamp.of(Date.from(session.getCreationTime())))
            .set("atime", Timestamp.of(Date.from(session.getLastAccessedTime())))
            .set("ttl", session.getMaxInactiveInterval().toSeconds())
            .set("attrs", attrs.build())
            .set("expire", Timestamp.of(Date.from(session.getLastAccessedTime().plus(session.getMaxInactiveInterval()))))
            .build()
        );
    }

    @Override
    public MapSession findById(String id) {
        return Optional
            .ofNullable(datastore.get(key(id)))
            .map(entity -> {
                var session = new MapSession(entity.getKey().getName());
                session.setCreationTime(entity.getTimestamp("ctime").toDate().toInstant());
                session.setLastAccessedTime(entity.getTimestamp("atime").toDate().toInstant());
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
