package me.alidg.errors.handlers;

import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.server.*;

import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static me.alidg.errors.handlers.LastResortWebErrorHandler.UNKNOWN_ERROR_CODE;
import static me.alidg.errors.handlers.ServletWebErrorHandler.METHOD_NOT_ALLOWED;
import static me.alidg.errors.handlers.ServletWebErrorHandler.NOT_ACCEPTABLE;
import static me.alidg.errors.handlers.ServletWebErrorHandler.*;
import static org.springframework.http.HttpStatus.*;

/**
 * {@link WebErrorHandler} implementation expert at handling exceptions of type
 * {@link ResponseStatusException}.
 *
 * @author Ali Dehghani
 */
public class ResponseStatusWebErrorHandler implements WebErrorHandler {

    /**
     * Only can handle exceptions of type {@link ResponseStatusException}.
     *
     * @param exception The exception to examine.
     * @return {@code true} for {@link ResponseStatusException}s, {@code false} for others.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ResponseStatusException;
    }

    /**
     * Handle each subtype of {@link ResponseStatusException} class in its own unique and appropriate way.
     *
     * @param exception The exception to handle.
     * @return A handled exception.
     */
    @NonNull
    @Override
    public HandledException handle(Throwable exception) {
        if (exception instanceof MediaTypeNotSupportedStatusException) {
            Map<String, List<?>> arguments =
                    arguments(NOT_SUPPORTED, ((MediaTypeNotSupportedStatusException) exception).getSupportedMediaTypes());
            return new HandledException(NOT_SUPPORTED, UNSUPPORTED_MEDIA_TYPE, arguments);
        }

        if (exception instanceof MethodNotAllowedException) {
            List<String> arguments = singletonList(((MethodNotAllowedException) exception).getHttpMethod());
            return new HandledException(METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED, arguments(METHOD_NOT_ALLOWED, arguments));
        }

        if (exception instanceof NotAcceptableStatusException) {
            List<List<MediaType>> arguments = singletonList(((NotAcceptableStatusException) exception).getSupportedMediaTypes());
            return new HandledException(NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE, arguments(NOT_ACCEPTABLE, arguments));
        }

        if (exception instanceof ServerWebInputException) {
            return new HandledException(INVALID_OR_MISSING_BODY, BAD_REQUEST, null);
        }

        if (exception instanceof ResponseStatusException) {
            HttpStatus status = ((ResponseStatusException) exception).getStatus();
            return new HandledException(UNKNOWN_ERROR_CODE, status, null);
        }

        return new HandledException(UNKNOWN_ERROR_CODE, INTERNAL_SERVER_ERROR, null);
    }

    private Map<String, List<?>> arguments(String code, List<?> arguments) {
        return singletonMap(code, arguments);
    }
}
