package wtf.the.spring.session.datastore;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
@Import(DatastoreHttpSessionConfiguration.class)
@Configuration
public @interface EnableDatastoreHttpSession {

    String kind();

}
