package me.alidg.errors.annotation;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

import java.lang.annotation.*;

/**
 * {@link ImportAutoConfiguration Auto-configuration imports} to enable web error handlers
 * support for Spring MVC and Web Flux tests. Suppose you're going to test a controller named {@code UserController}:
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
@ImportAutoConfiguration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConfigureErrors {
}
