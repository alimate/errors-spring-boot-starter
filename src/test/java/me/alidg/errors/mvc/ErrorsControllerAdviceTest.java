package me.alidg.errors.mvc;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.adapter.HttpErrorAttributesAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;

import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ErrorsControllerAdvice}.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class ErrorsControllerAdviceTest {

    @Test
    @Parameters(method = "provideParamsForConstructor")
    public void constructor_ShouldEnforceItsPreconditions(WebErrorHandlers handlers,
                                                          HttpErrorAttributesAdapter adapter,
                                                          String message) {
        assertThatThrownBy(() -> new ErrorsControllerAdvice(handlers, adapter) {
        })
            .isInstanceOf(NullPointerException.class)
            .hasMessage(message);
    }

    private Object[] provideParamsForConstructor() {
        return p(
            p(null, null, "Error handlers is required"),
            p(mock(WebErrorHandlers.class), null, "Adapter is required")
        );
    }
}
