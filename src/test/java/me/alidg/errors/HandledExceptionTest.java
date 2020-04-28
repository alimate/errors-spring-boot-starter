package me.alidg.errors;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Unit tests for {@link HandledException} class.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class HandledExceptionTest {

    @Test
    @Parameters(method = "provideParamsForPrimary")
    public void primaryConstructor_ShouldEnforceItsPreconditions(List<ErrorWithArguments> errors,
                                                                 HttpStatus status,
                                                                 Class<? extends Throwable> expected,
                                                                 String message) {
        assertThatThrownBy(() -> new HandledException(errors, status))
            .isInstanceOf(expected)
            .hasMessage(message);
    }

    @Test
    @Parameters(method = "provideParamsForSecondary")
    public void secondConstructor_ShouldEnforceItsPreconditions(String errorCode,
                                                                HttpStatus status,
                                                                Class<? extends Throwable> expected,
                                                                String message) {
        assertThatThrownBy(() -> new HandledException(ErrorWithArguments.noArgumentError(errorCode), status))
            .isInstanceOf(expected)
            .hasMessage(message);
    }

    @Test
    public void testGetErrorCodes_singleError() {
        HandledException exception = new HandledException(ErrorWithArguments.noArgumentError("error"), BAD_REQUEST);
        assertThat(exception.getErrorCodes()).hasSize(1)
                                             .contains("error");

    }

    @Test
    public void testGetErrorCodes_multipleErrors() {
        List<ErrorWithArguments> list = asList(ErrorWithArguments.noArgumentError("error"),
                                               new ErrorWithArguments("error2", singletonList(Argument.arg("argName", "argValue"))));
        HandledException exception = new HandledException(list, BAD_REQUEST);
        assertThat(exception.getErrorCodes()).hasSize(2)
                                             .contains("error", "error2");

    }

    @Test
    public void testGetErrorCodes_duplicateErrors() {
        List<ErrorWithArguments> list = asList(new ErrorWithArguments("error", singletonList(Argument.arg("argName", "argValue1"))),
                                               new ErrorWithArguments("error", singletonList(Argument.arg("argName", "argValue2"))));
        HandledException exception = new HandledException(list, BAD_REQUEST);
        assertThat(exception.getErrorCodes()).hasSize(1)
                                             .contains("error");

    }

    @Test
    public void testGetArguments_singleError() {
        HandledException exception = new HandledException(ErrorWithArguments.noArgumentError("error"), BAD_REQUEST);
        assertThat(exception.getArguments()).hasSize(1)
                                            .hasEntrySatisfying("error", arguments -> assertThat(arguments).isEmpty());

    }

    @Test
    public void testGetArguments_multipleErrors() {
        List<ErrorWithArguments> list = asList(ErrorWithArguments.noArgumentError("error"),
                                               new ErrorWithArguments("error2", singletonList(Argument.arg("argName", "argValue"))));
        HandledException exception = new HandledException(list, BAD_REQUEST);
        assertThat(exception.getArguments()).hasSize(2)
                                            .hasEntrySatisfying("error", arguments -> assertThat(arguments).isEmpty())
                                            .hasEntrySatisfying("error2", arguments -> assertThat(arguments).hasSize(1));

    }

    @Test
    public void testGetArguments_duplicateErrors() {
        List<ErrorWithArguments> list = asList(new ErrorWithArguments("error", singletonList(Argument.arg("argName", "argValue1"))),
                                               new ErrorWithArguments("error", singletonList(Argument.arg("argName", "argValue2"))));
        HandledException exception = new HandledException(list, BAD_REQUEST);
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(exception::getArguments)
            .withMessage("Duplicate key error (attempted merging values [argName=argValue1] and [argName=argValue2])");
    }

    private Object[] provideParamsForPrimary() {
        return p(
            p(null, null, NullPointerException.class, "Error codes is required"),
            p(asList(ErrorWithArguments.noArgumentError(""), ErrorWithArguments.noArgumentError(""), null), null, NullPointerException.class, "Status code is required"),
            p(singletonList(null), BAD_REQUEST, NullPointerException.class, "The single error code can't be null"),
            p(emptyList(), BAD_REQUEST, IllegalArgumentException.class, "At least one error code should be provided")
        );
    }

    private Object[] provideParamsForSecondary() {
        return p(
            p(null, null, NullPointerException.class, "The single error code can't be null"),
            p("error", null, NullPointerException.class, "Status code is required"),
            p(null, BAD_REQUEST, NullPointerException.class, "The single error code can't be null")
        );
    }
}
