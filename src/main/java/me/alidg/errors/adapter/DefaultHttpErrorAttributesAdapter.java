package me.alidg.errors.adapter;

import me.alidg.errors.HttpError;
import me.alidg.errors.HttpError.CodedMessage;
import me.alidg.errors.conf.ErrorsProperties;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Default implementation of {@link HttpErrorAttributesAdapter} which converts the given
 * {@link HttpError} to a {@link Map} like following:
 * <pre>
 *     {
 *         "errors": [
 *              {
 *                  "code": "the_code",
 *                  "message": "the_message",
 *                  "arguments": {
 *                      "name": "value"
 *                  }
 *              }, ...
 *         ],
 *         "fingerprint": "value"
 *     }
 * </pre>
 *
 * @author Ali Dehghani
 */
public class DefaultHttpErrorAttributesAdapter implements HttpErrorAttributesAdapter {

    /**
     * Encapsulates the configuration properties to configure the errors starter.
     */
    private final ErrorsProperties errorsProperties;

    /**
     * Constructs an instance of {@link DefaultHttpErrorAttributesAdapter} given the
     * configuration properties.
     *
     * @param errorsProperties Encapsulates the configuration properties to configure the
     *                         errors starter.
     */
    public DefaultHttpErrorAttributesAdapter(ErrorsProperties errorsProperties) {
        this.errorsProperties = errorsProperties;
    }

    /**
     * Converts the given {@link HttpError} to a {@link Map}.
     *
     * @param httpError The {@link HttpError} to convert.
     * @return The adapted {@link Map}.
     */
    @NonNull
    @Override
    public Map<String, Object> adapt(@NonNull HttpError httpError) {
        return httpError.getErrors().stream()
                .map(this::toMap)
                .collect(collectingAndThen(
                        toList(),
                        errors -> errorDetails(errors, httpError)
                ));
    }

    private Map<String, Object> errorDetails(Object errors, HttpError httpError) {
        Map<String, Object> map = new HashMap<>();
        map.put("errors", errors);

        if (httpError.getFingerprint() != null) {
            map.put("fingerprint", httpError.getFingerprint());
        }

        return map;
    }

    private Map<String, Object> toMap(CodedMessage codedMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", codedMessage.getCode());
        error.put("message", codedMessage.getMessage());

        errorsProperties.getExposeArguments().expose(error, codedMessage.getArguments());

        return error;
    }
}
