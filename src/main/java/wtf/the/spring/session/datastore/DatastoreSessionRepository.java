package wtf.the.spring.session.datastore;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
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

    @NewSpan
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

    @NewSpan
    @Override
    public MapSession findById(String id) {
        return Optional
            .ofNullable(datastore.get(key(id)))
            .map(entity -> entity.getBlob("data"))
            .map(Blob::toByteArray)
            .map(SerializationUtils::deserialize)
            .filter(MapSession.class::isInstance)
            .map(MapSession.class::cast)
            .filter(not(Session::isExpired))
            .orElse(null);
    }

    @NewSpan
    @Override
    public void deleteById(String id) {
        datastore.delete(key(id));
    }
}
