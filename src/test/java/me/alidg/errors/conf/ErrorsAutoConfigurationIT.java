package me.alidg.errors.conf;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.adapter.DefaultHttpErrorAttributesAdapter;
import me.alidg.errors.adapter.HttpErrorAttributesAdapter;
import me.alidg.errors.conf.ErrorsProperties.ArgumentExposure;
import me.alidg.errors.handlers.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.lang.reflect.Field;
import java.util.List;

import static java.util.Collections.singletonMap;
import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ErrorsAutoConfiguration} configuration.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class ErrorsAutoConfigurationIT {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ErrorsAutoConfiguration.class,
            ServletErrorsAutoConfiguration.class
        ));

    @Test
    public void whenAnotherWebErrorHandlersRegistered_TheDefaultOneShouldBeDiscarded() {
        contextRunner.withUserConfiguration(CustomWebErrorHandlers.class).run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            assertThat(errorHandlers).isNotNull();

            // Web Error Handlers
            List<WebErrorHandler> implementations = getImplementations(errorHandlers);
            assertImplementations(implementations, 1, First.class);

            // Default Error Handler
            WebErrorHandler defaultHandler = getDefaultHandler(errorHandlers);
            assertDefaultHandler(defaultHandler, Sec.class);
        });
    }

    @Test
    public void byDefault_TheAutoConfigurationShouldRegisterBuiltinAndDefaultHandlers() {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            assertThat(errorHandlers).isNotNull();

            // Web Error Handlers
            List<WebErrorHandler> implementations = getImplementations(errorHandlers);
            assertImplementations(implementations, 8,
                SpringValidationWebErrorHandler.class, ConstraintViolationWebErrorHandler.class,
                AnnotatedWebErrorHandler.class, TypeMismatchWebErrorHandler.class,
                ServletWebErrorHandler.class, SpringSecurityWebErrorHandler.class,
                MissingRequestParametersWebErrorHandler.class, ResponseStatusWebErrorHandler.class
            );

            // Default Error Handler
            WebErrorHandler defaultHandler = getDefaultHandler(errorHandlers);
            assertDefaultHandler(defaultHandler, LastResortWebErrorHandler.class);
        });
    }

    @Test
    public void customHandlers_ShouldBeRegisteredAfterBuiltinHandlers() {
        contextRunner.withUserConfiguration(CustomHandlers.class).run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            assertThat(errorHandlers).isNotNull();

            // Web Error Handlers
            List<WebErrorHandler> implementations = getImplementations(errorHandlers);
            assertImplementations(implementations, 10,
                SpringValidationWebErrorHandler.class, ConstraintViolationWebErrorHandler.class,
                AnnotatedWebErrorHandler.class, TypeMismatchWebErrorHandler.class, ServletWebErrorHandler.class,
                Sec.class, First.class, SpringSecurityWebErrorHandler.class,
                MissingRequestParametersWebErrorHandler.class, ResponseStatusWebErrorHandler.class);

            // Default Error Handler
            WebErrorHandler defaultHandler = getDefaultHandler(errorHandlers);
            assertDefaultHandler(defaultHandler, LastResortWebErrorHandler.class);
        });
    }

    @Test
    public void defaultHandler_ShouldReplaceTheDefaultWebErrorHandler() {
        contextRunner.withUserConfiguration(DefaultHandler.class).run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            assertThat(errorHandlers).isNotNull();

            // Web Error Handlers
            List<WebErrorHandler> implementations = getImplementations(errorHandlers);
            assertImplementations(implementations, 8,
                SpringValidationWebErrorHandler.class, ConstraintViolationWebErrorHandler.class,
                AnnotatedWebErrorHandler.class, TypeMismatchWebErrorHandler.class, ServletWebErrorHandler.class,
                SpringSecurityWebErrorHandler.class, MissingRequestParametersWebErrorHandler.class,
                ResponseStatusWebErrorHandler.class
            );

            // Default Error Handler
            WebErrorHandler defaultHandler = getDefaultHandler(errorHandlers);
            assertDefaultHandler(defaultHandler, Default.class);
        });
    }

    @Test
    public void customHandlersWithDefault_ShouldBothBeRegisteredAfterBuiltinOnesAndReplaceDefaultHandler() {
        contextRunner.withUserConfiguration(DefaultHandler.class, CustomHandlers.class).run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            assertThat(errorHandlers).isNotNull();

            // Web Error Handlers
            List<WebErrorHandler> implementations = getImplementations(errorHandlers);
            assertImplementations(implementations, 10,
                SpringValidationWebErrorHandler.class, ConstraintViolationWebErrorHandler.class,
                AnnotatedWebErrorHandler.class, TypeMismatchWebErrorHandler.class, ServletWebErrorHandler.class,
                Sec.class, First.class, SpringSecurityWebErrorHandler.class,
                MissingRequestParametersWebErrorHandler.class, ResponseStatusWebErrorHandler.class);

            // Default Error Handler
            WebErrorHandler defaultHandler = getDefaultHandler(errorHandlers);
            assertDefaultHandler(defaultHandler, Default.class);
        });
    }

    @Test
    public void withoutCustomAdapter_TheDefaultHttpErrorAdapterShouldBeRegistered() {
        contextRunner.run(ctx -> {
            HttpErrorAttributesAdapter adapter = ctx.getBean(HttpErrorAttributesAdapter.class);

            assertThat(adapter).isInstanceOf(DefaultHttpErrorAttributesAdapter.class);
        });
    }

    @Test
    public void withCustomAdapter_TheDefaultHttpErrorAdapterShouldBeDiscardedInFavorOfTheCustomOne() {
        contextRunner.withUserConfiguration(CustomAdapter.class).run(ctx -> {
            HttpErrorAttributesAdapter adapter = ctx.getBean(HttpErrorAttributesAdapter.class);

            assertThat(adapter).isNotInstanceOf(DefaultHttpErrorAttributesAdapter.class);
        });
    }

    @Test
    public void withProperties_ErrorsPropertiesBeanIsLoaded() {
        contextRunner.run(ctx -> {
            ErrorsProperties properties = ctx.getBean(ErrorsProperties.class);

            assertThat(properties).isNotNull();
            assertThat(properties.getExposeArguments()).isEqualTo(ArgumentExposure.NEVER);
        });
    }

    @Test
    @Parameters(method = "provideExposures")
    public void withProperties_ErrorsPropertiesBeanIsConfigurable(String value, ArgumentExposure expected) {
        contextRunner.withPropertyValues("errors.expose-arguments=" + value).run(ctx -> {
            ErrorsProperties properties = ctx.getBean(ErrorsProperties.class);

            assertThat(properties).isNotNull();
            assertThat(properties.getExposeArguments()).isEqualTo(expected);
        });
    }

    private Object[] provideExposures() {
        return p(
            p("ALWAYS¨", ArgumentExposure.ALWAYS),
            p("always¨", ArgumentExposure.ALWAYS),
            p("NON_EMPTY¨", ArgumentExposure.NON_EMPTY),
            p("non_empty¨", ArgumentExposure.NON_EMPTY),
            p("NEVER¨", ArgumentExposure.NEVER),
            p("never¨", ArgumentExposure.NEVER)
        );
    }

    private void assertDefaultHandler(WebErrorHandler actualHandler,
                                      Class<? extends WebErrorHandler> expectedType) {
        assertThat(actualHandler).isNotNull();
        assertThat(actualHandler).isInstanceOf(expectedType);
    }

    @SafeVarargs
    private final void assertImplementations(List<WebErrorHandler> actualHandlers,
                                             int expectedSize,
                                             Class<? extends WebErrorHandler>... expectedTypes) {
        assertThat(actualHandlers).hasSize(expectedSize);
        for (int i = 0; i < expectedTypes.length; i++) {
            assertThat(actualHandlers.get(i)).isInstanceOf(expectedTypes[i]);
        }
    }

    @SuppressWarnings("unchecked")
    private List<WebErrorHandler> getImplementations(WebErrorHandlers webErrorHandlers) {
        try {
            Field field = webErrorHandlers.getClass().getDeclaredField("webErrorHandlers");
            field.setAccessible(true);
            return (List<WebErrorHandler>) field.get(webErrorHandlers);
        } catch (Exception e) {
            return null;
        }
    }

    private WebErrorHandler getDefaultHandler(WebErrorHandlers webErrorHandlers) {
        try {
            Field field = webErrorHandlers.getClass().getDeclaredField("defaultWebErrorHandler");
            field.setAccessible(true);

            return (WebErrorHandler) field.get(webErrorHandlers);
        } catch (Exception e) {
            return null;
        }
    }

    @TestConfiguration
    protected static class CustomWebErrorHandlers {

        @Bean
        public WebErrorHandlers webErrorHandlers(MessageSource messageSource) {
            return WebErrorHandlers.builder(messageSource)
                .withErrorHandlers(new First())
                .withDefaultWebErrorHandler(new Sec())
                .build();
        }
    }

    @TestConfiguration
    protected static class CustomHandlers {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public WebErrorHandler denyAll2() {
            return new Sec();
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 2)
        public WebErrorHandler denyAll() {
            return new First();
        }
    }

    @TestConfiguration
    protected static class DefaultHandler {

        @Bean
        public WebErrorHandler defaultWebErrorHandler() {
            return new Default();
        }
    }

    @TestConfiguration
    protected static class CustomAdapter {

        @Bean
        public HttpErrorAttributesAdapter customAdapter() {
            return httpError -> singletonMap("errors", httpError);
        }
    }

    private static class First implements WebErrorHandler {

        @Override
        public boolean canHandle(Throwable exception) {
            return false;
        }

        @Override
        @NonNull
        public HandledException handle(Throwable exception) {
            return new HandledException("", HttpStatus.BAD_REQUEST, null);
        }
    }

    private static class Sec implements WebErrorHandler {

        @Override
        public boolean canHandle(Throwable exception) {
            return false;
        }

        @Override
        @NonNull
        public HandledException handle(Throwable exception) {
            return new HandledException("", HttpStatus.BAD_REQUEST, null);
        }
    }

    private static class Default implements WebErrorHandler {

        @Override
        public boolean canHandle(Throwable exception) {
            return false;
        }

        @Override
        @NonNull
        public HandledException handle(Throwable exception) {
            return new HandledException("", HttpStatus.BAD_REQUEST, null);
        }
    }
}
