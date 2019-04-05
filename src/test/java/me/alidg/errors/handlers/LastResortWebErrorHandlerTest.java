package me.alidg.errors.handlers;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;

import static java.util.Collections.singleton;
import static me.alidg.Params.p;
import static me.alidg.errors.handlers.LastResortWebErrorHandler.INSTANCE;
import static me.alidg.errors.handlers.LastResortWebErrorHandler.UNKNOWN_ERROR_CODE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LastResortWebErrorHandler} exception handler.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class LastResortWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final WebErrorHandler handler = INSTANCE;

    @Test
    @Parameters(method = "provideParams")
    public void canHandle_AlwaysReturnsFalse(Throwable exception) {
        assertThat(handler.canHandle(exception))
            .isFalse();
    }

    @Test
    @Parameters(method = "provideParams")
    public void handle_AlwaysReturn500InternalErrorWithStaticErrorCode(Throwable exception) {
        HandledException handled = handler.handle(exception);

        assertThat(handled.getErrorCodes()).isEqualTo(singleton(UNKNOWN_ERROR_CODE));
        assertThat(handled.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(handled.getArguments()).isEmpty();
    }

    private Object[] provideParams() {
        return p(null, new RuntimeException(), new OutOfMemoryError());
    }
}
