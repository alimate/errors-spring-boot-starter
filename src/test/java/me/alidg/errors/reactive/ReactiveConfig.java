package me.alidg.errors.reactive;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebFlux
@EnableWebFluxSecurity
public class ReactiveConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ServerAccessDeniedHandler accessDeniedHandler,
                                                         ServerAuthenticationEntryPoint authenticationEntryPoint) {
        return http
            .csrf()
            .accessDeniedHandler(accessDeniedHandler)
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler)
            .and()
            .authorizeExchange()
            .pathMatchers(GET, "/test/protected").authenticated()
            .pathMatchers(POST, "/test/protected").hasRole("ADMIN")
            .anyExchange().permitAll()
            .and().build();
    }
}
