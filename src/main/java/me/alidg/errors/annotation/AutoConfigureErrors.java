package me.alidg.errors.annotation;

import me.alidg.errors.conf.ErrorsAutoConfiguration;
import me.alidg.errors.conf.ReactiveErrorsAutoConfiguration;
import me.alidg.errors.conf.ServletErrorsAutoConfiguration;
import me.alidg.errors.conf.ServletSecurityErrorsAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;

import java.lang.annotation.*;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} to enable web error handlers
 * support for Spring MVC tests. Suppose you're going to test a controller named {@code UserController}:
 * <pre>
 * {@code
 *
 *     &#64;AutoConfigureErrors
 *     &#64;RunWith(SpringRunner.class)
 *     &#64;WebMvcTest(UserController.class)
 *     public class UserControllerIT {
 *         // test stuff
 *     }
 * }
 * </pre>
 *
 * @author Ali Dehghani
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ImportAutoConfiguration({
        ErrorsAutoConfiguration.class,
        ServletErrorsAutoConfiguration.class,
        ServletErrorsAutoConfiguration.class,
        ReactiveErrorsAutoConfiguration.class,
        ReactiveSecurityAutoConfiguration.class,
        ServletSecurityErrorsAutoConfiguration.class
})
public @interface AutoConfigureErrors {
}
