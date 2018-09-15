package me.alidg.errors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

/**
 * Encapsulates details about a handled exception, including:
 * <ul>
 * <li>The mapped business level error codes</li>
 * <li>The corresponding HTTP status code</li>
 * <li>A collection of arguments that can be used for message translation</li>
 * </ul>
 *
 * @author Ali Dehghani
 * @see WebErrorHandler
 */
public class HandledException {

    /**
     * Collection of error codes corresponding to the handled exception. Usually this collection
     * contains only one error code but not always, say for validation errors.
     */
    private final Set<String> errorCodes;

    /**
     * Corresponding status code for the handled exception.
     */
    private final HttpStatus statusCode;

    /**
     * Collection of to-be-exposed arguments grouped by the error code. This is a mapping
     * between the error code and all its to-be-exposed arguments. For example, suppose
     * we have a bean like:
     * <pre>
     *     public class User {
     *
     *         &#64;Size(min=1, max=7, message="interests.range_limit")
     *         private List&lt;String&gt; interests;
     *         // omitted for the sake of brevity
     *     }
     * </pre>
     * If the given interest list wasn't valid, then this map would contain an entry with the
     * {@code interests.range_limit} as the key and {@code List(1, 7)} as the values. Later on
     * we can use those exposed values in our message, for example, the following error template:
     * <pre>
     *     You should define between {0} and {1} interests.
     * </pre>
     * Would be translated to:
     * <pre>
     *     You should define between 1 and 7 interests.
     * </pre>
     */
    private final Map<String, List<?>> arguments;

    /**
     * Initialize a handled exception with a set of error codes, a HTTP status code and an
     * optional collection of arguments.
     *
     * @param errorCodes The corresponding error codes for the handled exception.
     * @param statusCode The corresponding status code for the handled exception.
     * @param arguments Arguments to be exposed from the handled exception to the outside world.
     *
     * @throws NullPointerException When one of the required parameters is null.
     * @throws IllegalArgumentException At least one error code should be provided.
     */
    public HandledException(@NonNull Set<String> errorCodes,
                            @NonNull HttpStatus statusCode,
                            @Nullable Map<String, List<?>> arguments) {
        enforcePreconditions(errorCodes, statusCode);
        this.errorCodes = errorCodes;
        this.statusCode = statusCode;
        this.arguments = arguments == null ? Collections.emptyMap() : arguments;
    }

    /**
     * Initialize a handled exception with an error code, a HTTP status code and an
     * optional collection of arguments.
     *
     * @param errorCode The corresponding error code for the handled exception.
     * @param statusCode The corresponding status code for the handled exception.
     * @param arguments Arguments to be exposed from the handled exception to the outside world.
     *
     * @throws NullPointerException When one of the required parameters is null.
     * @throws IllegalArgumentException At least one error code should be provided.
     */
    public HandledException(@NonNull String errorCode,
                            @NonNull HttpStatus statusCode,
                            @Nullable Map<String, List<?>> arguments) {
        this(singleton(errorCode), statusCode, arguments);
    }

    /**
     * @return Collection of mapped error codes.
     * @see #errorCodes
     */
    @NonNull
    public Set<String> getErrorCodes() {
        return errorCodes;
    }

    /**
     * @return The mapped status code.
     * @see #statusCode
     */
    @NonNull
    public HttpStatus getStatusCode() {
        return statusCode;
    }

    /**
     * @return Collection of to-be-exposed arguments.
     * @see #arguments
     */
    @NonNull
    public Map<String, List<?>> getArguments() {
        return arguments;
    }

    private void enforcePreconditions(Set<String> errorCodes, HttpStatus statusCode) {
        requireNonNull(errorCodes, "Error codes is required");
        requireNonNull(statusCode, "Status code is required");

        if (errorCodes.isEmpty())
            throw new IllegalArgumentException("At least one error code should be provided");

        if (errorCodes.size() == 1 && errorCodes.contains(null))
            throw new NullPointerException("The single error code can't be null");
    }
}
