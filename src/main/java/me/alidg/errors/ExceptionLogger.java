package me.alidg.errors;

import org.springframework.lang.Nullable;

/**
 * Defines a contract to log the to-be-handled exceptions, that's it!
 *
 * <p>
 * For a richer alternative check {@link WebErrorHandlerPostProcessor}.
 *
 * @author Ali Dehghani
 * @implNote Do not throw exceptions in method implementations.
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
     * A NoOp implementation for {@link ExceptionLogger}s.
     *
     * @author zarebski-m
     */
    enum NoOp implements ExceptionLogger {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * Does nothing special!
         *
         * @param exception The exception to log.
         */
        @Override
        public void log(Throwable exception) {
        }
    }
}
