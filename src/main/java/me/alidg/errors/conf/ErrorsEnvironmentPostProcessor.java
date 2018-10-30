package me.alidg.errors.conf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * A custom {@link EnvironmentPostProcessor} which allows to override a particular bean
 * definition, e.g. registering a bean with an already registered name.
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
        application.setAllowBeanDefinitionOverriding(true);
    }
}
