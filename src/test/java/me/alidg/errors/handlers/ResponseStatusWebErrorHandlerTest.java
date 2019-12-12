package me.alidg.errors.handlers;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.Argument;
import me.alidg.errors.HandledException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.*;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.*;
import static me.alidg.Params.p;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.*;
import static me.alidg.errors.handlers.ServletWebErrorHandler.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;

/**
 * Unit tests for {@link ResponseStatusWebErrorHandler} handler.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class ResponseStatusWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final ResponseStatusWebErrorHandler handler = new ResponseStatusWebErrorHandler();

    @Test
    @Parameters(method = "paramsForCanHandle")
    public void canHandle_ShouldReturnTrueForResponseStatusExceptions(Exception e, boolean expected) {
        assertThat(handler.canHandle(e))
            .isEqualTo(expected);
    }

    @Test
    @Parameters(method = "paramsForHandle")
    public void handle_ShouldHandleResponseStatusExceptionsAppropriately(Exception e,
                                                                         String expectedErrorCode,
                                                                         HttpStatus expectedStatus,
                                                                         List<Argument> expectedArguments) {
        HandledException handledException = handler.handle(e);

        assertThat(handledException.getErrorCodes()).containsExactly(expectedErrorCode);
        assertThat(handledException.getStatusCode()).isEqualTo(expectedStatus);
        if (expectedArguments == null || expectedArguments.isEmpty())
            assertThat(handledException.getArguments()).isEmpty();
        else
            assertThat(handledException.getArguments().get(expectedErrorCode)).containsAll(expectedArguments);
    }

    private Object[] paramsForCanHandle() {
        return p(
            p(mock(UnsupportedMediaTypeStatusException.class), true),
            p(mock(WebExchangeBindException.class), true),
            p(mock(ServerWebInputException.class), true),
            p(new RuntimeException(), false),
            p(null, false)
        );
    }

    private Object[] paramsForHandle() {
        return p(

            // MediaTypeNotSupportedStatusException related parameters
            p(
                new MediaTypeNotSupportedStatusException(emptyList()),
                NOT_SUPPORTED,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                emptyList()
            ),
            p(
                new MediaTypeNotSupportedStatusException(singletonList(APPLICATION_JSON)),
                NOT_SUPPORTED,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                singletonList(Argument.arg("types", singleton(APPLICATION_JSON_VALUE)))
            ),

            // UnsupportedMediaTypeStatusException related parameters
            p(
                new UnsupportedMediaTypeStatusException(null, emptyList()),
                NOT_SUPPORTED,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                emptyList()
            ),
            p(
                new UnsupportedMediaTypeStatusException(null, singletonList(APPLICATION_PDF)),
                NOT_SUPPORTED,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                singletonList(Argument.arg("types", singleton(APPLICATION_PDF_VALUE)))
            ),

            // NotAcceptableStatusException related params
            p(
                new NotAcceptableStatusException(emptyList()),
                NOT_ACCEPTABLE,
                HttpStatus.NOT_ACCEPTABLE,
                emptyList()
            ),
            p(
                new NotAcceptableStatusException(Arrays.asList(APPLICATION_XML, APPLICATION_JSON)),
                NOT_ACCEPTABLE,
                HttpStatus.NOT_ACCEPTABLE,
                singletonList(Argument.arg("types", new HashSet<>(Arrays.asList(APPLICATION_XML_VALUE, APPLICATION_JSON_VALUE))))
            ),

            // MethodNotAllowedException related parameters
            p(
                new MethodNotAllowedException(HttpMethod.POST, null),
                METHOD_NOT_ALLOWED,
                HttpStatus.METHOD_NOT_ALLOWED,
                singletonList(Argument.arg("method", "POST"))
            ),

            // ServerWebInputException related parameters
            p(
                new ServerWebInputException("", null, mismatch("name", "invalid", String.class)),
                TypeMismatchWebErrorHandler.TYPE_MISMATCH + ".name",
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("expected", "String"),
                    Argument.arg("invalid", "invalid"),
                    Argument.arg("property", "name")
                )
            ),
            p(
                new ServerWebInputException("", parameter("pro"), new TypeMismatchException("invalid", String.class)),
                TypeMismatchWebErrorHandler.TYPE_MISMATCH + ".pro",
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("expected", "String"),
                    Argument.arg("invalid", "invalid"),
                    Argument.arg("property", "pro")
                )
            ),

            // Missing Request Params
            p(
                new ServerWebInputException("", annotatedParameter("name", param("another"), RequestParam.class)),
                MISSING_PARAMETER,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "another"),
                    Argument.arg("expected", "String")
                )
            ),
            p(
                new ServerWebInputException("", annotatedParameter("name", param(""), RequestParam.class)),
                MISSING_PARAMETER,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "name"),
                    Argument.arg("expected", "String")
                )
            ),

            // Missing Request Headers
            p(
                new ServerWebInputException("", annotatedParameter("name", header("another"), RequestHeader.class)),
                MISSING_HEADER,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "another"),
                    Argument.arg("expected", "String")
                )
            ),
            p(
                new ServerWebInputException("", annotatedParameter("name", header(""), RequestHeader.class)),
                MISSING_HEADER,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "name"),
                    Argument.arg("expected", "String")
                )
            ),

            // Missing Cookies
            p(
                new ServerWebInputException("", annotatedParameter("name", cookie("another"), CookieValue.class)),
                MISSING_COOKIE,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "another"),
                    Argument.arg("expected", "String")
                )
            ),
            p(
                new ServerWebInputException("", annotatedParameter("name", cookie(""), CookieValue.class)),
                MISSING_COOKIE,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "name"),
                    Argument.arg("expected", "String")
                )
            ),

            // Missing Matrix Variables
            p(
                new ServerWebInputException("", annotatedParameter("name", matrix("another"), MatrixVariable.class)),
                MISSING_MATRIX_VARIABLE,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "another"),
                    Argument.arg("expected", "String")
                )
            ),
            p(
                new ServerWebInputException("", annotatedParameter("name", matrix(""), MatrixVariable.class)),
                MISSING_MATRIX_VARIABLE,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "name"),
                    Argument.arg("expected", "String")
                )
            ),

            // Missing Parts
            p(
                new ServerWebInputException("", annotatedParameter("name", part("another"), RequestPart.class)),
                MISSING_PART,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "another"),
                    Argument.arg("expected", "String")
                )
            ),
            p(
                new ServerWebInputException("", annotatedParameter("name", part(""), RequestPart.class)),
                MISSING_PART,
                HttpStatus.BAD_REQUEST,
                Arrays.asList(
                    Argument.arg("name", "name"),
                    Argument.arg("expected", "String")
                )
            ),

            p(
                new ServerWebInputException("", null),
                INVALID_OR_MISSING_BODY,
                HttpStatus.BAD_REQUEST,
                emptyList()
            ),

            // General ResponseStatusException
            p(
                new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY),
                LastResortWebErrorHandler.UNKNOWN_ERROR_CODE,
                HttpStatus.UNPROCESSABLE_ENTITY,
                emptyList()
            ),
            p(
                new ResponseStatusException(HttpStatus.NOT_FOUND),
                NO_HANDLER,
                HttpStatus.NOT_FOUND,
                emptyList()
            ),

            // Last but not least
            p(
                new RuntimeException(),
                LastResortWebErrorHandler.UNKNOWN_ERROR_CODE,
                HttpStatus.INTERNAL_SERVER_ERROR,
                emptyList()
            )
        );
    }

    private TypeMismatchException mismatch(String property, Object invalid, Class<?> type) {
        TypeMismatchException exception = mock(TypeMismatchException.class);
        when(exception.getPropertyName()).thenReturn(property);
        doReturn(type).when(exception).getRequiredType();
        when(exception.getValue()).thenReturn(invalid);

        return exception;
    }

    private MethodParameter parameter(String name) {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.getParameterName()).thenReturn(name);

        return parameter;
    }

    private <T extends Annotation> MethodParameter annotatedParameter(String name, T annotation, Class<T> token) {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.getParameterName()).thenReturn(name);
        doReturn(name.getClass()).when(parameter).getParameterType();
        when(parameter.getParameterAnnotation(token)).thenReturn(annotation);

        return parameter;
    }

    private RequestParam param(String name) {
        RequestParam param = mock(RequestParam.class);
        when(param.name()).thenReturn(name);

        return param;
    }

    private RequestHeader header(String name) {
        RequestHeader header = mock(RequestHeader.class);
        when(header.name()).thenReturn(name);

        return header;
    }

    private RequestPart part(String name) {
        RequestPart part = mock(RequestPart.class);
        when(part.name()).thenReturn(name);

        return part;
    }

    private CookieValue cookie(String name) {
        CookieValue cookie = mock(CookieValue.class);
        when(cookie.name()).thenReturn(name);

        return cookie;
    }

    private MatrixVariable matrix(String name) {
        MatrixVariable matrix = mock(MatrixVariable.class);
        when(matrix.name()).thenReturn(name);

        return matrix;
    }
}