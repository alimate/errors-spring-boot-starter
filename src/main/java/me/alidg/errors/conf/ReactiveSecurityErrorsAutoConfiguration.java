package me.alidg.errors.conf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

/**
 * A reactive specific auto-configuration to register an {@link ServerAccessDeniedHandler} and another
 * {@link ServerAuthenticationEntryPoint} when the Reactive Spring Security is detected. These two handlers
 * are just gonna eventually delegate the exception handling procedure to plain old
 * {@link me.alidg.errors.WebErrorHandlers}.
 *
 * @author Ali Dehghani
 * @implNote In contrast with other handlers which register themselves automatically, in order to use these
 * two handlers, you should register them in your security configuration manually as follows:
 * <pre>
 * {@code
 * &#64;EnableWebFluxSecurity
 * public class WebFluxSecurityConfig {
 *
 *     // other configurations
 *
 *     &#64;Bean
 *     public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
 *                                                             ServerAccessDeniedHandler accessDeniedHandler,
 *                                                             ServerAuthenticationEntryPoint authenticationEntryPoint) {
 *         http
 *                 .csrf().accessDeniedHandler(accessDeniedHandler)
 *                 .and()
 *                 .exceptionHandling()
 *                     .accessDeniedHandler(accessDeniedHandler)
 *                     .authenticationEntryPoint(authenticationEntryPoint)
 *                 // other configurations
 *
 *         return http.build();
 *     }
 * }
 * }
 * </pre>
 */
@ConditionalOnWebApplication(type = REACTIVE)
@ConditionalOnBean(ErrorWebExceptionHandler.class)
@ConditionalOnClass(name = "org.springframework.security.web.server.authorization.ServerAccessDeniedHandler")
public class ReactiveSecurityErrorsAutoConfiguration {

    /**
     * Responsible for catching all access denied exceptions and delegating them to typical web error handlers
     * to perform the actual exception handling procedures.
     *
     * @param errorWebExceptionHandler Spring Boot's default exception handler which in turn would delegate to our
     *                                 typical error handlers.
     * @return The registered access denied handler.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.security.web.server.authorization.ServerAccessDeniedHandler")
    public ServerAccessDeniedHandler accessDeniedHandler(ErrorWebExceptionHandler errorWebExceptionHandler) {
        return errorWebExceptionHandler::handle;
    }

    /**
     * Responsible for catching all authentication exceptions and delegating them to typical web error handlers
     * to perform the actual exception handling procedures.
     *
     * @param errorWebExceptionHandler Spring Boot's default exception handler which in turn would delegate to our
     *                                 typical error handlers.
     * @return The registered authentication entry point.
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.security.web.server.ServerAuthenticationEntryPoint")
    public ServerAuthenticationEntryPoint authenticationEntryPoint(ErrorWebExceptionHandler errorWebExceptionHandler) {
        return errorWebExceptionHandler::handle;
    }
}
