package me.alidg.errors.conf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * A servlet specific auto-configuration to register an {@link AccessDeniedHandler} and another
 * {@link AuthenticationEntryPoint} when the traditional Spring Security is detected. Using these
 * two handlers will make sure that our exception handling mechanism would properly catch and handle
 * all security related exceptions.
 *
 * @implNote In contrast with other handlers that register themselves automatically, in order to use these
 * two handlers, you should register them in your security configuration manually as follows:
 * <pre>
 * {@code
 * &#64;EnableWebSecurity
 * public class SecurityConfig extends WebSecurityConfigurerAdapter {
 *
 *     private final AccessDeniedHandler accessDeniedHandler;
 *     private final AuthenticationEntryPoint authenticationEntryPoint;
 *
 *     public SecurityConfig(AccessDeniedHandler accessDeniedHandler, AuthenticationEntryPoint authenticationEntryPoint) {
 *         this.accessDeniedHandler = accessDeniedHandler;
 *         this.authenticationEntryPoint = authenticationEntryPoint;
 *     }
 *
 *     &#64;Override
 *     protected void configure(HttpSecurity http) throws Exception {
 *         http
 *                 .exceptionHandling()
 *                     .accessDeniedHandler(accessDeniedHandler)
 *                     .authenticationEntryPoint(authenticationEntryPoint);
 *     }
 * }
 * }
 * </pre>
 *
 * @author Ali Dehghani
 */
@ConditionalOnWebApplication(type = SERVLET)
@ConditionalOnClass(name = "org.springframework.security.web.access.AccessDeniedHandler")
public class ServletSecurityErrorsAutoConfiguration {

    /**
     * The error attribute we're going save the exception under it.
     */
    private static final String ERROR_ATTRIBUTE = "javax.servlet.error.exception";

    /**
     * Registers a handler to handle to access denied exceptions.
     *
     * @return The registered access denied handler.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.security.web.access.AccessDeniedHandler")
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            if (!response.isCommitted()) {
                request.setAttribute(ERROR_ATTRIBUTE, exception);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        };
    }

    /**
     * Registers a handler to handle all authentication exceptions.
     *
     * @return The registered authentication entry point.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.security.web.AuthenticationEntryPoint")
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            if (!response.isCommitted()) {
                request.setAttribute(ERROR_ATTRIBUTE, exception);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        };
    }
}
