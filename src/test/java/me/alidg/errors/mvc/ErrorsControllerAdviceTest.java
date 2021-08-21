package me.alidg.errors.mvc;

import me.alidg.errors.WebErrorHandlers;
import me.alidg.errors.adapter.HttpErrorAttributesAdapter;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ErrorsControllerAdvice}.
 *
 * @author Ali Dehghani
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ErrorsControllerAdviceTest {

    @ParameterizedTest
    @MethodSource("provideParamsForConstructor")
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