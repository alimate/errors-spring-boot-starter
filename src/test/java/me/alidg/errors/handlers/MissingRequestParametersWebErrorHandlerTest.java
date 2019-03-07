package me.alidg.errors.handlers;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.MISSING_COOKIE;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.MISSING_HEADER;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.MISSING_MATRIX_VARIABLE;
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
                                                                      Map<String, List<?>> expectedArgs) {
        HandledException handledException = handler.handle(exception);

        assertThat(handledException.getErrorCodes()).containsOnly(expectedCode);
        assertThat(handledException.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(handledException.getArguments()).isEqualTo(expectedArgs);
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
                        singletonMap(MISSING_HEADER, singletonList(arg("header", "Authorization")))
                ),
                p(
                        new MissingRequestCookieException("sessionId", getParameter()),
                        MISSING_COOKIE,
                        BAD_REQUEST,
                        singletonMap(MISSING_COOKIE, singletonList(arg("cookie", "sessionId")))
                ),
                p(
                        new MissingMatrixVariableException("name", getParameter()),
                        MISSING_MATRIX_VARIABLE,
                        BAD_REQUEST,
                        singletonMap(MISSING_MATRIX_VARIABLE, singletonList(arg("variable", "name")))
                )
        );
    }

    private MethodParameter getParameter() {
        return new MethodParameter(getClass().getMethods()[0], 1);
    }
}
