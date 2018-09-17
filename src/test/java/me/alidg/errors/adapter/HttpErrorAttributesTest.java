package me.alidg.errors.adapter;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.WebErrorHandlers;
import org.junit.Test;
import org.junit.runner.RunWith;

import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link HttpErrorAttributes}.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class HttpErrorAttributesTest {

    @Test
    @Parameters(method = "provideInvalidParamsToConstructor")
    public void constructor_ShouldEnforceItsPreconditions(WebErrorHandlers handlers,
                                                          HttpErrorAttributesAdapter adapter,
                                                          Class<? extends Throwable> expectedException,
                                                          String expectedMessage) {
        assertThatThrownBy(() -> new HttpErrorAttributes(handlers, adapter))
                .isInstanceOf(expectedException)
                .hasMessage(expectedMessage);
    }

    private Object[] provideInvalidParamsToConstructor() {
        return p(
                p(null, null, NullPointerException.class, "Web error handlers is required"),
                p(mock(WebErrorHandlers.class), null, NullPointerException.class, "Adapter is required")
        );
    }
}
