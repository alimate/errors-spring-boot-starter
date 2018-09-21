package me.alidg.errors.annotation;

import me.alidg.errors.WebErrorHandlers;
import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureErrors} annotation.
 *
 * @author Ali Dehghani
 */
public class AutoConfigureErrorsIT {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    public void annotation_ShouldEnableTheWebErrorsSupport() {
        contextRunner.run(ctx -> {
            WebErrorHandlers handlers = ctx.getBean(WebErrorHandlers.class);

            assertThat(handlers).isNotNull();
        });
    }


    @TestConfiguration
    @AutoConfigureErrors
    protected static class TestConfig {}
}
