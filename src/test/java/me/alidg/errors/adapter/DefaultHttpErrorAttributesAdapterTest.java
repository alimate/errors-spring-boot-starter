package me.alidg.errors.adapter;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.Argument;
import me.alidg.errors.HttpError;
import me.alidg.errors.conf.ErrorsProperties;
import me.alidg.errors.conf.ErrorsProperties.ArgumentExposure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Unit tests for {@link DefaultHttpErrorAttributesAdapter}.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class DefaultHttpErrorAttributesAdapterTest {

    @Test
    @SuppressWarnings("unchecked")
    public void adapt_ShouldAdaptTheHttpErrorToAMapProperly() {
        ErrorsProperties errorsProperties = new ErrorsProperties();
        errorsProperties.setExposeArguments(ArgumentExposure.non_empty);
        HttpErrorAttributesAdapter adapter = new DefaultHttpErrorAttributesAdapter(errorsProperties);

        HttpError.CodedMessage first = new HttpError.CodedMessage("f", null, emptyList());
        HttpError.CodedMessage sec = new HttpError.CodedMessage("s", "a message", singletonList(arg("param", 123)));

        HttpError httpError = new HttpError(asList(first, sec), HttpStatus.BAD_REQUEST);
        assertThat(httpError.toString()).isNotNull();

        Map<String, Object> adapted = adapter.adapt(httpError);

        List<Map<String, Object>> errors = (List<Map<String, Object>>) adapted.get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.get(0)).containsOnly(entry("code", "f"), entry("message", null));
        assertThat(errors.get(1)).containsOnly(entry("code", "s"), entry("message", "a message"), entry("arguments", singletonMap("param", 123)));
    }

    @Test
    public void adapt_ShouldAdaptFingerprintToAMapProperly() {
        HttpErrorAttributesAdapter adapter = new DefaultHttpErrorAttributesAdapter(new ErrorsProperties());

        HttpError httpError = new HttpError(emptyList(), HttpStatus.BAD_REQUEST);
        httpError.setFingerprint("fingerprint");

        Map<String, Object> adapted = adapter.adapt(httpError);

        assertThat(adapted).contains(entry("fingerprint", "fingerprint"));
    }

    @Test
    @Parameters(method = "provideExposureParams")
    @SuppressWarnings("unchecked")
    public void adapt_ShouldAdaptTheHttpErrorWithArgumentsToAMapProperly(
            ArgumentExposure exposure,
            List<Argument> arguments,
            boolean parametersFieldPresent) {
        ErrorsProperties errorsProperties = new ErrorsProperties();
        errorsProperties.setExposeArguments(exposure);
        HttpErrorAttributesAdapter adapter = new DefaultHttpErrorAttributesAdapter(errorsProperties);

        HttpError.CodedMessage codedMessage = new HttpError.CodedMessage("c", "msg", arguments);

        HttpError httpError = new HttpError(singletonList(codedMessage), HttpStatus.BAD_REQUEST);

        Map<String, Object> adapted = adapter.adapt(httpError);

        List<Map<String, Object>> errors = (List<Map<String, Object>>) adapted.get("errors");

        assertThat(errors.get(0).containsKey("arguments")).isEqualTo(parametersFieldPresent);
    }

    private Object[] provideExposureParams() {
        return p(
                p(ArgumentExposure.never, emptyList(), false),
                p(ArgumentExposure.never, singletonList(arg("name", "value")), false),
                p(ArgumentExposure.non_empty, emptyList(), false),
                p(ArgumentExposure.non_empty, singletonList(arg("name", "value")), true),
                p(ArgumentExposure.always, emptyList(), true),
                p(ArgumentExposure.always, singletonList(arg("name", "value")), true)
        );
    }
}
