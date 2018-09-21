package me.alidg.errors.annotation;

import me.alidg.errors.conf.ErrorsAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

import java.lang.annotation.*;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} to enable web error handlers
 * support for Spring MVC tests. Suppose you're going to test a controller named {@code UserController}:
 * <pre>
 *
 *     &#64;AutoConfigureErrors
 *     &#64;RunWith(SpringRunner.class)
 *     &#64;WebMvcTest(UserController.class)
 *     public class UserControllerIT {
 *         // test stuff
 *     }
 * </pre>
 *
 * @author Ali Dehghani
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ImportAutoConfiguration(ErrorsAutoConfiguration.class)
public @interface AutoConfigureErrors {
}
