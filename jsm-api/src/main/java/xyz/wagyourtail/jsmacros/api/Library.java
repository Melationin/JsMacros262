package xyz.wagyourtail.jsmacros.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The script-visible name of a {@link BaseLibrary}.
 *
 * @author Wagyourtail
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Library {
    String value();

}
