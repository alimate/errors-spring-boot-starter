package me.alidg.errors.handlers;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.Argument;
import me.alidg.errors.ErrorWithArguments;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingMatrixVariableException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Unit tests for {@link MissingRequestParametersWebErrorHandler} handler.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class MissingRequestParametersWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final WebErrorHandler handler = new MissingRequestParametersWebErrorHandler();

    @Test
    @Parameters(method = "provideParamsForCanHandle")
    public void canHandle_ShouldReturnTrueForMissingRequestParamsErrors(Throwable exception, boolean expected) {
        assertThat(handler.canHandle(exception))
            .isEqualTo(expected);
    }

    @Test
    @Parameters(method = "provideParamsForHandle")
    public void handle_ShouldHandleMissingRequestParamsErrorsProperly(Throwable exception,
                                                                      String expectedCode,
                                                                      HttpStatus expectedStatus,
                                                                      Map<String, List<Argument>> expectedArgs) {
        HandledException handledException = handler.handle(exception);

        List<ErrorWithArguments> errors = handledException.getErrors();
        assertThat(errors).extracting(ErrorWithArguments::getErrorCode)
                          .containsOnly(expectedCode);
        assertThat(errors).extracting(ErrorWithArguments::getArguments)
                          .containsExactlyInAnyOrderElementsOf(expectedArgs.values());

        assertThat(handledException.getStatusCode()).isEqualTo(expectedStatus);
    }

    private Object[] provideParamsForCanHandle() {
        return p(
            p(null, false),
            p(new RuntimeException(), false),
            p(new MissingPathVariableException("name", getParameter()), false),
            p(new MissingRequestHeaderException("name", getParameter()), true),
            p(new MissingRequestCookieException("name", getParameter()), true),
            p(new MissingMatrixVariableException("name", getParameter()), true)
        );
    }

    private Object[] provideParamsForHandle() {
        return p(
            p(
                new MissingRequestHeaderException("Authorization", getParameter()),
                MISSING_HEADER,
                BAD_REQUEST,
                singletonMap(MISSING_HEADER, asList(arg("name", "Authorization"), arg("expected", "boolean")))
            ),
            p(
                new MissingRequestCookieException("sessionId", getParameter()),
                MISSING_COOKIE,
                BAD_REQUEST,
                singletonMap(MISSING_COOKIE, asList(arg("name", "sessionId"), arg("expected", "boolean")))
            ),
            p(
                new MissingMatrixVariableException("name", getParameter()),
                MISSING_MATRIX_VARIABLE,
                BAD_REQUEST,
                singletonMap(MISSING_MATRIX_VARIABLE, asList(arg("name", "name"), arg("expected", "boolean")))
            )
        );
    }

    private MethodParameter getParameter() {
        Method testMethod = Arrays.stream(getClass().getMethods())
            .filter(m -> m.getName().equals("canHandle_ShouldReturnTrueForMissingRequestParamsErrors"))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
        return new MethodParameter(testMethod, 1);
    }
}
