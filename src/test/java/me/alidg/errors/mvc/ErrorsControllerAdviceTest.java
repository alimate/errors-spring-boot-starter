package me.alidg.errors.mvc;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ErrorsControllerAdvice}.
 *
 * @author Ali Dehghani
 */
public class ErrorsControllerAdviceTest {

    @Test
    public void constructor_ShouldEnforceItsPreconditions() {
        assertThatThrownBy(() -> new ErrorsControllerAdvice(null) {})
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Error handlers is required");
    }
}
