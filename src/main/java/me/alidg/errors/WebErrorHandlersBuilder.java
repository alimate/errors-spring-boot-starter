package me.alidg.errors;

import me.alidg.errors.conf.ErrorsProperties;
import me.alidg.errors.fingerprint.UuidFingerprintProvider;
import me.alidg.errors.handlers.LastResortWebErrorHandler;
import org.springframework.context.MessageSource;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static me.alidg.errors.conf.ErrorsProperties.ArgumentExposure.NEVER;

/**
 * A fluent builder responsible for creating {@link WebErrorHandlers} instances.
 *
 * @author zarebski.m
 */
public final class WebErrorHandlersBuilder {

    /**
     * The abstraction responsible for converting error codes to messages.
     */
    private final MessageSource messageSource;

    /**
     * Collection of error handlers to consult when facing a new exception.
     */
    private final List<WebErrorHandler> webErrorHandlers = new ArrayList<>();

    /**
     * Collection of post processors to call after handling any given exception.
     */
    private final List<WebErrorHandlerPostProcessor> webErrorHandlerPostProcessors = new ArrayList<>();

    /**
     * Determines the way we're gonna configure the error handling mechanism.
     */
    private ErrorsProperties errorsProperties = new ErrorsProperties();

    /**
     * Represents the default web error handler we're gonna use when all other handlers
     * refuse to handle an exception.
     */
    private WebErrorHandler defaultWebErrorHandler = LastResortWebErrorHandler.INSTANCE;

    /**
     * Enables us to refine exceptions before kicking off the exception handling procedure.
     */
    private ExceptionRefiner exceptionRefiner = ExceptionRefiner.NoOp.INSTANCE;

    /**
     * Defines a way to logs exceptions before handling them.
     */
    private ExceptionLogger exceptionLogger = ExceptionLogger.NoOp.INSTANCE;

    /**
     * Responsible for calculating error fingerprints for handled exceptions.
     */
    private FingerprintProvider fingerprintProvider = new UuidFingerprintProvider();

    /**
     * Creates a basic instance of the builder.
     *
     * @param messageSource Non-null instance of {@link MessageSource}.
     */
    WebErrorHandlersBuilder(@NonNull MessageSource messageSource) {
        this.messageSource = requireNonNull(messageSource,
                "Message source is required to create WebErrorHandlers instance");
    }

    /**
     * Determines the way we're gonna configure the error handling mechanism.
     *
     * @param errorsProperties Non-null instance of {@link ErrorsProperties}.
     *                         If not provided, default-constructed {@link ErrorsProperties} will be used.
     * @return This builder.
     * @see #errorsProperties
     */
    public WebErrorHandlersBuilder withErrorsProperties(@NonNull ErrorsProperties errorsProperties) {
        this.errorsProperties = requireNonNull(errorsProperties,
                "Error properties is required to create WebErrorHandlers instance");
        if (errorsProperties.getExposeArguments() == null)
            errorsProperties.setExposeArguments(NEVER);

        return this;
    }

    /**
     * Collection of error handlers to consult when facing a new exception.
     *
     * @param webErrorHandlers Non-null collection of {@link WebErrorHandler} instances.
     * @return This builder.
     * @see #webErrorHandlers
     */
    public WebErrorHandlersBuilder withErrorHandlers(@NonNull Collection<WebErrorHandler> webErrorHandlers) {
        this.webErrorHandlers.addAll(requireNonNull(webErrorHandlers,
                "Web error handlers are required to create WebErrorHandlers instance"));

        return this;
    }

    /**
     * Collection of error handlers to consult when facing a new exception.
     *
     * @param webErrorHandlers Varargs array of {@link WebErrorHandler} instances.
     * @return This builder.
     * @see #webErrorHandlers
     */
    public WebErrorHandlersBuilder withErrorHandlers(@NonNull WebErrorHandler... webErrorHandlers) {
        return withErrorHandlers(Arrays.asList(requireNonNull(webErrorHandlers,
                "Web error handlers are required to create WebErrorHandlers instance")));
    }

