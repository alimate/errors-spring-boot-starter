package me.alidg.errors;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic unit test that checks equals and hashCode contract.
 */
@RunWith(JUnitParamsRunner.class)
public class EqualsAndHashCodeTest {

    @Test
    @Parameters(method = "provideParams")
    public void testEqualsAndHashCode(Object obj, Object equalObj, Object notEqualObj) {
        assertThat(obj).isNotEqualTo(null);
        assertThat(obj).isEqualTo(obj);
        assertThat(obj).isEqualTo(equalObj);
        assertThat(obj).isNotEqualTo(notEqualObj);

        assertThat(obj.hashCode()).isEqualTo(equalObj.hashCode());
    }

    private Object[] provideParams() {
        return p(
                p(arg("name", "value"), arg("name", "value"), arg("differentName", "differentValue")),
                p(
                        new HttpError.CodedMessage("code", "message", emptyList()),
                        new HttpError.CodedMessage("code", "message", emptyList()),
                        new HttpError.CodedMessage("code", "differentMessage", emptyList())
                ),
                p(
                        new HttpError.CodedMessage("code", "message", emptyList()),
                        new HttpError.CodedMessage("code", "message", emptyList()),
                        new HttpError.CodedMessage("code", "message", singletonList(arg("foo", "bar")))
                )
        );
    }
}
