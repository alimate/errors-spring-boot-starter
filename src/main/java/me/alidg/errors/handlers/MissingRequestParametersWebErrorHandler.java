package me.alidg.errors.handlers;

import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MissingMatrixVariableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Spring Framework 5.1 added a few specific MVC exceptions for missing header, cookie,
 * matrix variables to allow for differentiated exception handling and status codes. The
 * main motivation of this handler is to handle those new exceptions.
 *
 * <p>We chose to handle those exceptions here (and not in {@link ServletWebErrorHandler}),
 * because this way we can conditionally register this handler iff those exceptions are
 * present in the classpath.
 *
 * @author Ali Dehghani
 */
public class MissingRequestParametersWebErrorHandler implements WebErrorHandler {

    /**
     * A required header is missing from the request.
     */
    public static final String MISSING_HEADER = "web.missing_header";

    /**
     * A required cookie is missing from the request.
     */
    public static final String MISSING_COOKIE = "web.missing_cookie";

    /**
     * A required matrix variable is missing from the request.
     */
    public static final String MISSING_MATRIX_VARIABLE = "web.missing_matrix_variable";

    /**
     * Only can handle exceptions about missing required headers, cookies or matrix variables.
     *
     * @param exception The exception to examine.
     * @return {@code true} when the exception is related to missing required headers, cookies or
     *         matrix variables, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof MissingRequestHeaderException ||
                exception instanceof MissingRequestCookieException ||
                exception instanceof MissingMatrixVariableException;
    }

    /**
     * Handles the given exception by selecting the appropriate error code, status code and
     * to-be-exposed arguments conditionally.
     *
     * @param exception The exception to handle.
     * @return The handled exception.
     */
    @NonNull
    @Override
    public HandledException handle(Throwable exception) {
        List<String> arguments = null;
        String errorCode = "unknown_error";

        if (exception instanceof MissingRequestHeaderException) {
            arguments = singletonList(((MissingRequestHeaderException) exception).getHeaderName());
            errorCode = MISSING_HEADER;
        } else if (exception instanceof MissingRequestCookieException) {
            arguments = singletonList(((MissingRequestCookieException) exception).getCookieName());
            errorCode = MISSING_COOKIE;
        } else if (exception instanceof MissingMatrixVariableException) {
            arguments = singletonList(((MissingMatrixVariableException) exception).getVariableName());
            errorCode = MISSING_MATRIX_VARIABLE;
        }

        return new HandledException(errorCode, BAD_REQUEST, singletonMap(errorCode, arguments));
    }
}
