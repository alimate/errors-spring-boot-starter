package me.alidg.errors;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Provides a fingerprint for given {@link HttpError}.
 *
 * <p>
 * A fingerprint helps with identifying errors across domains.
 * With fingerprint you can easily correlate error reported in
 * application (e.g. as entry in application log) with user-friendly
 * error reported via HTTP (which doesn't - and shouldn't -  contain
 * vital information, like e.g. stacktrace).
 *
 * @author zarebski-m
 */
public interface FingerprintProvider {

    /**
     * Generates a hopefully unique fingerprint from the given {@code httpError}.
     *
     * @param httpError Error event for which fingerprint is generated.
     * @return Fingerprint - an identifier of given error event.
     */
    @Nullable String generate(@NonNull HttpError httpError);
}
