package me.alidg.errors.adapter.attributes;

import me.alidg.errors.HttpError;
import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.adapter.HttpErrorAttributesAdapter;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Custom implementation of {@link org.springframework.boot.web.reactive.error.ErrorAttributes}
 * which adapts the handled {@link HttpError} to a Spring Boot's compatible error attributes
 * representation.
 *
 * @author Ali Dehghani
 */
public class ReactiveErrorAttributes extends DefaultErrorAttributes {

    /**
     * The facade responsible for catching all exceptions and delegating to appropriate
     * exception handlers.
     */
    private final WebErrorHandlers webErrorHandlers;

    /**
     * Adapts the internal representation of errors to a custom representation.
     */
    private final HttpErrorAttributesAdapter httpErrorAttributesAdapter;

    /**
     * Initializes the error attributes with required dependencies.
     *
     * @param webErrorHandlers           To handle exceptions.
     * @param httpErrorAttributesAdapter To adapt our representation of an error to Spring Boot's representation.
     * @throws NullPointerException When one of the required parameters is null.
     */
    public ReactiveErrorAttributes(WebErrorHandlers webErrorHandlers,
                                   HttpErrorAttributesAdapter httpErrorAttributesAdapter) {
        super(true);
        this.webErrorHandlers = requireNonNull(webErrorHandlers, "Web error handlers is required");
        this.httpErrorAttributesAdapter = requireNonNull(httpErrorAttributesAdapter, "Adapter is required");
    }

    /**
     * Handles the exception by delegating it to the {@link #webErrorHandlers} and then adapting
     * the representation.
     *
     * @param request           The source request.
     * @param includeStackTrace whether to include the error stacktrace information.
     * @return A map of error attributes
     */
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        Map<String, Object> attributes = super.getErrorAttributes(request, includeStackTrace);
        Throwable exception = getError(request);
        if (exception == null) exception = Exceptions.refineUnknownException(attributes);

        HttpError httpError = webErrorHandlers.handle(exception, request, LocaleContextHolder.getLocale());
        Map<String, Object> adapted = httpErrorAttributesAdapter.adapt(httpError);
        adapted.put("status", httpError.getHttpStatus().value());

        return adapted;
    }
}
