package me.alidg.errors;

import org.springframework.lang.Nullable;

/**
 * Defines a contract to log the to-be-handled exceptions, that's it!
 *
 * <p>
 * For a richer alternative check {@link WebErrorHandlerPostProcessor}.
 *
 * @implNote Do not throw exceptions in method implementations.
 *
 * @author Ali Dehghani
 */
public interface ExceptionLogger {

    /**
     * Actually logs the exception.
     *
     * <p>Please note that the method parameter is nullable.</p>
     *
     * @param exception The exception to log.
     */
    void log(@Nullable Throwable exception);

    /**
     * Exception logger that does nothing.
     */
    final class NoOp implements ExceptionLogger {
        @Override
        public void log(Throwable exception) {
            // do nothing
        }
    }
}
