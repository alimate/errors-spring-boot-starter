package me.alidg.errors.adapter;

import me.alidg.errors.HttpError;
import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.annotation.ExceptionMapping;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

import static java.util.Objects.requireNonNull;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * Custom implementation of {@link org.springframework.boot.web.servlet.error.ErrorAttributes}
 * which adapts the handled {@link HttpError} to a Spring Boot's compatible error attributes
 * representation.
 *
 * @author Ali Dehghani
 * @see HttpError
 * @see HttpErrorAttributesAdapter
 * @see org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
 */
public class HttpErrorAttributes extends DefaultErrorAttributes {

    /**
     * To convey the status code from the handled exception to the
     * {@link org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController}.
     */
    private static final String STATUS_CODE_ATTR = "javax.servlet.error.status_code";

    /**
     * To handle exceptions.
     */
    private final WebErrorHandlers webErrorHandlers;

    /**
     * To adapt our representation of an error to Spring Boot's representation.
     */
    private final HttpErrorAttributesAdapter httpErrorAttributesAdapter;

    /**
     * Initializes the error attributes with required dependencies.
     *
     * @param webErrorHandlers           To handle exceptions.
     * @param httpErrorAttributesAdapter To adapt our representation of an error to Spring Boot's representation.
     * @throws NullPointerException When one of the required parameters is null.
     */
    public HttpErrorAttributes(WebErrorHandlers webErrorHandlers,
                               HttpErrorAttributesAdapter httpErrorAttributesAdapter) {
        requireNonNull(webErrorHandlers, "Web error handlers is required");
        requireNonNull(httpErrorAttributesAdapter, "Adapter is required");

        this.webErrorHandlers = webErrorHandlers;
        this.httpErrorAttributesAdapter = httpErrorAttributesAdapter;
    }

    /**
     * Extracts the thrown exception from the request attributes. If it was null, then checks the stored
     * status code and tries its best to find an exception related to that status code. Finally, handles
     * the exception using the {@link #webErrorHandlers} and adapts the returned {@link HttpError} to a
     * Spring Boot compatible representation.
     *
     * @param webRequest        The current HTTP request.
     * @param includeStackTrace Whether or not to include the stack trace in the error attributes.
     * @return Error details.
     */
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, includeStackTrace);
        Throwable exception = getError(webRequest);

        if (exception == null) {
            switch (getStatusCode(attributes)) {
                case 401:
                    exception = new UnauthorizedException();
                    break;
                case 403:
                    exception = new ForbiddenException();
                    break;
                case 404:
                    exception = new NoHandlerFoundException("", getPath(attributes), null);
                    break;
                default:
                    exception = new IllegalStateException("The exception is null: " + attributes);
            }
        }

        HttpError httpError = webErrorHandlers.handle(exception, webRequest.getLocale());
        saveStatusCodeInRequest(webRequest, httpError);

        return httpErrorAttributesAdapter.adapt(httpError);
    }

    /**
     * Storing the handled status code in the request as an attribute. The
     * {@link org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController}
     * would use this status code when it's writing the HTTP response.
     *
     * @param webRequest The request.
     * @param httpError  Represents the handled exception.
     */
    private void saveStatusCodeInRequest(WebRequest webRequest, HttpError httpError) {
        int statusCode = httpError.getHttpStatus().value();
        webRequest.setAttribute(STATUS_CODE_ATTR, statusCode, SCOPE_REQUEST);
    }

    /**
     * Extracts the status code from error attributes.
     *
     * @param attributes The error attributes.
     * @return Extracted status code.
     */
    private int getStatusCode(Map<String, Object> attributes) {
        try {
            return (Integer) attributes.getOrDefault("status", 0);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * @param attributes The error attributes.
     * @return The request URL.
     */
    private String getPath(Map<String, Object> attributes) {
        try {
            return (String) attributes.getOrDefault("path", "N/A");
        } catch (Exception e) {
            return "N/A";
        }
    }

    @ExceptionMapping(statusCode = UNAUTHORIZED, errorCode = AUTH_REQUIRED)
    private static final class UnauthorizedException extends RuntimeException {}

    @ExceptionMapping(statusCode = FORBIDDEN, errorCode = ACCESS_DENIED)
    private static final class ForbiddenException extends RuntimeException {}
}
