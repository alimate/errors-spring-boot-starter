package me.alidg.errors.handlers;

import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.http.HttpStatus;

import java.util.Collections;

/**
 * The default fallback {@link WebErrorHandler} which will be used when all
 * other registered handlers refuse to handle a particular exception. You can
 * also use a custom implementation of {@link WebErrorHandler} for this scenario.
 *
 * @author Ali Dehghani
 * @see WebErrorHandler
 */
public enum LastResortWebErrorHandler implements WebErrorHandler {

    /**
     * The singleton instance!
     */
    INSTANCE;

    /**
     * This error handler simply returns an error code representing an unknown error.
     */
    public static final String UNKNOWN_ERROR_CODE = "unknown_error";

    /**
     * Since this is the last resort error handler, this value would simply be ignored.
     *
     * @param exception The exception to examine.
     * @return Does not matter what!
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return false;
    }

    /**
     * Always return 500 Internal Error with {@code unknown_error} as the error code with no
     * arguments.
     *
     * @param exception The exception to handle.
     * @return 500 Internal Error with {@code unknown_error} as the error code and no arguments.
     */
    @Override
    public HandledException handle(Throwable exception) {
        return new HandledException(UNKNOWN_ERROR_CODE, HttpStatus.INTERNAL_SERVER_ERROR, Collections.emptyMap());
    }
}
