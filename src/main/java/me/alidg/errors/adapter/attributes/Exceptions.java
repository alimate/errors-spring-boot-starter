package me.alidg.errors.adapter.attributes;

import me.alidg.errors.annotation.ExceptionMapping;

import java.util.Map;

import static me.alidg.errors.handlers.ServletWebErrorHandler.NO_HANDLER;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.ACCESS_DENIED;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.AUTH_REQUIRED;
import static org.springframework.http.HttpStatus.*;

/**
 * A simple container of a few peculiar exceptions.
 *
 * @author Ali Dehghani
 */
class Exceptions {

    @ExceptionMapping(statusCode = UNAUTHORIZED, errorCode = AUTH_REQUIRED)
    private static final class UnauthorizedException extends RuntimeException {}

    @ExceptionMapping(statusCode = FORBIDDEN, errorCode = ACCESS_DENIED)
    private static final class ForbiddenException extends RuntimeException {}

    @ExceptionMapping(statusCode = NOT_FOUND, errorCode = NO_HANDLER)
    private static final class HandlerNotFoundException extends RuntimeException {}

    /**
     * Given a classic set of error attributes, it will determines the to-be-handled
     * exception from the status code.
     *
     * @param attributes Key-value pairs representing the error attributes.
     * @return The mapped exception.
     */
    static Exception refineUnknownException(Map<String, Object> attributes) {
        switch (getStatusCode(attributes)) {
            case 401:
                return new UnauthorizedException();
            case 403:
                return new ForbiddenException();
            case 404:
                return new HandlerNotFoundException();
            default:
                return new IllegalStateException("The exception is null: " + attributes);
        }
    }

    /**
     * Extracts the status code from error attributes.
     *
     * @param attributes The error attributes.
     * @return Extracted status code.
     */
    private static int getStatusCode(Map<String, Object> attributes) {
        try {
            return (Integer) attributes.getOrDefault("status", 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
