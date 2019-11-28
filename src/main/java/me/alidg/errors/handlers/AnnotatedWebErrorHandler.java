package me.alidg.errors.handlers;

import me.alidg.errors.Argument;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeArg;
import me.alidg.errors.annotation.ExposeAsArg;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static me.alidg.errors.Argument.arg;

/**
 * {@link WebErrorHandler} implementation responsible for handling exceptions annotated with
 * the {@link ExceptionMapping} annotation. The web error code and status code would be
 * extracted form the annotated exception. Also, any member annotated with {@link ExposeArg}
 * would be exposed as arguments.
 *
 * @author Ali Dehghani
 * @see ExposeArg
 * @see ExceptionMapping
 */
public class AnnotatedWebErrorHandler implements WebErrorHandler {

    /**
     * Helps us to sort different elements annotated with {@link ExposeArg} based on their
     * {@link ExposeArg#order()}.
     */
    private static final Comparator<AccessibleObject> byExposedIndex = Comparator
        .comparing((AccessibleObject o) -> requireNonNull(getExposeAnnotation(o)).order())
        .thenComparing(o -> requireNonNull(getExposeAnnotation(o)).value())
        .thenComparing(AnnotatedWebErrorHandler::objectName);

    private static String objectName(AccessibleObject o) {
        if (o instanceof Field) {
            return ((Field) o).getName();
        } else if (o instanceof Method) {
            return ((Method) o).getName();
        }
        return "";
    }

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
     * as the HTTP status code and also, exposing exception members annotated with {@link ExposeArg}.
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
     * Finds all fields and methods annotated with {@link ExposeArg} and return
     * their value or return value as to-be-exposed arguments.
     *
     * @param exception The exception to extract the members from.
     * @return Array of exposed arguments.
     */
    private List<Argument> getExposedValues(Throwable exception) {
        List<AccessibleObject> members = new ArrayList<>();
        members.addAll(getExposedFields(exception));
        members.addAll(getExposedMethods(exception));
        members.sort(byExposedIndex);

        return members
            .stream()
            .map(e -> getArgument(e, exception))
            .filter(Objects::nonNull)
            .collect(toList());
    }

    /**
     * Given an element annotated with {@link ExposeArg}
     *
     * @param element   The field or method we're going to extract its value.
     * @param exception The containing exception that those fields or methods are declared in.
     * @return The field value or method return value.
     */
    private Argument getArgument(AccessibleObject element, Throwable exception) {
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
     * {@link ExposeArg} annotation.
     *
     * @param exception The exception reflect on.
     * @return List of all annotated fields.
     */
    private List<Field> getExposedFields(Throwable exception) {
        return Stream.of(exception.getClass().getDeclaredFields())
            .filter(this::exposeAnnotationIsPresent)
            .collect(toList());
    }

    /**
     * All methods (with a return type and no parameters) annotated with the {@link ExposeArg} annotation.
     *
     * @param exception The exception reflect on.
     * @return List of all annotated methods.
     */
    private List<Method> getExposedMethods(Throwable exception) {
        return Stream.of(exception.getClass().getMethods())
            .filter(m -> exposeAnnotationIsPresent(m) && hasReturnType(m) && hasNoParameters(m))
            .collect(toList());
    }

    /**
     * Returns the to-be-exposed name. If the {@link ExposeArg#value()} is not blank, then
     * it would be the exposed name. Otherwise, we use the {@link Member#getName()} as that name.
     *
     * @param member The exception member.
     * @param <T>    The member type.
     * @return The to-be-exposed name.
     */
    private <T extends AnnotatedElement & Member> String getExposedName(T member) {
        ExposeArg annotation = getExposeAnnotation(member);
        if (annotation != null && !annotation.value().trim().isEmpty()) {
            return annotation.value();
        }

        return member.getName();
    }

    /**
     * Returns instance of {@link ExposeArg} annotation. If this annotation is found on given member,
     * it is simply returned. If {@link ExposeAsArg} annotation is present instead, method returns
     * instance of {@link ExposeArg} migrated from deprecated {@link ExposeAsArg} annotation.
     *
     * @param member The exception member.
     * @return Instance of {@link ExposeArg} annotation, migrated from deprecated {@link ExposeAsArg} if necessary.
     */
    @SuppressWarnings("deprecation")
    private static ExposeArg getExposeAnnotation(AnnotatedElement member) {
        ExposeArg annotation = member.getAnnotation(ExposeArg.class);
        if (annotation != null) return annotation;
        ExposeAsArg legacyAnnotation = member.getAnnotation(ExposeAsArg.class);
        if (legacyAnnotation != null) {
            return new ExposeArg() {
                @Override
                public String value() {
                    return legacyAnnotation.name();
                }

                @Override
                public int order() {
                    return legacyAnnotation.value();
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return ExposeArg.class;
                }
            };
        }
        return null;
    }

    private boolean hasNoParameters(Method m) {
        return m.getParameterCount() == 0;
    }

    private boolean hasReturnType(Method m) {
        return m.getReturnType() != Void.TYPE;
    }

    @SuppressWarnings("deprecation")
    private boolean exposeAnnotationIsPresent(AccessibleObject m) {
        return m.isAnnotationPresent(ExposeArg.class) || m.isAnnotationPresent(ExposeAsArg.class);
    }
}