    /**
     * Represents the default web error handler we're gonna use when all other handlers
     * refuse to handle an exception.
     *
     * @param defaultWebErrorHandler Non-null instance of {@link WebErrorHandler}.
     *                               If not provided, {@link LastResortWebErrorHandler} will be used.
     * @return This builder.
     * @see #defaultWebErrorHandler
     */
    public WebErrorHandlersBuilder withDefaultWebErrorHandler(@NonNull WebErrorHandler defaultWebErrorHandler) {
        this.defaultWebErrorHandler = requireNonNull(defaultWebErrorHandler,
                "Default web error handler is required to create WebErrorHandlers instance");

        return this;
    }

    /**
     * Enables us to refine exceptions before kicking off the exception handling procedure.
     *
     * @param exceptionRefiner Non-null instance of {@link ExceptionRefiner}.
     *                         If not provided, {@link ExceptionRefiner.NoOp} will be used.
     * @return This builder.
     * @see #exceptionRefiner
     */
    public WebErrorHandlersBuilder withExceptionRefiner(@NonNull ExceptionRefiner exceptionRefiner) {
        this.exceptionRefiner = requireNonNull(exceptionRefiner,
                "An exception refiner is required to create WebErrorHandlers instance");

        return this;
    }

    /**
     * Defines a way to logs exceptions before handling them.
     *
     * @param exceptionLogger Non-null instance of {@link ExceptionLogger}.
     *                        If not provided, {@link ExceptionLogger.NoOp} will be used.
     * @return This builder.
     * @see #exceptionLogger
     */
    public WebErrorHandlersBuilder withExceptionLogger(@NonNull ExceptionLogger exceptionLogger) {
        this.exceptionLogger = requireNonNull(exceptionLogger,
                "An exception logger is required to create WebErrorHandlers instance");

        return this;
    }

    /**
     * Collection of post processors to call after handling any given exception.
     *
     * @param webErrorHandlerPostProcessors Non-null collection of {@link WebErrorHandlerPostProcessor} instances.
     * @return This builder.
     * @see #webErrorHandlerPostProcessors
     */
    public WebErrorHandlersBuilder withPostProcessors(@NonNull Collection<WebErrorHandlerPostProcessor> webErrorHandlerPostProcessors) {
        this.webErrorHandlerPostProcessors.addAll(requireNonNull(webErrorHandlerPostProcessors,
                "Post processors are required to create WebErrorHandlers instance"));

        return this;
    }

    /**
     * Collection of post processors to call after handling any given exception.
     *
     * @param webErrorHandlerPostProcessors Varargs array of {@link WebErrorHandlerPostProcessor} instances.
     * @return This builder.
     * @see #webErrorHandlerPostProcessors
     */
    public WebErrorHandlersBuilder withPostProcessors(@NonNull WebErrorHandlerPostProcessor... webErrorHandlerPostProcessors) {
        return withPostProcessors(Arrays.asList(requireNonNull(webErrorHandlerPostProcessors,
                "Post processors are required to create WebErrorHandlers instance")));
    }

    /**
     * Responsible for calculating error fingerprints for handled exceptions.
     *
     * @param fingerprintProvider Non-null instance of {@link FingerprintProvider}.
     *                            If not provided, {@link UuidFingerprintProvider} will be used.
     * @return This builder.
     * @see #exceptionLogger
     */
    public WebErrorHandlersBuilder withFingerprintProvider(@NonNull FingerprintProvider fingerprintProvider) {
        this.fingerprintProvider = requireNonNull(fingerprintProvider,
                "Fingerprint provider is required to create WebErrorHandlers instance");

        return this;
    }

    /**
     * Creates the {@link WebErrorHandlers} instance from the current builder state.
     *
     * @return Instance of {@link WebErrorHandlers}.
     */
    @NonNull
    public WebErrorHandlers build() {
        return new WebErrorHandlers(
                messageSource, webErrorHandlers, defaultWebErrorHandler,
                exceptionRefiner, exceptionLogger, webErrorHandlerPostProcessors,
                fingerprintProvider, errorsProperties
        );
    }
}
