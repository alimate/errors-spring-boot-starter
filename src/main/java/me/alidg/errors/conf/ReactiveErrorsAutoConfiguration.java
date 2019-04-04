package me.alidg.errors.conf;

import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.adapter.HttpErrorAttributesAdapter;
import me.alidg.errors.adapter.attributes.ReactiveErrorAttributes;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

/**
 * Encapsulates the reactive parts of the errors auto configurations.
 *
 * @author Ali Dehghani
 */
@ConditionalOnWebApplication(type = REACTIVE)
@AutoConfigureAfter(ErrorsAutoConfiguration.class)
@AutoConfigureBefore(ErrorWebFluxAutoConfiguration.class)
public class ReactiveErrorsAutoConfiguration {

    /**
     * A custom web error attributes responsible for converting the exceptions to a map of
     * attributes.
     *
     * @param webErrorHandlers           Performing the actual exception handling.
     * @param httpErrorAttributesAdapter Adapter for error representation.
     * @return The to-be-registered web error attributes.
     */
    @Bean
    @ConditionalOnBean(WebErrorHandlers.class)
    public ErrorAttributes errorAttributes(WebErrorHandlers webErrorHandlers,
                                           HttpErrorAttributesAdapter httpErrorAttributesAdapter) {
        return new ReactiveErrorAttributes(webErrorHandlers, httpErrorAttributesAdapter);
    }
}
