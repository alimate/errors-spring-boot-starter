package me.alidg.errors;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HttpError.CodedMessage;
import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeAsArg;
import me.alidg.errors.conf.ErrorsAutoConfiguration;
import me.alidg.errors.impl.LastResortWebErrorHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static me.alidg.Params.p;
import static me.alidg.errors.WebErrorHandlersIT.Pojo.pojo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebErrorHandlers}.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class WebErrorHandlersIT {

    private static final Locale IRAN_LOCALE = new Locale("fa", "IR");
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withPropertyValues("spring.messages.basename=test_messages")
            .withConfiguration(AutoConfigurations.of(
                    MessageSourceAutoConfiguration.class,
                    ValidationAutoConfiguration.class,
                    ErrorsAutoConfiguration.class

            ));

    @Test
    @Parameters(method = "provideValidationParams")
    public void validationException_ShouldBeHandledProperly(Object pojo, Locale locale, CodedMessage... codedMessages) {
        contextRunner.run(ctx -> {
            HttpError error;

            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);
            Validator validator = ctx.getBean(Validator.class);

            BindingResult result = new BeanPropertyBindingResult(pojo, "pojo");
            validator.validate(pojo, result);

            // Assertions for BindException
            error = errorHandlers.handle(new BindException(result), locale);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.getErrors()).containsOnly(codedMessages);

            // Assertions for MethodArgumentNotValidException
            error = errorHandlers.handle(new MethodArgumentNotValidException(null, result), locale);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.getErrors()).containsOnly(codedMessages);
        });
    }

    @Test
    public void annotatedException_ShouldBeHandledProperly() {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            SomeException exception = new SomeException(10, 12);

            // Without locale
            HttpError error = errorHandlers.handle(exception, null);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getErrors()).containsOnly(cm("invalid_params", "Params are: 10, 12 and 42"));

            // With locale
            error = errorHandlers.handle(exception, IRAN_LOCALE);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(error.getErrors()).containsOnly(cm("invalid_params", "Params are: 10, 12 and 42"));
        });
    }

    @Test
    @Parameters(method = "provideParamsForUnknownErrors")
    public void unknownErrors_ShouldBeHandledProperly(Throwable exception) {
        contextRunner.run(ctx -> {
            WebErrorHandlers errorHandlers = ctx.getBean(WebErrorHandlers.class);

            HttpError error = errorHandlers.handle(exception, null);
            assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(error.getErrors()).containsOnly(cm(LastResortWebErrorHandler.UNKNOWN_ERROR_CODE, null));
        });
    }

    private Object[] provideParamsForUnknownErrors() {
        return p(null, new IllegalArgumentException(), new OutOfMemoryError());
    }

    private Object[] provideValidationParams() {
        return p(
                // Invalid text
                p(pojo("", 10, "a"), null, cm("text.required", "The text is required")),
                p(pojo("", 10, "a"), Locale.CANADA, cm("text.required", "The text is required")),
                p(pojo("", 10, "a"), IRAN_LOCALE, cm("text.required", "متن اجباری است")),

                // Invalid number: min
                p(pojo("t", -1, "a"), null, cm("number.min", "The min is 0")),
                p(pojo("t", -1, "a"), Locale.GERMANY, cm("number.min", "The min is 0")),
                p(pojo("t", -1, "a"), IRAN_LOCALE, cm("number.min", "کم ترین مقدار قابل قبول 0 است")),

                // Invalid number: max
                p(pojo("t", 11, "a"), null, cm("number.max", null)),
                p(pojo("t", 11, "a"), Locale.GERMANY, cm("number.max", null)),
                p(pojo("t", 11, "a"), IRAN_LOCALE, cm("number.max", null)),

                // Invalid range
                p(pojo("t", 0), null, cm("range.limit", "Between 1 and 3")),
                p(pojo("t", 0), Locale.GERMANY, cm("range.limit", "Between 1 and 3")),
                p(pojo("t", 0), IRAN_LOCALE, cm("range.limit", "Between 1 and 3")),

                // Mixed
                p(
                        pojo("", 11), null, cm("range.limit", "Between 1 and 3"),
                        cm("number.max", null), cm("text.required", "The text is required")
                ),
                p(
                        pojo("", 11), Locale.CANADA, cm("range.limit", "Between 1 and 3"),
                        cm("number.max", null), cm("text.required", "The text is required")
                ),
                p(
                        pojo("", 11), IRAN_LOCALE, cm("range.limit", "Between 1 and 3"),
                        cm("number.max", null), cm("text.required", "متن اجباری است")
                )
        );
    }

    private CodedMessage cm(String code, String message) {
        return new CodedMessage(code, message);
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

        static Pojo pojo(String text, int number, String... range) {
            return new Pojo(text, number, range);
        }
    }

    @ExceptionMapping(errorCode = "invalid_params", statusCode = HttpStatus.UNPROCESSABLE_ENTITY)
    private class SomeException extends RuntimeException {

        @ExposeAsArg(100) private final int min;
        @ExposeAsArg(101) private final int max;

        SomeException(int min, int max) {
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
}
