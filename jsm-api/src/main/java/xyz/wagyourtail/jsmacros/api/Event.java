package xyz.wagyourtail.jsmacros.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The script-visible name of a {@link BaseEvent}.
 *
 * @author Wagyourtail
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Event {
    String value();

    String oldName() default "";

    boolean cancellable() default false;

    boolean joinable() default false;

    Class<? extends EventFilterer> filterer() default EventFilterer.class;

}
