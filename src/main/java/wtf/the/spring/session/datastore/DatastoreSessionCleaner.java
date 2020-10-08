package wtf.the.spring.session.datastore;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import org.springframework.cloud.sleuth.annotation.NewSpan;

import java.util.ArrayList;

import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.lt;
import static java.util.Objects.requireNonNull;

public class DatastoreSessionCleaner {

    private final Datastore datastore;
    private final String kind;

    public DatastoreSessionCleaner(Datastore datastore, String kind) {
        this.datastore = requireNonNull(datastore);
        this.kind = requireNonNull(kind);
    }

    @NewSpan
    public void clean() {
        var expired = new ArrayList<Key>();
        do {
            expired.clear();
            datastore.run(Query
                .newKeyQueryBuilder()
                .setKind(kind)
                .setFilter(lt("expire", Timestamp.now()))
                .setLimit(500)
                .build()
            ).forEachRemaining(expired::add);
            datastore.delete(expired.toArray(Key[]::new));
        } while (!expired.isEmpty());
    }
}
