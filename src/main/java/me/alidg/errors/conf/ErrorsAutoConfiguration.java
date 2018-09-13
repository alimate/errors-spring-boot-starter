package me.alidg.errors.conf;

import me.alidg.errors.WebErrorHandler;
import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.impl.AnnotatedWebErrorHandler;
import me.alidg.errors.impl.SpringValidationWebErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.MessageInterpolator;
import java.util.*;

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
 * <h3>Default Fallback Web Error Handlers</h3>
 * While handling a particular exception, each registered {@link WebErrorHandler} in {@link WebErrorHandlers}
 * would be consulted one after another (Depending on their priority). If all of the registered handlers
 * refuse to handle the exception, then a default fallback {@link WebErrorHandler} should handle the exception.
 * By default, {@link WebErrorHandlers} use the {@link me.alidg.errors.impl.LastResortWebErrorHandler} as the
 * default handler. You can replace this handler by providing a {@link WebErrorHandler} and register it with
 * a bean named {@code defaultWebErrorHandler}.
 *
 * @author Ali Dehghani
 * @see WebErrorHandler
 * @see WebErrorHandlers
 * @see me.alidg.errors.impl.LastResortWebErrorHandler
 */
@ConditionalOnWebApplication
@AutoConfigureAfter({MessageSourceAutoConfiguration.class, WebMvcAutoConfiguration.class})
public class ErrorsAutoConfiguration {

    /**
     * Built-in {@link WebErrorHandler}s which would be on top of all other {@link WebErrorHandler}s
     * and will be consulted before any other implementations for error handling.
     */
    private static final List<WebErrorHandler> BUILT_IN_HANDLERS = Arrays.asList(
            new SpringValidationWebErrorHandler(),
            new AnnotatedWebErrorHandler()
    );

    /**
     * Registers a bean of type {@link WebErrorHandlers} (If not provided by the user) filled with a set of
     * built-in {@link WebErrorHandler}s, a set of custom {@link WebErrorHandler}s and a default fallback
     * {@link WebErrorHandler}.
     *
     * @param messageSource Will be used for error code to error message translation.
     * @param customHandlers Optional custom {@link WebErrorHandler}s.
     * @param defaultWebErrorHandler A default {@link WebErrorHandler} to be used as the fallback error handler.
     * @return The expected {@link WebErrorHandlers}.
     */
    @Bean
    @ConditionalOnMissingBean
    public WebErrorHandlers webErrorHandlers(MessageSource messageSource,
                                             @Autowired(required = false) List<WebErrorHandler> customHandlers,
                                             @Qualifier("defaultWebErrorHandler") @Autowired(required = false) WebErrorHandler defaultWebErrorHandler) {

        List<WebErrorHandler> handlers = new ArrayList<>(BUILT_IN_HANDLERS);
        if (customHandlers != null && !customHandlers.isEmpty()) {
            customHandlers.remove(defaultWebErrorHandler);
            customHandlers.removeIf(Objects::isNull);
            customHandlers.sort(AnnotationAwareOrderComparator.INSTANCE);

            handlers.addAll(customHandlers);
        }

        return new WebErrorHandlers(messageSource, handlers, defaultWebErrorHandler);
    }

    /**
     * Registers a new validator that does not interpolate messages.
     *
     * @return The custom validator.
     */
    @Bean
    public Validator defaultValidator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setMessageInterpolator(new NoOpMessageInterpolator());

        return factoryBean;
    }

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
