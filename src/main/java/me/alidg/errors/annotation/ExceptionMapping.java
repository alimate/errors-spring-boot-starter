package me.alidg.errors.annotation;

import org.springframework.http.HttpStatus;

import java.lang.annotation.*;

/**
 * When an exception annotated with this annotation happens, the metadata encapsulated
 * in the annotation would help us to transform the language level exception to REST API
 * error code/status code combination.
 *
 * @author Ali Dehghani
 * @see ExposeAsArg
 * @see me.alidg.errors.handlers.AnnotatedWebErrorHandler
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionMapping {

    /**
     * Corresponding HTTP status code for this particular exception.
     */
    String errorCode();

    /**
     * The mapping error code for this exception.
     */
    HttpStatus statusCode();
}
