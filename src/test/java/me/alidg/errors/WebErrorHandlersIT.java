package me.alidg.errors;

import me.alidg.errors.HttpError.CodedMessage;
import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeArg;
import me.alidg.errors.annotation.ExposeAsArg;
import me.alidg.errors.conf.ErrorsAutoConfiguration;
import me.alidg.errors.handlers.LastResortWebErrorHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.util.Collections.emptyList;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static me.alidg.errors.WebErrorHandlersIT.Pojo.pojo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link WebErrorHandlers}.
 *
 * @author Ali Dehghani
 */
public class WebErrorHandlersIT {

    private static final Locale IRAN_LOCALE = new Locale("fa", "IR");
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withPropertyValues("spring.messages.basename=test_messages")
        .withConfiguration(AutoConfigurations.of(
            MessageSourceAutoConfiguration.class,
            ValidationAutoConfiguration.class,
            ErrorsAutoConfiguration.class

        ));

    @ParameterizedTest
    @MethodSource("provideValidationParams")
    public void validationException_ShouldBeHandledProperly(Object pojo, Locale locale, CodedMessage... codedMessages) {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);
            Validator validator = ctx.getBean(Validator.class);

            BindingResult result = new BeanPropertyBindingResult(pojo, "pojo");
            validator.validate(pojo, result);

            // Assertions for BindException
            HttpError bindError = errorHandlers.handle(new BindException(result), null, locale);
            assertThat(bindError.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bindError.getErrors()).containsOnly(codedMessages);

            // Assertions for MethodArgumentNotValidException
            HttpError argumentError = errorHandlers.handle(new MethodArgumentNotValidException(mock(MethodParameter.class), result), null, locale);
            assertThat(argumentError.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(argumentError.getErrors()).containsOnly(codedMessages);

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @Test
    public void annotatedException_ShouldBeHandledProperly() {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            SomeException exception = new SomeException(10, 12);

            // Without locale
            HttpError error = errorHandlers.handle(exception, null, null);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getErrors()).containsOnly(cm("invalid_params", "Params are: 10, 12 and 42",
                arg("min", 10), arg("max", 12), arg("namedAnswer", "42"), arg("notUsed", "123")));

            // With locale
            error = errorHandlers.handle(exception, null, Locale.CANADA);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getErrors()).containsOnly(cm("invalid_params", "Params are: 10, 12 and 42",
                arg("min", 10), arg("max", 12), arg("namedAnswer", "42"), arg("notUsed", "123")));

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @Test
    public void annotatedException_withDeprecatedAnnotations_ShouldBeHandledProperly() {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            SomeExceptionWithDeprecatedExposeAnnotations exception = new SomeExceptionWithDeprecatedExposeAnnotations(10, 12);

            // Without locale
            HttpError error = errorHandlers.handle(exception, null, null);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getErrors()).containsOnly(cm("invalid_params", "Params are: 10, 12 and 42",
                arg("min", 10), arg("max", 12), arg("theAnswer", "42"), arg("notUsed", "123")));

