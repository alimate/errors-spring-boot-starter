package me.alidg.errors;

import org.springframework.lang.Nullable;

/**
 * Defines a contract to handle exceptions in the Web layer and convert them to appropriate
 * error codes with meaningful status codes. This is a contract for exception handling and
 * nobody's gonna handle the exceptions thrown by the contract methods, so do not throw
 * exceptions in your method implementations.
 *
 * @author Ali Dehghani
 */
public interface WebErrorHandler {

    /**
     * Determines whether this particular implementation can handle the given exception
     * or not.
     *
     * @param exception The exception to examine.
     * @return {@code true} if this implementation can handle the exception, {@code false}
     *         otherwise.
     */
    boolean canHandle(@Nullable Throwable exception);

    /**
     * Handles the given exception and returns an instance of {@link HandledException}.
     * This method should be called iff the call to {@link #canHandle(Throwable)} for
     * the same exception returns {@code true}.
     *
     * @param exception The exception to handle.
     * @return A set of error codes.
     */
    HandledException handle(@Nullable Throwable exception);
}
