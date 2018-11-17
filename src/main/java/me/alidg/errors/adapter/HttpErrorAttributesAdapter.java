package me.alidg.errors.adapter;

import me.alidg.errors.HttpError;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Responsible for adapting the {@link HttpError} to a {@link Map} which is
 * compatible with the {@link org.springframework.boot.web.servlet.error.ErrorAttributes}
 * interface.
 *
 * @author Ali Dehghani
 * @see HttpError
 * @see org.springframework.boot.web.servlet.error.ErrorAttributes
 */
public interface HttpErrorAttributesAdapter {

    /**
     * Converts the given {@link HttpError} instance to a {@link Map}.
     *
     * @param httpError The {@link HttpError} to convert.
     * @return The converted {@link Map}.
     */
    @NonNull Map<String, Object> adapt(@NonNull HttpError httpError);
}
