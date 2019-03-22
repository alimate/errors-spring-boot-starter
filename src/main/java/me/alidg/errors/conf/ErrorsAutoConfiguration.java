package me.alidg.errors.conf;

import me.alidg.errors.ExceptionLogger;
import me.alidg.errors.ExceptionRefiner;
import me.alidg.errors.FingerprintProvider;
import me.alidg.errors.WebErrorHandler;
import me.alidg.errors.WebErrorHandlerPostProcessor;
import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.WebErrorHandlersBuilder;
import me.alidg.errors.adapter.DefaultHttpErrorAttributesAdapter;
import me.alidg.errors.adapter.HttpErrorAttributesAdapter;
import me.alidg.errors.fingerprint.UuidFingerprintProvider;
import me.alidg.errors.handlers.AnnotatedWebErrorHandler;
import me.alidg.errors.handlers.ConstraintViolationWebErrorHandler;
import me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler;
import me.alidg.errors.handlers.ResponseStatusWebErrorHandler;
import me.alidg.errors.handlers.ServletWebErrorHandler;
import me.alidg.errors.handlers.SpringSecurityWebErrorHandler;
import me.alidg.errors.handlers.SpringValidationWebErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.WebApplicationContext;

import javax.validation.MessageInterpolator;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * Auto-configuration responsible for registering a {@link WebErrorHandlers} filled with
 * builtin, custom and default fallback {@link WebErrorHandler}s.
 *
 * <h3>Builtin Web Error Handlers</h3>
 * Built in {@link WebErrorHandler}s are those we provided out of the box. It's highly recommended
 * to use these implementations with most possible priority, as we did in this auto-configuration.
 *
 * <h3>Custom Web Error Handlers</h3>
 * You can also provide your own custom {@link WebErrorHandler} implementations. Just implement the
 * {@link WebErrorHandler} interface and register it as Spring Bean. If you're willing to prioritize
 * your implementations, use the Spring {@link org.springframework.core.annotation.Order} annotation
 * to specify the priority requirements of the bean.
 * <p><b>Please Note that</b> your custom handlers would be registered after the built-in ones. If you're
 * not OK with that, you can always discard this auto-configuration by registering your own
 * {@link WebErrorHandlers} factory bean.</p>
 *
 * <h3>Default Fallback Web Error Handler</h3>
 * While handling a particular exception, each registered {@link WebErrorHandler} in {@link WebErrorHandlers}
 * would be consulted one after another (Depending on their priority). If all of the registered handlers
 * refuse to handle the exception, then a default fallback {@link WebErrorHandler} should handle the exception.
 * By default, {@link WebErrorHandlers} use the {@link me.alidg.errors.handlers.LastResortWebErrorHandler} as the
 * default handler. You can replace this handler by providing a {@link WebErrorHandler} and register it with
 * a bean named {@code defaultWebErrorHandler}.
 *
 * @author Ali Dehghani
 * @see WebErrorHandler
 * @see WebErrorHandlers
 * @see me.alidg.errors.handlers.LastResortWebErrorHandler
 */
@ConditionalOnWebApplication
@AutoConfigureAfter({MessageSourceAutoConfiguration.class, WebMvcAutoConfiguration.class})
@EnableConfigurationProperties(ErrorsProperties.class)
public class ErrorsAutoConfiguration {

    /**
     * Built-in {@link WebErrorHandler}s which would be on top of all other {@link WebErrorHandler}s
     * and will be consulted before any other implementations for error handling.
     */
    private static final List<WebErrorHandler> BUILT_IN_HANDLERS = Arrays.asList(
            new SpringValidationWebErrorHandler(),
            new ConstraintViolationWebErrorHandler(),
            new AnnotatedWebErrorHandler()
    );

    /**
     * Registers a bean of type {@link WebErrorHandlers} (If not provided by the user) filled with a set of
     * built-in {@link WebErrorHandler}s, a set of custom {@link WebErrorHandler}s and a default fallback
     * {@link WebErrorHandler}.
     *
     * @param messageSource                 Will be used for error code to error message translation.
     * @param customHandlers                Optional custom {@link WebErrorHandler}s.
     * @param defaultWebErrorHandler        A default {@link WebErrorHandler} to be used as the fallback error handler.
     * @param exceptionRefiner              To refine exceptions before handling them.
     * @param exceptionLogger               To log exceptions.
     * @param webErrorHandlerPostProcessors Post processors to execute after we handled the exception.
     * @param fingerprintProvider           To generate unique fingerprints for handled exceptions.
     * @param errorsProperties              Configuration properties bean.
     * @param context                       To tell Servlet or Reactive stacks apart.
     * @return The expected {@link WebErrorHandlers}.
     */
    @Bean
    @ConditionalOnMissingBean
    public WebErrorHandlers webErrorHandlers(MessageSource messageSource,
                                             List<WebErrorHandler> customHandlers,
                                             @Qualifier("defaultWebErrorHandler") @Autowired(required = false) WebErrorHandler defaultWebErrorHandler,
                                             ExceptionRefiner exceptionRefiner,
                                             ExceptionLogger exceptionLogger,
                                             List<WebErrorHandlerPostProcessor> webErrorHandlerPostProcessors,
                                             FingerprintProvider fingerprintProvider,
                                             ErrorsProperties errorsProperties,
                                             ApplicationContext context) {

        List<WebErrorHandler> handlers = new ArrayList<>(BUILT_IN_HANDLERS);
        if (isServletApplication(context)) handlers.add(new ServletWebErrorHandler());

        if (customHandlers != null && !customHandlers.isEmpty()) {
            customHandlers.remove(defaultWebErrorHandler);
            customHandlers.removeIf(Objects::isNull);
            customHandlers.sort(AnnotationAwareOrderComparator.INSTANCE);

            handlers.addAll(customHandlers);
        }

        WebErrorHandlersBuilder builder = WebErrorHandlers.builder(messageSource)
                .withErrorsProperties(errorsProperties)
                .withErrorHandlers(handlers)
                .withExceptionRefiner(exceptionRefiner)
                .withExceptionLogger(exceptionLogger)
                .withFingerprintProvider(fingerprintProvider);

        if (defaultWebErrorHandler != null) builder.withDefaultWebErrorHandler(defaultWebErrorHandler);
        if (webErrorHandlerPostProcessors != null) builder.withPostProcessors(webErrorHandlerPostProcessors);

        return builder.build();
    }

