package me.alidg.errors.adapter.attributes;

import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.adapter.HttpErrorAttributesAdapter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ServletErrorAttributes}.
 *
 * @author Ali Dehghani
 */
public class ServletErrorAttributesTest {

    @ParameterizedTest
    @MethodSource("provideInvalidParamsToConstructor")
    public void constructor_ShouldEnforceItsPreconditions(WebErrorHandlers handlers,
                                                          HttpErrorAttributesAdapter adapter,
                                                          Class<? extends Throwable> expectedException,
                                                          String expectedMessage) {
        assertThatThrownBy(() -> new ServletErrorAttributes(handlers, adapter))
            .isInstanceOf(expectedException)
            .hasMessage(expectedMessage);
    }

    private static Object[] provideInvalidParamsToConstructor() {
        return p(
            p(null, null, NullPointerException.class, "Web error handlers is required"),
            p(mock(WebErrorHandlers.class), null, NullPointerException.class, "Adapter is required")
        );
    }
}