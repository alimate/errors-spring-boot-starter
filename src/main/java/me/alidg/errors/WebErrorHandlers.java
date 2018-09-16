package me.alidg.errors;

import me.alidg.errors.HttpError.CodedMessage;
import me.alidg.errors.handlers.LastResortWebErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Locale;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A factory over {@link WebErrorHandler} implementations. The factory would query all the
 * implementations to find an appropriate exception handler to handle a particular exception.
 *
 * <h3>Code to Message Translation</h3>
 * The {@link WebErrorHandlers} is also responsible for Error Code to Error Message translation
 * and i18n (RIP SRP!). In order to fulfill this requirement, we need an instance of {@link MessageSource}
 * to translate error codes to error messages.
 *
 * <h3>Default Exception Handler</h3>
 * By default, when we couldn't find any {@link WebErrorHandler} implementation to handle the
 * exception, we would use the {@link LastResortWebErrorHandler} as the default exception handler.
 * If you don't like its exception handling approach, consider passing a valid non-null
 * {@link #defaultWebErrorHandler} to the constructor.
 *
 * @author Ali Dehghani
 * @see WebErrorHandler
 * @see HandledException
 * @see LastResortWebErrorHandler
 */
public class WebErrorHandlers {

    /**
     * Plain old logger.
     */
    private static final Logger log = LoggerFactory.getLogger(WebErrorHandlers.class);

    /**
     * Helps us to translate error codes to possibly localized error messages.
     */
    private final MessageSource messageSource;

    /**
     * Collection of {@link WebErrorHandler} implementations. The {@link WebErrorHandlers}
     * would choose at most one implementation from this collection to delegate the exception
     * handling task. This collection can't be null or empty.
     */
    private final List<WebErrorHandler> implementations;

    /**
     * This is the fallback error handler which will be used when all other {@link WebErrorHandler}
     * implementations refuse to handle the exception. By default, the {@link LastResortWebErrorHandler}
     * would be used in such circumstances but you have the option to provide your own custom error
     * handler as the fallback handler.
     */
    private WebErrorHandler defaultWebErrorHandler = LastResortWebErrorHandler.INSTANCE;

    /**
     * To initialize the {@link WebErrorHandlers} instance with a code-to-message translator, a
     * non-empty collection of {@link WebErrorHandler} implementations and an optional fallback
     * error handler.
     *
     * @param messageSource          The code to message translator.
     * @param implementations        Collection of {@link WebErrorHandler} implementations.
     * @param defaultWebErrorHandler Fallback web error handler.
     * @throws NullPointerException     When one of the required parameters is null.
     * @throws IllegalArgumentException When the collection of implementations is empty.
     */
    public WebErrorHandlers(@NonNull MessageSource messageSource,
                            @NonNull List<WebErrorHandler> implementations,
                            @Nullable WebErrorHandler defaultWebErrorHandler) {
        enforcePreconditions(messageSource, implementations);
        this.messageSource = messageSource;
        this.implementations = implementations;
        if (defaultWebErrorHandler != null) this.defaultWebErrorHandler = defaultWebErrorHandler;
    }

    /**
     * Given any {@code exception}, first it would select an appropriate exception handler or
     * falls back to a default handler and then tries to handle the exception using the chosen
     * handler. Then would convert the {@link HandledException} to its corresponding {@link HttpError}.
     *
     * @param exception The exception to handle.
     * @param locale    Will be used to target a specific locale while translating the codes to error
     *                  messages.
     * @return An {@link HttpError} instance containing both error and message combinations and also,
     * the intended HTTP Status Code.
     */
    public HttpError handle(@Nullable Throwable exception, @Nullable Locale locale) {
        if (locale == null) locale = Locale.ROOT;

        log.debug("About to handle an exception", exception);
        WebErrorHandler handler = findHandler(exception);
        log.debug("The '{}' is going to handle the '{}' exception", className(handler), className(exception));

        HandledException handled = handler.handle(exception);
        List<CodedMessage> codeWithMessages = translateErrors(handled, locale);

        return new HttpError(codeWithMessages, handled.getStatusCode());
    }

    private List<CodedMessage> translateErrors(HandledException handled, Locale locale) {
        return handled
                .getErrorCodes()
                .stream()
                .map(code -> withMessage(code, getArgumentsFor(handled, code), locale))
                .collect(toList());
    }

    private void enforcePreconditions(MessageSource messageSource, List<WebErrorHandler> webErrorHandlers) {
        requireNonNull(messageSource, "We need a MessageSource implementation to message translation");
        requireNonNull(webErrorHandlers, "Collection of error handlers is required");
        if (webErrorHandlers.isEmpty())
            throw new IllegalArgumentException("We need at least one error handler");
    }

    private CodedMessage withMessage(String code, Object[] args, Locale locale) {
        try {
            String message = messageSource.getMessage(code, args, locale);
            return new CodedMessage(code, message);
        } catch (Exception e) {
            return new CodedMessage(code, null);
        }
    }

    private WebErrorHandler findHandler(Throwable exception) {
        if (exception == null) return defaultWebErrorHandler;

        return implementations
                .stream()
                .filter(p -> p.canHandle(exception))
                .findFirst()
                .orElse(defaultWebErrorHandler);
    }

    private String className(Object toInspect) {
        if (toInspect == null) return "null";

        return toInspect.getClass().getName();
    }

    private Object[] getArgumentsFor(HandledException handled, String errorCode) {
        return handled.getArguments().getOrDefault(errorCode, emptyList()).toArray();
    }
}
