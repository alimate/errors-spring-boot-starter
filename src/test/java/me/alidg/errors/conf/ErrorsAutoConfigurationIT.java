package me.alidg.errors.conf;

import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.handlers.*;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Field;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ErrorsAutoConfiguration} configuration.
 *
 * @author Ali Dehghani
 */
public class ErrorsAutoConfigurationIT {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ErrorsAutoConfiguration.class));

    @Test
    public void whenAnotherWebErrorHandlersRegister_TheDefaultOneShouldBeDiscarded() {
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
            assertImplementations(implementations, 4,
                    SpringValidationWebErrorHandler.class, AnnotatedWebErrorHandler.class,
                    SpringMvcWebErrorHandler.class, SpringSecurityWebErrorHandler.class
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
            assertImplementations(implementations, 6,
                    SpringValidationWebErrorHandler.class, AnnotatedWebErrorHandler.class, SpringMvcWebErrorHandler.class,
                    Sec.class, First.class, SpringSecurityWebErrorHandler.class);

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
            assertImplementations(implementations, 4,
                    SpringValidationWebErrorHandler.class, AnnotatedWebErrorHandler.class,
                    SpringMvcWebErrorHandler.class, SpringSecurityWebErrorHandler.class
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
            assertImplementations(implementations, 6,
                    SpringValidationWebErrorHandler.class, AnnotatedWebErrorHandler.class, SpringMvcWebErrorHandler.class,
                    Sec.class, First.class, SpringSecurityWebErrorHandler.class);

            // Default Error Handler
            WebErrorHandler defaultHandler = getDefaultHandler(errorHandlers);
            assertDefaultHandler(defaultHandler, Default.class);
        });
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
            Field field = webErrorHandlers.getClass().getDeclaredField("implementations");
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
    public static class CustomWebErrorHandlers {

        @Bean
        public WebErrorHandlers webErrorHandlers(MessageSource messageSource) {
            return new WebErrorHandlers(messageSource, singletonList(new First()), new Sec());
        }
    }

    @TestConfiguration
    public static class CustomHandlers {

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
    public static class DefaultHandler {

        @Bean
        public WebErrorHandler defaultWebErrorHandler() {
            return new Default();
        }
    }

    private static class First implements WebErrorHandler {

        @Override
        public boolean canHandle(Throwable exception) {
            return false;
        }

        @Override
        public HandledException handle(Throwable exception) {
            return null;
        }
    }

    private static class Sec implements WebErrorHandler {

        @Override
        public boolean canHandle(Throwable exception) {
            return false;
        }

        @Override
        public HandledException handle(Throwable exception) {
            return null;
        }
    }

    private static class Default implements WebErrorHandler {

        @Override
        public boolean canHandle(Throwable exception) {
            return false;
        }

        @Override
        public HandledException handle(Throwable exception) {
            return null;
        }
    }
}
