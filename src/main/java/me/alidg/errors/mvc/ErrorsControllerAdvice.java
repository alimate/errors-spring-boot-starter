package me.alidg.errors.mvc;

import me.alidg.errors.HttpError;
import me.alidg.errors.WebErrorHandlers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Tries its best to catch and handle all exceptions thrown inside the
 * Web layer using {@link WebErrorHandlers} assistance.
 *
 * @author Ali Dehghani
 */
@RestControllerAdvice
public abstract class ErrorsControllerAdvice {

    /**
     * Responsible for handling exceptions and converting them to appropriate {@link HttpError}s.
     */
    private final WebErrorHandlers errorHandlers;

    /**
     * Initializing the rest controller advice by injecting the {@link WebErrorHandlers} bean.
     *
     * @param errorHandlers The exception handler collaborator.
     * @throws NullPointerException When the {@code errorHandlers} is null.
     */
    public ErrorsControllerAdvice(WebErrorHandlers errorHandlers) {
        requireNonNull(errorHandlers, "Error handlers is required");
        this.errorHandlers = errorHandlers;
    }

    /**
     * Catches any exception and converts it to a HTTP response with appropriate status
     * code and error code-message combinations.
     *
     * @param exception The caught exception.
     * @param locale Determines the locale for message translation.
     * @return A HTTP response with appropriate error body and status code.
     */
    @ExceptionHandler
    public ResponseEntity<HttpError> handleException(Throwable exception, Locale locale) {
        HttpError httpError = errorHandlers.handle(exception, locale);

        return ResponseEntity.status(httpError.getHttpStatus()).body(httpError);
    }
}
