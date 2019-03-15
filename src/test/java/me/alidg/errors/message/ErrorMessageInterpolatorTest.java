package me.alidg.errors.message;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import me.alidg.errors.Argument;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static me.alidg.Params.p;
import static me.alidg.errors.Argument.arg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class ErrorMessageInterpolatorTest {

    private MessageSource messageSource = mock(MessageSource.class);

    @Test
    @Parameters(method = "provideParamsForNamedArguments")
    public void messageInterpolation_WithNamedArguments(String template, List<Argument> arguments, String expectedMessage) {
        ErrorMessageInterpolator interpolator = new ErrorMessageInterpolator(messageSource);

        when(messageSource.getMessage(eq("code"), isNull(), any(Locale.class))).thenReturn(template);

        String result = interpolator.interpolate("code", arguments, Locale.ROOT);

        verify(messageSource, times(1)).getMessage(eq("code"), isNull(), any(Locale.class));
        verifyNoMoreInteractions(messageSource);

        assertThat(result).isEqualTo(expectedMessage);
    }

    private Object[] provideParamsForNamedArguments() {
        return p(
                args("Some message", emptyList(), "Some message"),
                args(null, singletonList(arg("arg", "value")), null),
                args("arg={arg}", singletonList(arg("arg", "resolved")), "arg=resolved"),
                args("arg1={min}, arg2={max}", asList(arg("max", 100), arg("min", -100)), "arg1=-100, arg2=100"),
                args("arg1={min}, arg2={0}", asList(arg("max", 100), arg("min", -100)), "arg1=-100, arg2=100"),
                args("arg1={0}, arg2={1}", asList(arg("min", 100), arg("0", -100)), "arg1=-100, arg2=-100"),
                args("arg1={1}, arg2={0}", asList(arg("max", 100), arg("min", -100)), "arg1=-100, arg2=100"),
                args("arg1={min}, arg2=\\{max}", asList(arg("max", 100), arg("min", -100)), "arg1=-100, arg2={max}"),
                args("arg1={arg1}", asList(arg("arg1", "resolved"), arg("arg2", "skipped")), "arg1=resolved"),
                args("arg1={arg}, arg2={unresolved}", singletonList(arg("arg", "resolved")), "arg1=resolved, arg2={unresolved}"),
                args("arg1={arg1}, arg2={arg2}, arg1again={arg1}", asList(arg("arg1", 111), arg("arg2", 222)), "arg1=111, arg2=222, arg1again=111")
        );
    }

    private static Object[] args(String template, List<Argument> arguments, String expectedMessage) {
        return p(template, arguments, expectedMessage);
    }
}