package me.alidg.errors;

import org.springframework.lang.NonNull;

/**
 * Post processor/action executor  for {@link HttpError}s. Every post processor registered
 * as Spring bean will be called after error is prepared.
 *
 * <p>
 * This might be considered as richer and more flexible alternative to {@link ExceptionLogger}.
 * The former logs at the beginning of error handling and the exception is the only parameter
 * there. This post processor takes as its parameter rich {@link HttpError} parameter, which
 * contains original exception along with other goodies. Moreover, more than single post processor
 * can be declared.
 *
 * @author zarebski-m
 * @implNote Ensure the implementation doesn't throw.
 */
public interface WebErrorHandlerPostProcessor {

    /**
     * The logic to execute when we finished to handle the exception and just before returning the
     * result.
     *
     * @param error HttpError to act upon.
     */
    void process(@NonNull HttpError error);
}
