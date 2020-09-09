package wtf.the.spring.session.datastore;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.time.temporal.ChronoUnit.HOURS;

@Retention(RUNTIME)
@Target(TYPE)
@Import(DatastoreHttpSessionConfiguration.class)
@Configuration
@Documented
public @interface EnableDatastoreHttpSession {

    String kind();
    long ttl() default 1;
    ChronoUnit ttlUnit() default HOURS;

}
