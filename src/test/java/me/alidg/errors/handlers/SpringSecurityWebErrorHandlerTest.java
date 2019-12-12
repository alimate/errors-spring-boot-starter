package me.alidg.errors.handlers;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.HandledException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;

import static me.alidg.Params.p;
import static me.alidg.errors.handlers.SpringSecurityWebErrorHandler.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

/**
 * Unit tests for {@link SpringSecurityWebErrorHandler} hanlder.
 *
 * @author Ali Dehghani
 */
@RunWith(JUnitParamsRunner.class)
public class SpringSecurityWebErrorHandlerTest {

    /**
     * Subject under test.
     */
    private final SpringSecurityWebErrorHandler handler = new SpringSecurityWebErrorHandler();

    @Test
    @Parameters(method = "provideParamsForCanHandle")
    public void canHandle_CanOnlyHandleSpringSecurityRelatedExceptions(Throwable exception, boolean expected) {
        assertThat(handler.canHandle(exception))
            .isEqualTo(expected);
    }

    @Test
    @Parameters(method = "provideParamsForHandle")
    public void handle_ShouldHandleSecurityRelatedExceptionsProperly(Throwable exception,
                                                                     String expectedErrorCode,
                                                                     HttpStatus expectedStatusCode) {
        HandledException handled = handler.handle(exception);

        assertThat(handled.getStatusCode()).isEqualTo(expectedStatusCode);
        assertThat(handled.getErrorCodes()).containsOnly(expectedErrorCode);
        assertThat(handled.getArguments()).isEmpty();
    }

    private Object[] provideParamsForCanHandle() {
        return p(
            p(null, false),
            p(new IllegalArgumentException(), false),
            p(new AccessDeniedException(""), true),
            p(new LockedException(""), true),
            p(new DisabledException(""), true),
            p(new UsernameNotFoundException(""), true),
            p(new BadCredentialsException(""), true),
            p(new AccountExpiredException(""), true),
            p(new AuthenticationServiceException(""), true),
            p(new InsufficientAuthenticationException(""), true),
            p(new AuthenticationCredentialsNotFoundException(""), true)
        );
    }

    private Object[] provideParamsForHandle() {
        return p(
            p(new AccessDeniedException(""), ACCESS_DENIED, FORBIDDEN),
            p(new LockedException(""), USER_LOCKED, BAD_REQUEST),
            p(new DisabledException(""), USER_DISABLED, BAD_REQUEST),
            p(new UsernameNotFoundException(""), BAD_CREDENTIALS, BAD_REQUEST),
            p(new BadCredentialsException(""), BAD_CREDENTIALS, BAD_REQUEST),
            p(new AccountExpiredException(""), ACCOUNT_EXPIRED, BAD_REQUEST),
            p(new AuthenticationServiceException(""), INTERNAL_ERROR, INTERNAL_SERVER_ERROR),
            p(new InsufficientAuthenticationException(""), AUTH_REQUIRED, UNAUTHORIZED),
            p(new AuthenticationCredentialsNotFoundException(""), AUTH_REQUIRED, UNAUTHORIZED),
            p(new CookieTheftException(""), "unknown_error", INTERNAL_SERVER_ERROR)
        );
    }
}
