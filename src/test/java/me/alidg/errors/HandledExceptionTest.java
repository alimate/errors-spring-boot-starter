package me.alidg.errors;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    public void constructors_ShouldSetNullArgumentsAsEmptyMaps() {
        // TODO check arguments assertThat(new HandledException(new HandledException.ErrorWithArguments("error", null), BAD_REQUEST).getArguments())
        //   .isEqualTo(Collections.emptyList());

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