            // With locale
            error = errorHandlers.handle(exception, null, Locale.CANADA);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getErrors()).containsOnly(cm("invalid_params", "Params are: 10, 12 and 42",
                arg("min", 10), arg("max", 12), arg("theAnswer", "42"), arg("notUsed", "123")));

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @Test
    public void annotatedException_withMixedNewAndDeprecatedAnnotations_ShouldBeHandledProperly() {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            SomeExceptionWithMixedExposeAnnotations exception = new SomeExceptionWithMixedExposeAnnotations(10, 12);

            // Without locale
            HttpError error = errorHandlers.handle(exception, null, null);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getErrors()).containsOnly(cm("invalid_params_named", "Params are: 10, 12 and 42",
                arg("min", 10), arg("legacyMax", 12), arg("notUsed", "123"), arg("theAnswer", "42")));

            // With locale
            error = errorHandlers.handle(exception, null, Locale.CANADA);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getErrors()).containsOnly(cm("invalid_params_named", "Params are: 10, 12 and 42",
                arg("min", 10), arg("legacyMax", 12), arg("notUsed", "123"), arg("theAnswer", "42")));

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @ParameterizedTest
    @MethodSource("provideParamsForUnknownErrors")
    public void unknownErrors_ShouldBeHandledProperly(Throwable exception) {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            HttpError error = errorHandlers.handle(exception, null, null);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(error.getErrors()).containsOnly(cm(LastResortWebErrorHandler.UNKNOWN_ERROR_CODE, null));

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @ParameterizedTest
    @MethodSource("provideParamsForRefined")
    public void refiner_ShouldRefineExceptionsBeforeHandlingThem(Throwable exception,
                                                                 HttpStatus expectedStatus,
                                                                 CodedMessage... codedMessages) {
        contextRunner.withUserConfiguration(RefinerConfig.class).run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            HttpError httpError = errorHandlers.handle(exception, null, null);

            assertThat(httpError.getHttpStatus()).isEqualTo(expectedStatus);
            assertThat(httpError.getErrors()).containsOnly(codedMessages);

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @ParameterizedTest
    @MethodSource("provideEmptyViolations")
    public void constraintViolation_WithNoViolation_ShouldBeHandledByTheDefaultHandler(Exception exception) {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            HttpError httpError = errorHandlers.handle(exception, null, null);

            assertThat(httpError.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(httpError.getErrors()).containsOnly(cm("unknown_error", null));

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @ParameterizedTest
    @MethodSource("provideValidationParams")
    public void constraintViolationException_ShouldBeHandledProperly(Object pojo, Locale locale, CodedMessage... codedMessages) {
        contextRunner.run(ctx -> {
            HttpError error;

            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);
            javax.validation.Validator validator = ctx.getBean(javax.validation.Validator.class);

            ConstraintViolationException exception = new ConstraintViolationException(validator.validate(pojo));

            error = errorHandlers.handle(exception, null, locale);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.getErrors()).containsOnly(codedMessages);

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @Test
    public void errorFingerprint_ShouldNotBeCalculatedByDefault() {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            HttpError error = errorHandlers.handle(new SomeException(10, 12), null, null);
            assertThat(error.getFingerprint()).isNull();

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @Test
    public void errorFingerprint_ShouldBeCalculatedWhenConfigured() {
        contextRunner.withPropertyValues("errors.add-fingerprint=true").run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            HttpError error = errorHandlers.handle(new SomeException(10, 12), null, null);
            assertThat(error.getFingerprint()).isNotNull();

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    @Test
    public void errorFingerprint_ShouldBeUnique() {
        contextRunner.withPropertyValues("errors.add-fingerprint=true").run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            Exception e1 = new SomeException(1, 2);
            Exception e2 = new RuntimeException();

            HttpError error1 = errorHandlers.handle(e1, null, null);
            HttpError error2 = errorHandlers.handle(e2, null, null);

            assertThat(error1.getFingerprint()).isNotEqualTo(error2.getFingerprint());

            verifyPostProcessorsHasBeenCalled(ctx);
        });
    }

    private void verifyPostProcessorsHasBeenCalled(ApplicationContext ctx) {
        ctx.getBeansOfType(WebErrorHandlerPostProcessor.class).values().forEach(p -> verify(p).process(any()));
    }

    private static Object[] provideParamsForUnknownErrors() {
        return p(null, new IllegalArgumentException(), new OutOfMemoryError());
    }

    private static Object[] provideValidationParams() {
        return p(
            // Invalid text
            p(
                pojo("", 10, "a"), null,
                arr(
                    cm("text.required", "The text is required",
                        arg("invalid", ""), arg("property", "text"))
                )
            ),
            p(
                pojo("", 10, "a"), Locale.CANADA,
                arr(
                    cm("text.required", "The text is required",
                        arg("invalid", ""), arg("property", "text"))
                )
            ),
            p(
                pojo("", 10, "a"), IRAN_LOCALE,
                arr(
                    cm("text.required", "متن اجباری است",
                        arg("invalid", ""), arg("property", "text"))
                )
            ),

            // Invalid number: min
            p(
                pojo("t", -1, "a"), null,
                arr(
                    cm("number.min", "The min is 0",
                        arg("value", 0L), arg("invalid", -1), arg("property", "number"))
                )
            ),
            p(
                pojo("t", -1, "a"), Locale.GERMANY,
                arr(
                    cm("number.min", "The min is 0",
                        arg("value", 0L), arg("invalid", -1), arg("property", "number"))
                )
            ),

            // Invalid number: max
            p(
                pojo("t", 11, "a"), null,
                arr(
                    cm("number.max", null,
                        arg("value", 10L), arg("invalid", 11), arg("property", "number"))
                )
            ),
            p(
                pojo("t", 11, "a"), Locale.GERMANY,
                arr(
                    cm("number.max", null,
                        arg("value", 10L), arg("invalid", 11), arg("property", "number"))
                )
            ),
            p(
                pojo("t", 11, "a"), IRAN_LOCALE,
                arr(
                    cm("number.max", null,
                        arg("value", 10L), arg("invalid", 11), arg("property", "number"))
                )
            ),

            // Invalid range
            p(
                pojo("t", 0), null,
                arr(
                    cm("range.limit", "Between 1 and 3",
                        arg("max", 3), arg("min", 1), arg("invalid", emptyList()), arg("property", "range"))
                )
            ),
            p(
                pojo("t", 0), Locale.GERMANY,
                arr(
                    cm("range.limit", "Between 1 and 3",
                        arg("max", 3), arg("min", 1), arg("invalid", emptyList()), arg("property", "range"))
                )
            ),

            // Mixed
            p(
                pojo("", 11), null,
                arr(
                    cm("range.limit", "Between 1 and 3",
                        arg("max", 3), arg("min", 1), arg("invalid", emptyList()), arg("property", "range")),
                    cm("number.max", null,
                        arg("value", 10L), arg("invalid", 11), arg("property", "number")),
                    cm("text.required", "The text is required",
                        arg("invalid", ""), arg("property", "text"))
                )
            ),
            p(
                pojo("", 11), Locale.CANADA,
                arr(
                    cm("range.limit", "Between 1 and 3",
                        arg("max", 3), arg("min", 1), arg("invalid", emptyList()), arg("property", "range")),
                    cm("number.max", null,
                        arg("value", 10L), arg("invalid", 11), arg("property", "number")),
                    cm("text.required", "The text is required",
                        arg("invalid", ""), arg("property", "text"))
                )
            )
        );
    }

    private static Object[] provideParamsForRefined() {
        return p(
            p(
                new SymptomException(new SomeException(10, 11)),
                HttpStatus.UNPROCESSABLE_ENTITY,
                arr(
                    cm("invalid_params", "Params are: 10, 11 and 42",
                        arg("min", 10), arg("max", 11), arg("namedAnswer", "42"), arg("notUsed", "123"))
                )
            ),
            p(
                new SymptomException(null), HttpStatus.INTERNAL_SERVER_ERROR, arr(cm("unknown_error", null))
            ),
            p(
                new IllegalArgumentException(), HttpStatus.INTERNAL_SERVER_ERROR, arr(cm("unknown_error", null))
            )
        );
    }

    private static Object[] provideEmptyViolations() {
        return p(
            new ConstraintViolationException(null),
            new ConstraintViolationException(Collections.emptySet())
        );
    }

    private static CodedMessage cm(String code, String message, Argument... arguments) {
        return new CodedMessage(code, message, Arrays.asList(arguments));
    }

    private static CodedMessage[] arr(CodedMessage... objs) {
        return objs;
    }

    static class Pojo {

        @NotBlank(message = "{text.required}")
        private String text;

        @Min(value = 0, message = "number.min")
        @Max(value = 10, message = "number.max")
        private int number;

        @Size(min = 1, max = 3, message = "range.limit")
        private List<String> range;

        Pojo(String text, int number, String... range) {
            this.text = text;
            this.number = number;
            this.range = Arrays.asList(range);
        }

        static Pojo pojo(String text, int number, String... range) {
            return new Pojo(text, number, range);
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public List<String> getRange() {
            return range;
        }

        public void setRange(List<String> range) {
            this.range = range;
        }
    }

    private static class SymptomException extends RuntimeException {

        SymptomException(Throwable cause) {
            super(cause);
        }
    }

    @TestConfiguration
    protected static class RefinerConfig {

        @Bean
        public ExceptionRefiner exceptionRefiner() {
            return exception -> exception instanceof SymptomException ? exception.getCause() : exception;
        }
    }

    @TestConfiguration
    static class ErrorActionExecutorConfig {

        @Bean
        public WebErrorHandlerPostProcessor first() {
            return mock(WebErrorHandlerPostProcessor.class);
        }

        @Bean
        public WebErrorHandlerPostProcessor second() {
            return mock(WebErrorHandlerPostProcessor.class);
        }
    }

    @ExceptionMapping(errorCode = "invalid_params", statusCode = HttpStatus.UNPROCESSABLE_ENTITY)
    private static class SomeException extends RuntimeException {

        @ExposeArg(order = 100)
        private final int min;
        @ExposeArg(order = 101)
        private final int max;

        SomeException(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @ExposeArg(order = 1000, value = "namedAnswer")
        public String theAnswer() {
            return "42";
        }

        @ExposeArg(order = 2000)
        public String notUsed() {
            return "123";
        }
    }

    @ExceptionMapping(errorCode = "invalid_params", statusCode = HttpStatus.UNPROCESSABLE_ENTITY)
    private static class SomeExceptionWithDeprecatedExposeAnnotations extends RuntimeException {

        @ExposeAsArg(100)
        private final int min;
        @ExposeAsArg(101)
        private final int max;

        SomeExceptionWithDeprecatedExposeAnnotations(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @ExposeAsArg(1000)
        public String theAnswer() {
            return "42";
        }

        @ExposeAsArg(2000)
        public String notUsed() {
            return "123";
        }
    }

    @ExceptionMapping(errorCode = "invalid_params_named", statusCode = HttpStatus.UNPROCESSABLE_ENTITY)
    private static class SomeExceptionWithMixedExposeAnnotations extends RuntimeException {

        @ExposeArg(order = 100)
        private final int min;
        @ExposeAsArg(value = 101, name = "legacyMax")
        private final int max;

        SomeExceptionWithMixedExposeAnnotations(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @ExposeArg
        public String theAnswer() {
            return "42";
        }

        @ExposeAsArg(2000)
        public String notUsed() {
            return "123";
        }
    }
}
