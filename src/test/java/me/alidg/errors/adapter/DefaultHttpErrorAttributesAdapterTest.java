package me.alidg.errors.adapter;

import me.alidg.errors.HttpError;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Unit tests for {@link DefaultHttpErrorAttributesAdapter}.
 *
 * @author Ali Dehghani
 */
public class DefaultHttpErrorAttributesAdapterTest {

    /**
     * Subject under test.
     */
    private final HttpErrorAttributesAdapter adapter = new DefaultHttpErrorAttributesAdapter();

    @Test
    @SuppressWarnings("unchecked")
    public void adapt_ShouldAdaptTheHttpErrorToAMapProperly() {
        HttpError.CodedMessage first = new HttpError.CodedMessage("f", null);
        HttpError.CodedMessage sec = new HttpError.CodedMessage("s", "a message");

        HttpError httpError = new HttpError(asList(first, sec), HttpStatus.BAD_REQUEST);

        Map<String, Object> adapted = adapter.adapt(httpError);

        List<Map<String, String>> errors = (List<Map<String, String>>) adapted.get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.get(0)).containsOnlyKeys("code", "message");
        assertThat(errors.get(0)).containsValues("f", null);
        assertThat(errors.get(1)).containsOnlyKeys("code", "message");
        assertThat(errors.get(1)).containsValues("s", "a message");
    }
}
