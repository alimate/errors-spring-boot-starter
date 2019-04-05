package me.alidg.errors.handlers;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HandledException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.ServletException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static me.alidg.errors.handlers.ServletWebErrorHandler.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

/**
 * Unit tests for {@link ServletWebErrorHandler} handler.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class ServletWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final ServletWebErrorHandler handler = new ServletWebErrorHandler();

    @Test
    @Parameters(method = "provideParamsForCanHandle")
    public void canHandle_ShouldReturnTrueForSpringMvcSpecificErrors(Throwable exception, boolean expected) {
        assertThat(handler.canHandle(exception))
            .isEqualTo(expected);
    }

    @Test
    @Parameters(method = "provideParamsForHandle")
    public void handle_ShouldHandleSpringMvcErrorsProperly(Throwable exception,
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
            p(new NoHandlerFoundException(null, null, null), true),
            p(new HttpMessageNotReadableException("", mock(HttpInputMessage.class)), true),
            p(new MissingServletRequestParameterException("name", "String"), true),
            p(new HttpMediaTypeNotAcceptableException(""), true),
            p(new HttpMediaTypeNotSupportedException(""), true),
            p(new HttpRequestMethodNotSupportedException(""), true),
            p(new MissingServletRequestPartException("file"), true)
        );
    }

    private Object[] provideParamsForHandle() {
        return p(
            p(new HttpMessageNotReadableException("", mock(HttpInputMessage.class)), INVALID_OR_MISSING_BODY, BAD_REQUEST, emptyMap()),
            p(
                new HttpMediaTypeNotAcceptableException(asList(APPLICATION_JSON, MediaType.APPLICATION_PDF)),
                ServletWebErrorHandler.NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE,
                singletonMap(ServletWebErrorHandler.NOT_ACCEPTABLE, singletonList(arg("types", new HashSet<>(asList(APPLICATION_JSON_VALUE, APPLICATION_PDF_VALUE)))))
            ),
            p(
                new HttpMediaTypeNotSupportedException(APPLICATION_JSON, emptyList()),
                NOT_SUPPORTED,
                UNSUPPORTED_MEDIA_TYPE,
                singletonMap(NOT_SUPPORTED, singletonList(arg("type", APPLICATION_JSON_VALUE)))
            ),
            p(
                new HttpRequestMethodNotSupportedException("POST"),
                ServletWebErrorHandler.METHOD_NOT_ALLOWED,
                HttpStatus.METHOD_NOT_ALLOWED,
                singletonMap(ServletWebErrorHandler.METHOD_NOT_ALLOWED, singletonList(arg("method", "POST")))
            ),
            p(
                new MissingServletRequestParameterException("name", "String"),
                MISSING_PARAMETER,
                BAD_REQUEST,
                singletonMap(MISSING_PARAMETER, asList(arg("name", "name"), arg("expected", "String")))
            ),
            p(
                new MissingServletRequestPartException("file"),
                MISSING_PART,
                BAD_REQUEST,
                singletonMap(MISSING_PART, singletonList(arg("name", "file")))
            ),
            p(
                new NoHandlerFoundException("POST", "/test", null),
                NO_HANDLER,
                NOT_FOUND,
                singletonMap(NO_HANDLER, singletonList(arg("path", "/test")))
            ),
            p(new ServletException(), "unknown_error", INTERNAL_SERVER_ERROR, emptyMap())
        );
    }
}
