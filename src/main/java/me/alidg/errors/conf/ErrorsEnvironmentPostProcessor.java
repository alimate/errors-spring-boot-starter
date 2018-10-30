package me.alidg.errors.conf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * A custom {@link EnvironmentPostProcessor} which allows to override a particular bean
 * definition, e.g. registering a bean with an already registered name.
 *
 * <p>As of Spring Boot 2.1.0+, Bean overriding has been disabled by default to prevent a bean
 * being accidentally overridden. In order to enable it, we need to set
 * {@code spring.main.allow-bean-definition-overriding} to {@code true}.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.1-Release-Notes#bean-overriding">Bean Overriding</a>
 *
 * @author Ali Dehghani
 */
public class ErrorsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /**
     * Enables bean definition overriding.
     *
     * @param environment The environment.
     * @param application The spring application.
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> enableBeanOverriding = singletonMap("spring.main.allow-bean-definition-overriding", "true");
        MapPropertySource inMemoryPropertySource = new MapPropertySource("errors-starter-property-source", enableBeanOverriding);

        environment.getPropertySources().addLast(inMemoryPropertySource);
    }
}
