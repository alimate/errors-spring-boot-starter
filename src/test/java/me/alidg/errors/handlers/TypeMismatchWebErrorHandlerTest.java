package me.alidg.errors.handlers;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.Argument;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;

import java.util.List;

import static java.util.Arrays.asList;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static me.alidg.errors.handlers.TypeMismatchWebErrorHandler.TYPE_MISMATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link TypeMismatchWebErrorHandler}.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class TypeMismatchWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final WebErrorHandler errorHandler = new TypeMismatchWebErrorHandler();

    @Test
    @Parameters(method = "paramsForCanHandle")
    public void canHandle_OnlyReturnsTrueForTypeMismatches(Exception ex, boolean expected) {
        assertThat(errorHandler.canHandle(ex))
            .isEqualTo(expected);
    }

    @Test
    @Parameters(method = "paramsForHandle")
    public void handle_ShouldHandleTypeMismatchExceptionsAppropriately(TypeMismatchException ex, List<Argument> arguments) {
        HandledException handledException = errorHandler.handle(ex);

        String errorCode = String.format("%s.%s", TYPE_MISMATCH, ex.getPropertyName());
        // TODO check arguments assertThat(handledException.getArguments().get(errorCode)).containsAll(arguments);
        assertThat(handledException.getErrorCodes()).contains(errorCode);
        assertThat(handledException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Object[] paramsForCanHandle() {
        return p(
            p(mock(TypeMismatchException.class), true),
            p(new RuntimeException(), false),
            p(null, false)
        );
    }

    private Object[] paramsForHandle() {
        return p(
            p(
                mismatch("username", "ali", String.class),
                asList(arg("property", "username"), arg("invalid", "ali"), arg("expected", "String"))
            ),
            p(
                mismatch("username", "ali", null),
                asList(arg("property", "username"), arg("invalid", "ali"))
            )
        );
    }

    private TypeMismatchException mismatch(String name, String value, Class<?> type) {
        TypeMismatchException exception = mock(TypeMismatchException.class);
        when(exception.getPropertyName()).thenReturn(name);
        when(exception.getPropertyName()).thenReturn(name);
        doReturn(type).when(exception).getRequiredType();
        when(exception.getValue()).thenReturn(value);

        return exception;
    }
}