package me.alidg.errors.adapter.attributes;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;

import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Exceptions} class.
 *
 * @author Ali Dehghani
 */
public class ExceptionsTest {

    @ParameterizedTest
    @MethodSource("provideParams")
    public void refineUnknownException_ShouldMapTheStatusCodeToExceptionAsExpected(Object code, String expected) {
        Map<String, Object> attributes = null;
        if (code != null) {
            attributes = new HashMap<>();
            attributes.put("status", code);
        }

        assertThat(Exceptions.refineUnknownException(attributes).getClass().getSimpleName())
            .isEqualTo(expected);
    }

    private static Object[] provideParams() {
        return p(
            p("dqd", "IllegalStateException"),
            p(null, "IllegalStateException"),
            p(12, "IllegalStateException"),
            p(401, "UnauthorizedException"),
            p(403, "ForbiddenException"),
            p(404, "HandlerNotFoundException")
        );
    }
}