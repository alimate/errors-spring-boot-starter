package me.alidg.errors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Encapsulates details about a handled exception, including:
 * <ul>
 * <li>The mapped business level error codes and their arguments that can be used for message translation</li>
 * <li>The corresponding HTTP status code</li>
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
    private final List<ErrorWithArguments> errors;

    /**
     * Corresponding status code for the handled exception.
     */
    private final HttpStatus statusCode;

    /**
     * Initialize a handled exception with a set of error codes, a HTTP status code and an
     * optional collection of arguments.
     *
     * @param errors     The corresponding error codes for the handled exception.
     * @param statusCode The corresponding status code for the handled exception.
     * @throws NullPointerException     When one of the required parameters is null.
     * @throws IllegalArgumentException At least one error code should be provided.
     */
    public HandledException(@NonNull List<ErrorWithArguments> errors,
                            @NonNull HttpStatus statusCode) {
        enforcePreconditions(errors, statusCode);
        this.errors = errors;
        this.statusCode = statusCode;
    }

    /**
     * Initialize a handled exception with an error code, a HTTP status code and an
     * optional collection of arguments.
     *
     * @param error      The corresponding error code for the handled exception.
     * @param statusCode The corresponding status code for the handled exception.
     * @throws NullPointerException     When one of the required parameters is null.
     * @throws IllegalArgumentException At least one error code should be provided.
     */
    public HandledException(@NonNull ErrorWithArguments error,
                            @NonNull HttpStatus statusCode) {
        this(singletonList(error), statusCode);
    }

    /**
     *
     * @return Collection of errors
     */
    @NonNull
    public List<ErrorWithArguments> getErrors() {
        return errors;
    }

    /**
     * @return Collection of mapped error codes.
     * @see #errors
     */
    @NonNull
    public Set<String> getErrorCodes() {
        return errors.stream()
                     .map(ErrorWithArguments::getErrorCode)
                     .collect(Collectors.toSet());
    }

    /**
     * @return The mapped status code.
     * @see #statusCode
     */
    @NonNull
    public HttpStatus getStatusCode() {
        return statusCode;
    }

    private void enforcePreconditions(List<ErrorWithArguments> errorCodes, HttpStatus statusCode) {
        requireNonNull(errorCodes, "Error codes is required");
        requireNonNull(statusCode, "Status code is required");

        if (errorCodes.isEmpty()) {
            throw new IllegalArgumentException("At least one error code should be provided");
        }

        if (errorCodes.size() == 1 && errorCodes.contains(null)) {
            throw new NullPointerException("The single error code can't be null");
        }
    }
}
