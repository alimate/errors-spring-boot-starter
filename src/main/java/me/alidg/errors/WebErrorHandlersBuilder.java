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

/**
 * Builder for {@link WebErrorHandlers} instance.
 *
 * @author zarebski.m
 */
public final class WebErrorHandlersBuilder {
    private final MessageSource messageSource;
    private final List<WebErrorHandler> webErrorHandlers = new ArrayList<>();
    private final List<WebErrorHandlerPostProcessor> webErrorHandlerPostProcessors = new ArrayList<>();

    private ErrorsProperties errorsProperties = new ErrorsProperties();
    private WebErrorHandler defaultWebErrorHandler = LastResortWebErrorHandler.INSTANCE;
    private ExceptionRefiner exceptionRefiner = new ExceptionRefiner.NoOp();
    private ExceptionLogger exceptionLogger = new ExceptionLogger.NoOp();
    private FingerprintProvider fingerprintProvider = new UuidFingerprintProvider();

    /**
     * @param messageSource Non-null instance of {@link MessageSource}.
     */
    WebErrorHandlersBuilder(@NonNull MessageSource messageSource) {
        this.messageSource = requireNonNull(messageSource, "messageSource");
    }

    /**
     * @param errorsProperties Non-null instance of {@link ErrorsProperties}.
     *         If not provided, default-constructed {@link ErrorsProperties} will be used.
     *
     * @return This builder.
     *
     * @see #errorsProperties
     */
    public WebErrorHandlersBuilder withErrorsProperties(@NonNull ErrorsProperties errorsProperties) {
        this.errorsProperties = requireNonNull(errorsProperties, "errorsProperties");
        return this;
    }

    /**
     * @param webErrorHandlers Non-null collection of {@link WebErrorHandler} instances.
     *
     * @return This builder.
     *
     * @see #webErrorHandlers
     */
    public WebErrorHandlersBuilder withErrorHandlers(@NonNull Collection<WebErrorHandler> webErrorHandlers) {
        this.webErrorHandlers.addAll(requireNonNull(webErrorHandlers, "webErrorHandlers"));
        return this;
    }

    /**
     * @param webErrorHandlers Varargs array of {@link WebErrorHandler} instances.
     *
     * @return This builder.
     *
     * @see #webErrorHandlers
     */
    public WebErrorHandlersBuilder withErrorHandlers(@NonNull WebErrorHandler... webErrorHandlers) {
        return withErrorHandlers(Arrays.asList(requireNonNull(webErrorHandlers, "webErrorHandlers")));
    }

    /**
     * @param defaultWebErrorHandler Non-null instance of {@link WebErrorHandler}.
     *         If not provided, {@link LastResortWebErrorHandler} will be used.
     *
     * @return This builder.
     *
     * @see #defaultWebErrorHandler
     */
    public WebErrorHandlersBuilder withDefaultWebErrorHandler(@NonNull WebErrorHandler defaultWebErrorHandler) {
        this.defaultWebErrorHandler = requireNonNull(defaultWebErrorHandler, "defaultWebErrorHandler");
        return this;
    }

    /**
     * @param exceptionRefiner Non-null instance of {@link ExceptionRefiner}.
     *         If not provided, {@link ExceptionRefiner.NoOp} will be used.
     *
     * @return This builder.
     *
     * @see #exceptionRefiner
     */
    public WebErrorHandlersBuilder withExceptionRefiner(@NonNull ExceptionRefiner exceptionRefiner) {
        this.exceptionRefiner = requireNonNull(exceptionRefiner, "exceptionRefiner");
        return this;
    }

    /**
     * @param exceptionLogger Non-null instance of {@link ExceptionLogger}.
     *         If not provided, {@link ExceptionLogger.NoOp} will be used.
     *
     * @return This builder.
     *
     * @see #exceptionLogger
     */
    public WebErrorHandlersBuilder withExceptionLogger(@NonNull ExceptionLogger exceptionLogger) {
        this.exceptionLogger = requireNonNull(exceptionLogger, "exceptionLogger");
        return this;
    }

    /**
     * @param webErrorHandlerPostProcessors Non-null collection of {@link WebErrorHandlerPostProcessor} instances.
     *
     * @return This builder.
     *
     * @see #webErrorHandlerPostProcessors
     */
    public WebErrorHandlersBuilder withErrorActionExecutors(@NonNull Collection<WebErrorHandlerPostProcessor> webErrorHandlerPostProcessors) {
        this.webErrorHandlerPostProcessors.addAll(requireNonNull(webErrorHandlerPostProcessors, "webErrorHandlerPostProcessors"));
        return this;
    }

    /**
     * @param webErrorHandlerPostProcessors Varargs array of {@link WebErrorHandlerPostProcessor} instances.
     *
     * @return This builder.
     *
     * @see #webErrorHandlerPostProcessors
     */
    public WebErrorHandlersBuilder withErrorActionExecutors(@NonNull WebErrorHandlerPostProcessor... webErrorHandlerPostProcessors) {
        return withErrorActionExecutors(Arrays.asList(requireNonNull(webErrorHandlerPostProcessors, "webErrorHandlerPostProcessors")));
    }

    /**
     * @param fingerprintProvider Non-null instance of {@link FingerprintProvider}.
     *         If not provided, {@link UuidFingerprintProvider} will be used.
     *
     * @return This builder.
     *
     * @see #exceptionLogger
     */
    public WebErrorHandlersBuilder withFingerprintProvider(@NonNull FingerprintProvider fingerprintProvider) {
        this.fingerprintProvider = requireNonNull(fingerprintProvider, "fingerprintProvider");
        return this;
    }

    /**
     * @return Instance of {@link WebErrorHandlers}.
     */
    @NonNull
    public WebErrorHandlers build() {
        return new WebErrorHandlers(messageSource, webErrorHandlers, defaultWebErrorHandler,
                exceptionRefiner, exceptionLogger, webErrorHandlerPostProcessors, fingerprintProvider, errorsProperties);
    }
}
