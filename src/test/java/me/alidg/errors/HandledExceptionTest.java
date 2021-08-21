package me.alidg.errors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static me.alidg.Params.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Unit tests for {@link HandledException} class.
 *
 * @author Ali Dehghani
 */
public class HandledExceptionTest {

    @ParameterizedTest
    @MethodSource("provideParamsForPrimary")
    public void primaryConstructor_ShouldEnforceItsPreconditions(Set<String> errorCodes,
                                                                 HttpStatus status,
                                                                 Class<? extends Throwable> expected,
                                                                 String message) {
        assertThatThrownBy(() -> new HandledException(errorCodes, status, singletonMap("error", emptyList())))
            .isInstanceOf(expected)
            .hasMessage(message);
    }

    @ParameterizedTest
    @MethodSource( "provideParamsForSecondary")
    public void secondConstructor_ShouldEnforceItsPreconditions(String errorCode,
                                                                HttpStatus status,
                                                                Class<? extends Throwable> expected,
                                                                String message) {
        assertThatThrownBy(() -> new HandledException(errorCode, status, singletonMap("error", emptyList())))
            .isInstanceOf(expected)
            .hasMessage(message);
    }

    @ParameterizedTest
    @MethodSource( "provideMaps")
    public void constructors_ShouldSetNullArgumentsAsEmptyMaps(Map<String, List<Argument>> provided,
                                                               Map<?, ?> expected) {
        assertThat(new HandledException(singleton("error"), BAD_REQUEST, provided).getArguments())
            .isEqualTo(expected);

        assertThat(new HandledException("error", BAD_REQUEST, provided).getArguments())
            .isEqualTo(expected);
    }

    private static Object[] provideParamsForPrimary() {
        return p(
            p(null, null, NullPointerException.class, "Error codes is required"),
            p(new HashSet<>(asList("", "", null)), null, NullPointerException.class, "Status code is required"),
            p(singleton(null), BAD_REQUEST, NullPointerException.class, "The single error code can't be null"),
            p(emptySet(), BAD_REQUEST, IllegalArgumentException.class, "At least one error code should be provided")
        );
    }

    private static Object[] provideParamsForSecondary() {
        return p(
            p(null, null, NullPointerException.class, "Status code is required"),
            p("error", null, NullPointerException.class, "Status code is required"),
            p(null, BAD_REQUEST, NullPointerException.class, "The single error code can't be null")
        );
    }

    private static Object[] provideMaps() {
        return p(
            p(null, emptyMap()),
            p(singletonMap("key", emptyList()), singletonMap("key", emptyList()))
        );
    }
}
