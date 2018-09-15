package me.alidg.errors.impl;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HandledException;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static me.alidg.Params.p;
import static me.alidg.errors.impl.SpringMvcWebErrorHandler.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;

/**
 * Unit tests for {@link SpringMvcWebErrorHandler} handler.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class SpringMvcWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final SpringMvcWebErrorHandler handler = new SpringMvcWebErrorHandler();

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
                p(new HttpMessageNotReadableException(""), true),
                p(new MissingServletRequestParameterException("name", "String"), true),
                p(new HttpMediaTypeNotAcceptableException(""), true),
                p(new HttpMediaTypeNotSupportedException(""), true),
                p(new HttpRequestMethodNotSupportedException(""), true),
                p(new MissingServletRequestPartException("file"), true)
        );
    }

    private Object[] provideParamsForHandle() {
        return p(
                p(new HttpMessageNotReadableException(""), INVALID_OR_MISSING_BODY ,BAD_REQUEST, emptyMap()),
                p(
                        new HttpMediaTypeNotAcceptableException(asList(APPLICATION_JSON, MediaType.APPLICATION_PDF)),
                        SpringMvcWebErrorHandler.NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE,
                        singletonMap(SpringMvcWebErrorHandler.NOT_ACCEPTABLE, asList(APPLICATION_JSON, APPLICATION_PDF))
                ),
                p(
                        new HttpMediaTypeNotSupportedException(APPLICATION_JSON, emptyList()),
                        NOT_SUPPORTED,
                        UNSUPPORTED_MEDIA_TYPE,
                        singletonMap(NOT_SUPPORTED, singletonList(APPLICATION_JSON))
                ),
                p(
                        new HttpRequestMethodNotSupportedException("POST"),
                        SpringMvcWebErrorHandler.METHOD_NOT_ALLOWED,
                        HttpStatus.METHOD_NOT_ALLOWED,
                        singletonMap(SpringMvcWebErrorHandler.METHOD_NOT_ALLOWED, singletonList("POST"))
                ),
                p(
                        new MissingServletRequestParameterException("name", "String"),
                        MISSING_PARAMETER,
                        BAD_REQUEST,
                        singletonMap(MISSING_PARAMETER, asList("name", "String"))
                ),
                p(
                        new MissingServletRequestPartException("file"),
                        MISSING_PART,
                        BAD_REQUEST,
                        singletonMap(MISSING_PART, singletonList("file"))
                ),
                p(
                        new NoHandlerFoundException("POST", "/test", null),
                        NO_HANDLER,
                        NOT_FOUND,
                        singletonMap(NO_HANDLER, singletonList("/test"))
                ),
                p(new ServletException(), "unknown_error", INTERNAL_SERVER_ERROR, emptyMap())
        );
    }
}
