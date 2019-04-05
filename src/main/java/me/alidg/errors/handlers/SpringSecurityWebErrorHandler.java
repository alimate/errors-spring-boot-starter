package me.alidg.errors.handlers;

import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.springframework.http.HttpStatus.*;

/**
 * A {@link WebErrorHandler} implementation responsible for handling Spring Security
 * related exceptions.
 *
 * @author Ali Dehghani
 */
public class SpringSecurityWebErrorHandler implements WebErrorHandler {

    /**
     * When the client is not authorized to access a protected resource.
     */
    public static final String ACCESS_DENIED = "security.access_denied";

    /**
     * When the user account is expired.
     */
    public static final String ACCOUNT_EXPIRED = "security.account_expired";

    /**
     * When someone tried to access a protected resource without proper authorization.
     */
    public static final String AUTH_REQUIRED = "security.auth_required";

    /**
     * When something unexpected happened during security checks.
     */
    public static final String INTERNAL_ERROR = "security.internal_error";

    /**
     * When the given user credentials where wrong.
     */
    public static final String BAD_CREDENTIALS = "security.bad_credentials";

    /**
     * When the user is locked.
     */
    public static final String USER_LOCKED = "security.user_locked";

    /**
     * When the user is disabled.
     */
    public static final String USER_DISABLED = "security.user_disabled";

    /**
     * Can only handle {@link AccessDeniedException} or {@link AuthenticationException}s.
     *
     * @param exception The exception to examine.
     * @return {@code true} for Spring Security specific exceptions, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof AccessDeniedException ||
            exception instanceof AccountExpiredException ||
            exception instanceof AuthenticationCredentialsNotFoundException ||
            exception instanceof AuthenticationServiceException ||
            exception instanceof BadCredentialsException ||
            exception instanceof UsernameNotFoundException ||
            exception instanceof InsufficientAuthenticationException ||
            exception instanceof LockedException ||
            exception instanceof DisabledException;
    }

    /**
     * Bunch of if-else-es to handle Spring Security specific exceptions.
     *
     * @param exception The exception to handle.
     * @return The handled exception details wrapped inside an instance of {@link HandledException}.
     */
    @NonNull
    @Override
    public HandledException handle(Throwable exception) {
        if (exception instanceof AccessDeniedException)
            return new HandledException(ACCESS_DENIED, FORBIDDEN, null);

        if (exception instanceof AccountExpiredException)
            return new HandledException(ACCOUNT_EXPIRED, BAD_REQUEST, null);

        if (exception instanceof AuthenticationCredentialsNotFoundException)
            return new HandledException(AUTH_REQUIRED, UNAUTHORIZED, null);

        if (exception instanceof AuthenticationServiceException)
            return new HandledException(INTERNAL_ERROR, INTERNAL_SERVER_ERROR, null);

        if (exception instanceof BadCredentialsException)
            return new HandledException(BAD_CREDENTIALS, BAD_REQUEST, null);

        if (exception instanceof UsernameNotFoundException)
            return new HandledException(BAD_CREDENTIALS, BAD_REQUEST, null);

        if (exception instanceof InsufficientAuthenticationException)
            return new HandledException(AUTH_REQUIRED, UNAUTHORIZED, null);

        if (exception instanceof LockedException) return new HandledException(USER_LOCKED, BAD_REQUEST, null);
        if (exception instanceof DisabledException) return new HandledException(USER_DISABLED, BAD_REQUEST, null);

        return new HandledException("unknown_error", INTERNAL_SERVER_ERROR, null);
    }
}
