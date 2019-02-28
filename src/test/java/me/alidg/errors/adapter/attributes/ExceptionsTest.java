package me.alidg.errors.adapter.attributes;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Exceptions} class.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class ExceptionsTest {

    @Test
    @Parameters(method = "provideParams")
    public void refineUnknownException_ShouldMapTheStatusCodeToExceptionAsExpected(Object code, String expected) {
        Map<String, Object> attributes = null;
        if (code != null) {
            attributes = new HashMap<>();
            attributes.put("status", code);
        }


        assertThat(Exceptions.refineUnknownException(attributes).getClass().getSimpleName())
                .isEqualTo(expected);
    }

    private Object[] provideParams() {
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
