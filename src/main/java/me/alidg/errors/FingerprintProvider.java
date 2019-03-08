package me.alidg.errors;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Provides a fingerprint for given {@link HttpError}.
 * <p>
 * A fingerprint helps with identifying errors across domains.
 * With fingerprint you can easily correlate error reported in
 * application (e.g. as entry in application log) with user-friendly
 * error reported via HTTP (which doesn't - and shouldn't -  contain
 * vital information, like e.g. stacktrace).
 * <p>
 * Simple implementation of MD5 fingerprinting is available in
 * {@link me.alidg.errors.fingerprint.MD5FingerprintProvider}.
 */
public interface FingerprintProvider {

    /**
     * @param httpError Error event for which fingerprint is generated.
     * @return Fingerprint - an identifier of given error event.
     */
    @Nullable
    String generate(@NonNull HttpError httpError);
}