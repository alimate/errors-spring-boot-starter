package me.alidg.errors.message;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.Argument;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link TemplateAwareMessageSource}.
 *
 * @author zarebski-m
 */
@RunWith(JUnitParamsRunner.class)
public class TemplateAwareMessageSourceIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TemplateAwareMessageSourceTestConfig.class);

    private static Object[] args(String template, List<Argument> arguments, String expectedMessage) {
        return p(template, arguments, expectedMessage);
    }

    @Test
    @Parameters(method = "provideParamsForNamedArguments")
    public void messageInterpolation_WithNamedArguments(String code,
                                                        List<Argument> arguments,
                                                        String expectedMessage) {

        contextRunner.run(ctx -> {
            TemplateAwareMessageSource interpolator = ctx.getBean(TemplateAwareMessageSource.class);

            String result = interpolator.interpolate(code, arguments, Locale.ROOT);

            assertThat(result).isEqualTo(expectedMessage);
        });
    }

    private Object[] provideParamsForNamedArguments() {
        return p(
            args("code.literal", emptyList(), "Some message {}"),
            args("code.literal", null, "Some message {}"),
            args("code.literal", singletonList(arg("arg", "resolved")), "Some message {}"),
            args("invalid", singletonList(arg("arg", "value")), null),
            args("code.simple-named", singletonList(arg("arg", "resolved")), "arg=resolved"),
            args("code.simple-named", singletonList(arg("arg", null)), "arg=null"),
            args("code.simple-named", singletonList(arg("other", "resolved")), "arg={arg}"),
            args("code.simple-named", null, "arg={arg}"),
            args("code.simple-named", emptyList(), "arg={arg}"),
            args("code.two-named", asList(arg("max", 100), arg("min", -100)), "arg1=-100, arg2=100"),
            args("code.named-pos", asList(arg("max", 100), arg("min", -100)), "arg1=-100, arg2=100"),
            args("code.two-pos", asList(arg("min", 100), arg("0", -100)), "arg1=-100, arg2=-100"),
            args("code.two-pos-inv", asList(arg("max", 100), arg("min", -100)), "arg1=-100, arg2=100"),
            args("code.named-with-escape", asList(arg("max", 100), arg("min", -100)), "arg1=-100, arg2={max} {0} is escaped {max} \\"),
            args("code.another-named", asList(arg("arg1", "resolved"), arg("arg2", "skipped")), "arg1=resolved"),
            args("code.unresolved1", singletonList(arg("arg", "resolved")), "arg1=resolved, arg2={unresolved} {0} is escaped {not closed {vr"),
            args("code.unresolved2", singletonList(arg("arg", "resolved")), "arg1=resolved, arg2={1}"),
            args("code.unresolved3", singletonList(arg("arg", "arg")), "arg1={0.0}"),
            args("code.repeat", asList(arg("arg1", 111), arg("arg2", 222)), "arg1=111, arg2=222, arg1again=111 The second arg was 222")
        );
    }

    static class TemplateAwareMessageSourceTestConfig {

        @Bean
        public MessageSource messageSource() {
            ResourceBundleMessageSource source = new ResourceBundleMessageSource();
            source.setBasename("templated_messages");

            return source;
        }

        @Bean
        public TemplateAwareMessageSource templateAwareMessageSource(MessageSource messageSource) {
            return new TemplateAwareMessageSource(messageSource);
        }
    }
}