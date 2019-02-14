package me.alidg.errors.adapter;

import me.alidg.errors.HttpError;
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
 *                  "message": "the_message"
 *              }, ...
 *         ]
 *     }
 * </pre>
 *
 * @author Ali Dehghani
 */
public class DefaultHttpErrorAttributesAdapter implements HttpErrorAttributesAdapter {

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
                        errors -> errorDetails(errors, httpError.getHttpStatus().value())
                ));
    }

    private Map<String, Object> errorDetails(Object errors, int status) {
        Map<String, Object> map = new HashMap<>();
        map.put("errors", errors);

        return map;
    }

    private Map<String, String> toMap(HttpError.CodedMessage codedMessage) {
        Map<String, String> map = new HashMap<>();
        map.put("code", codedMessage.getCode());
        map.put("message", codedMessage.getMessage());

        return map;
    }
}
