package me.alidg.errors.handlers;

import me.alidg.errors.Argument;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeAsArg;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static me.alidg.errors.Argument.arg;

/**
 * {@link WebErrorHandler} implementation responsible for handling exceptions annotated with
 * the {@link ExceptionMapping} annotation. The web error code and status code would be
 * extracted form the annotated exception. Also, any member annotated with {@link ExposeAsArg}
 * would be exposed as arguments.
 *
 * @author Ali Dehghani
 * @see ExposeAsArg
 * @see ExceptionMapping
 */
public class AnnotatedWebErrorHandler implements WebErrorHandler {

    /**
     * Helps us to sort different elements annotated with {@link ExposeAsArg} based on their
     * {@link ExposeAsArg#value()}.
     */
    private final Comparator<AnnotatedElement> byExposedIndex =
            Comparator.comparing(e -> e.getAnnotation(ExposeAsArg.class).value());

    /**
     * Only can handle non-null exceptions annotated with {@link ExceptionMapping} annotation.
     *
     * @param exception The exception to examine.
     * @return {@code true} if the exception is annotated with {@link ExceptionMapping}, {@code false}
     * otherwise.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        if (exception == null) return false;

        return exception.getClass().isAnnotationPresent(ExceptionMapping.class);
    }

    /**
     * Handles the thrown exception annotated with {@link ExceptionMapping} by using the
     * {@link ExceptionMapping#errorCode()} as the error code, the {@link ExceptionMapping#statusCode()}
     * as the HTTP status code and also, exposing exception members annotated with {@link ExposeAsArg}.
     *
     * @param exception The exception to handle.
     * @return An {@link HandledException} instance encapsulating the error code, status code and
     * all to-be-exposed arguments.
     */
    @NonNull
    @Override
    public HandledException handle(Throwable exception) {
        ExceptionMapping exceptionMapping = exception.getClass().getAnnotation(ExceptionMapping.class);
        String errorCode = exceptionMapping.errorCode();
        HttpStatus httpStatus = exceptionMapping.statusCode();
        List<Argument> arguments = getExposedValues(exception);

        return new HandledException(errorCode, httpStatus, singletonMap(errorCode, arguments));
    }

    /**
     * Finds all fields and methods annotated with {@link ExposeAsArg} and return their value or
     * return value as to-be-exposed arguments.
     *
     * @param exception The exception to extract the members from.
     * @return Array of exposed arguments.
     */
    private List<Argument> getExposedValues(Throwable exception) {
        List<AnnotatedElement> members = new ArrayList<>();
        members.addAll(getExposedFields(exception));
        members.addAll(getExposedMethods(exception));
        members.sort(byExposedIndex);

        return members.stream()
                .map(e -> getArgument(e, exception))
                .collect(toList());
    }

    /**
     * Given an element annotated with {@link ExposeAsArg}
     *
     * @param element   The field or method we're going to extract its value.
     * @param exception The containing exception that those fields or methods are declared in.
     * @return The field value or method return value.
     */
    private Argument getArgument(AnnotatedElement element, Throwable exception) {
        try {
            if (element instanceof Field) {
                Field f = (Field) element;
                f.setAccessible(true);

                return arg(getExposedName(f), f.get(exception));
            } else if (element instanceof Method) {
                Method m = (Method) element;
                m.setAccessible(true);

                return arg(getExposedName(m), m.invoke(exception));
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Returns all fields declared in the given {@code exception} that annotated with the
     * {@link ExposeAsArg} annotation.
     *
     * @param exception The exception reflect on.
     * @return List of all annotated fields.
     */
    private List<Field> getExposedFields(Throwable exception) {
        return Stream.of(exception.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(ExposeAsArg.class))
                .collect(toList());
    }

    /**
     * All methods (with a return type and no parameters) annotated with the {@link ExposeAsArg} annotation.
     *
     * @param exception The exception reflect on.
     * @return List of all annotated methods.
     */
    private List<Method> getExposedMethods(Throwable exception) {
        return Stream.of(exception.getClass().getMethods())
                .filter(m -> annotationIsPresent(m) && hasReturnType(m) && hasNoParameters(m))
                .collect(toList());
    }

    /**
     * Returns the to-be-exposed name. If the {@link ExposeAsArg#name()} is not blank, then
     * it would be the exposed name. Otherwise, we use the {@link Member#getName()} as that name.
     *
     * @param member The exception member.
     * @param <T>    The member type.
     * @return The to-be-exposed name.
     */
    private <T extends AnnotatedElement & Member> String getExposedName(T member) {
        ExposeAsArg annotation = member.getAnnotation(ExposeAsArg.class);
        if (annotation != null && !annotation.name().trim().isEmpty()) {
            return annotation.name();
        }

        return member.getName();
    }

    private boolean hasNoParameters(Method m) {
        return m.getParameterCount() == 0;
    }

    private boolean hasReturnType(Method m) {
        return m.getReturnType() != Void.TYPE;
    }

    private boolean annotationIsPresent(Method m) {
        return m.isAnnotationPresent(ExposeAsArg.class);
    }
}
