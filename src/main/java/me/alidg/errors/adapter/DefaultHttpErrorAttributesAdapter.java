package me.alidg.errors.adapter;

import me.alidg.errors.Argument;
import me.alidg.errors.HttpError;
import me.alidg.errors.conf.ErrorsProperties;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final ErrorsProperties errorsProperties;

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

    private Map<String, Object> toMap(HttpError.CodedMessage codedMessage) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", codedMessage.getCode());
        map.put("message", codedMessage.getMessage());

        exposeArgumentsIfNeeded(map, codedMessage);

        return map;
    }

    private void exposeArgumentsIfNeeded(Map<String, Object> map, HttpError.CodedMessage codedMessage) {
        switch (errorsProperties.getExposeArguments()) {
            case never:
                return;

            case always:
                map.put("arguments", codedMessage.getArguments().stream().collect(Collectors.toMap(Argument::getName, Argument::getValue)));
                return;

            case non_empty:
                if (!codedMessage.getArguments().isEmpty()) {
                    map.put("arguments", codedMessage.getArguments().stream().collect(Collectors.toMap(Argument::getName, Argument::getValue)));
                }
                return;

            default:
                throw new IllegalStateException();
        }
    }
}
