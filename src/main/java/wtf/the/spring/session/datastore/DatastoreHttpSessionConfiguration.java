package wtf.the.spring.session.datastore;

import com.google.cloud.datastore.Datastore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;

import static java.util.Objects.requireNonNull;

@Configuration
public class DatastoreHttpSessionConfiguration extends SpringHttpSessionConfiguration implements ImportAware {

    private String kind = null;

    @Override
    public void setImportMetadata(AnnotationMetadata meta) {
        var map = meta.getAnnotationAttributes(EnableDatastoreHttpSession.class.getName());
        var attrs = requireNonNull(AnnotationAttributes.fromMap(map));
        kind = attrs.getString("kind");
    }

    @Bean
    public SessionRepository<MapSession> sessionRepository(Datastore datastore) {
        return new DatastoreSessionRepository(datastore, kind);
    }
}
