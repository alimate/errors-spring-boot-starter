package me.alidg.errors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.MessageSource;

import java.util.List;

import static java.util.Collections.emptyList;
import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link WebErrorHandlers} factory.
 *
 * @author Ali Dehghani
 */
public class WebErrorHandlersTest {

    @ParameterizedTest
    @SuppressWarnings("deprecation")
    @MethodSource("paramsForConstructor")
    public void constructor_ShouldEnforceItsPreconditions(MessageSource messageSource,
                                                          List<WebErrorHandler> handlers,
                                                          Class<? extends Throwable> expectedException,
                                                          String expectedMessage) {
        assertThatThrownBy(() -> new WebErrorHandlers(messageSource, handlers, null, null, null))
            .isInstanceOf(expectedException)
            .hasMessage(expectedMessage);
    }

    private static Object[] paramsForConstructor() {
        return p(
            p(null, null, NullPointerException.class, "We need a MessageSource implementation to message translation"),
            p(mock(MessageSource.class), null, NullPointerException.class, "Collection of error handlers is required"),
            p(mock(MessageSource.class), emptyList(), IllegalArgumentException.class, "We need at least one error handler")
        );
    }
}