package me.alidg.errors.handlers;

import me.alidg.errors.Argument;
import me.alidg.errors.HandledException;
import me.alidg.errors.annotation.ExceptionMapping;
import me.alidg.errors.annotation.ExposeAsArg;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Unit tests for {@link AnnotatedWebErrorHandler} error handler.
 *
 * @author Ali Dehghani
 */
public class AnnotatedWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final AnnotatedWebErrorHandler handler = new AnnotatedWebErrorHandler();

    @ParameterizedTest
    @MethodSource("provideParamsForCanHandle")
    public void canHandle_ShouldOnlyReturnTrueForExceptionsAnnotatedWithExceptionMapping(Exception exception,
                                                                                         boolean expected) {
        assertThat(handler.canHandle(exception))
            .isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("provideParamsForHandle")
    public void handle_ShouldProperlyHandleTheGivenException(Exception exception,
                                                             String code,
                                                             HttpStatus status,
                                                             List<Argument> args) {
        HandledException handled = handler.handle(exception);

        assertThat(handled).isNotNull();
        assertThat(handled.getErrorCodes()).hasSize(1);
        assertThat(handled.getErrorCodes()).containsExactly(code);
        assertThat(handled.getStatusCode()).isEqualTo(status);
        assertThat((handled.getArguments().get(code))).isEqualTo(args);
    }

    private static Object[] provideParamsForCanHandle() {
        return p(
            p(null, false),
            p(new NotAnnotated(), false),
            p(new Annotated("", ""), true),
            p(new Inherited(), true)
        );
    }

    private static Object[] provideParamsForHandle() {
        return p(
            p(new Annotated("f", "s"), "annotated", BAD_REQUEST, asList(
                arg("staticExposure", "42"),
                arg("some_value", "f"),
                arg("other", "s"))),
            p(new Inherited(), "annotated", BAD_REQUEST, asList(
                arg("staticExposure", "42"),
                arg("random", "random"),
                arg("other", "s"))),
            p(new NoExposedArgs(), "no_exposed", BAD_REQUEST, Collections.emptyList())
        );
    }

    // Auxiliary exception definitions!

    private static class NotAnnotated extends RuntimeException {
    }

    @ExceptionMapping(statusCode = BAD_REQUEST, errorCode = "no_exposed")
    private static class NoExposedArgs extends RuntimeException {
    }

    @ExceptionMapping(statusCode = BAD_REQUEST, errorCode = "annotated")
    private static class Annotated extends RuntimeException {

        @ExposeAsArg(value = -1, name = "some_value")
        private final String someValue;
        private final String other;

        private Annotated(String someValue, String other) {
            this.someValue = someValue;
            this.other = other;
        }

        @ExposeAsArg(0)
        public void shouldBeDiscarded() {
        }

        @ExposeAsArg(-100)
        public String shouldBeIgnoredToo(String howToPassThis) {
            return "";
        }

        @ExposeAsArg(-11)
        public String staticExposure() {
            return "42";
        }

        @ExposeAsArg(value = 100, name = "other")
        public String getOther() {
            return other;
        }
    }

    private static class Inherited extends Annotated {
        private Inherited() {
            super("f", "s");
        }

        @ExposeAsArg(-1)
        public String random() {
            return "random";
        }

        @ExposeAsArg(-2)
        public int thrower() {
            throw new IllegalStateException(":(");
        }
    }
}