    /**
     * Registers a new validator that does not interpolate messages.
     *
     * @return The custom validator.
     */
    @Bean({"mvcValidator", "defaultValidator"})
    @ConditionalOnBean(WebErrorHandlers.class)
    public Validator validator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setMessageInterpolator(new NoOpMessageInterpolator());

        return factoryBean;
    }

    /**
     * In the absence of a bean of type {@link HttpErrorAttributesAdapter}, registers the default
     * implementation of {@link HttpErrorAttributesAdapter} as a bean, to adapt our
     * {@link me.alidg.errors.HttpError} to Spring's {@link ErrorAttributes} abstraction.
     *
     * @param errorsProperties Configuration properties bean.
     *
     * @return The to-be-registered {@link HttpErrorAttributesAdapter}.
     */
    @Bean
    @ConditionalOnBean(WebErrorHandlers.class)
    @ConditionalOnMissingBean(HttpErrorAttributesAdapter.class)
    public HttpErrorAttributesAdapter httpErrorAttributesAdapter(ErrorsProperties errorsProperties) {
        return new DefaultHttpErrorAttributesAdapter(errorsProperties);
    }

    /**
     * Registers a {@link WebErrorHandler} bean to handle Spring Security specific exceptions when
     * Spring Security's jar file is present on the classpath.
     *
     * @return A web error handler for Spring Security exceptions.
     */
    @Bean
    @ConditionalOnBean(WebErrorHandlers.class)
    @ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
    public SpringSecurityWebErrorHandler springSecurityWebErrorHandler() {
        return new SpringSecurityWebErrorHandler();
    }

    /**
     * Registers a {@link WebErrorHandler} to handle new Servlet exceptions defined in Spring Framework 5.1.
     * This handler would be registered iff we're using Spring Boot 2.1.0+.
     *
     * @return A web error handler to handle a few new servlet exceptions.
     */
    @Bean
    @ConditionalOnBean(WebErrorHandlers.class)
    @ConditionalOnWebApplication(type = SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.bind.MissingRequestHeaderException")
    public MissingRequestParametersWebErrorHandler missingRequestParametersWebErrorHandler() {
        return new MissingRequestParametersWebErrorHandler();
    }

    /**
     * Registers a handler expert at handling all possible
     * {@link org.springframework.web.server.ResponseStatusException}s.
     *
     * @return A web error handler to handle a set of brand new exceptions defined in Spring 5.x.
     */
    @Bean
    @ConditionalOnBean(WebErrorHandlers.class)
    @ConditionalOnClass(name = "org.springframework.web.server.ResponseStatusException")
    public ResponseStatusWebErrorHandler responseStatusWebErrorHandler() {
        return new ResponseStatusWebErrorHandler();
    }

    /**
     * Registers an empty {@link ExceptionRefiner} in the absence of a custom refiner.
     *
     * @return A no-op exception refiner.
     */
    @Bean
    @ConditionalOnBean(WebErrorHandlers.class)
    @ConditionalOnMissingBean
    ExceptionRefiner exceptionRefiner() {
        return new ExceptionRefiner.NoOp();
    }

    /**
     * Registers an empty {@link ExceptionLogger} in the absence of a custom logger.
     *
     * @return A no-op exception logger.
     */
    @Bean
    @ConditionalOnBean(WebErrorHandlers.class)
    @ConditionalOnMissingBean
    ExceptionLogger exceptionLogger() {
        return new ExceptionLogger.NoOp();
    }

    /**
     * Registers a very simple UUID based {@link FingerprintProvider} in the absence of a custom provider.
     *
     * @return The UUID based fingerprint provider.
     */
    @Bean
    @ConditionalOnBean(WebErrorHandlers.class)
    @ConditionalOnMissingBean
    public FingerprintProvider fingerprintProvider() {
        return new UuidFingerprintProvider();
    }

    /**
     * @param context The application context.
     * @return {@code true} if this a traditional web application, not a reactive one.
     */
    private boolean isServletApplication(ApplicationContext context) {
        return context instanceof WebApplicationContext;
    }

    /**
     * A No Op {@link MessageInterpolator} which does not interpolate the {@code ${...}} codes
     * to messages. We're switching off the Bean Validation message interpolator in favor of
     * Spring's {@link MessageSource}.
     */
    private static class NoOpMessageInterpolator implements MessageInterpolator {

        @Override
        public String interpolate(String messageTemplate, Context context) {
            return messageTemplate;
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale locale) {
            return messageTemplate;
        }
    }
}
