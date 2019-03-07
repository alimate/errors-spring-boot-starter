package me.alidg.errors.adapter;

import me.alidg.errors.HttpError;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static me.alidg.errors.Argument.arg;
import static org.assertj.core.api.Assertions.entry;
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
        HttpError.CodedMessage first = new HttpError.CodedMessage("f", null, emptyList());
        HttpError.CodedMessage sec = new HttpError.CodedMessage("s", "a message", singletonList(arg("param", 123)));

        HttpError httpError = new HttpError(asList(first, sec), HttpStatus.BAD_REQUEST);
        assertThat(httpError.toString()).isNotNull();

        Map<String, Object> adapted = adapter.adapt(httpError);

        List<Map<String, Object>> errors = (List<Map<String, Object>>) adapted.get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.get(0)).containsOnlyKeys("code", "message");
        assertThat(errors.get(0)).containsValues("f", null);
        assertThat(errors.get(0)).containsOnly(entry("code", "f"), entry("message", null));
        assertThat(errors.get(1)).containsOnly(entry("code", "s"), entry("message", "a message"), entry("arguments", singletonMap("param", 123)));

        System.out.println(errors);
    }

    @Test
    public void adapt_ShouldAdaptFingerprintToAMapProperly() {
        HttpError httpError = new HttpError(emptyList(), HttpStatus.BAD_REQUEST);
        httpError.setFingerprint("fingerprint");

        Map<String, Object> adapted = adapter.adapt(httpError);

        assertThat(adapted).contains(entry("fingerprint", "fingerprint"));
    }
}
