package me.alidg.errors.annotation;

import java.lang.annotation.*;

/**
 * Annotate fields/methods inside an exception with this annotation to expose its
 * value or return value for message interpolation. This way we can convey some information
 * from the exception to the translated message. For example, suppose we defined an
 * exception like the following:
 * <pre>
 *
 *     &#64;ExceptionMapping(statusCode=BAD_REQUEST, errorCode="user.exists")
 *     public class UserExistsException extends RuntimeException {
 *         &#64;ExposeArg private final String username;
 *
 *         // constructor and etc.
 *     }
 * </pre>
 * With this setting, when the exception happens, the {@link me.alidg.errors.handlers.AnnotatedWebErrorHandler}
 * would pick the error code from the annotation and find an appropriate message for the error code.
 * By annotating the {@code username} property with the {@link ExposeArg} annotation, we can use the username
 * value to report it in the translated error message:
 * <pre>
 *
 *     user.exists=Another user with {username} username is already exists.
 * </pre>
 * When interpolating the error message from the error code, the {@code {username}} would be replaced
 * with the value of {@code username} value exposed with:
 * <pre>
 *
 *     &#64;ExposeArg private final String username;
 * </pre>
 * <p>
 * This annotation has precedence over {@link ExposeAsArg} when both are used on the same element.
 *
 * @author zarebski.m
 * @see ExceptionMapping
 * @see ExposeAsArg
 * @see me.alidg.errors.handlers.AnnotatedWebErrorHandler
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ExposeArg {
    /**
     * If the arguments are meant to be exposed, then overrides the to-be-exposed name.
     *
     * @return The to-be-exposed name.
     */
    String value() default "";

    int order() default Integer.MAX_VALUE;
}
