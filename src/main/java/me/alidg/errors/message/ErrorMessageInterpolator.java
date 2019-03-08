package me.alidg.errors.message;

import me.alidg.errors.Argument;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.context.MessageSource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Interpolator for resolving arguments in message templates read from {@link #messageSource}.
 *
 * It is able to interpolate named arguments when created with {@link #namedArguments} being {@code true}.
 */
public class ErrorMessageInterpolator {

    private final MessageSource messageSource;
    private final boolean namedArguments;

    /**
     *
     * @param messageSource Source of message templates.
     * @param namedArguments Use named argument interpolation when {@code true}. If {@code false}, use positional interpolation.
     */
    public ErrorMessageInterpolator(@NonNull MessageSource messageSource, boolean namedArguments) {
        this.messageSource = messageSource;
        this.namedArguments = namedArguments;
    }

    /**
     * Interpolate message template read from {@link #messageSource}.
     *
     * @param code Error code a.k.a. message template key from {@link #messageSource}.
     * @param arguments Error arguments to interpolate/substitute in message template.
     * @param locale Locale.
     *
     * @return String with resolved arguments.
     */
    @Nullable
    public String interpolate(@NonNull String code, @NonNull List<Argument> arguments, @NonNull Locale locale) {
        return namedArguments
                ? interpolateNamedArgs(code, arguments, locale)
                : interpolatePositionalArgs(code, arguments, locale);
    }

    private String interpolatePositionalArgs(String code, List<Argument> arguments, Locale locale) {
        Object[] args = arguments.stream().map(Argument::getValue).toArray(Object[]::new);
        return messageSource.getMessage(code, args, locale);
    }

    private String interpolateNamedArgs(String code, List<Argument> arguments, Locale locale) {
        String template = messageSource.getMessage(code, null, locale);
        StringSubstitutor substitutor = new StringSubstitutor(new ArgumentLookup(arguments), "{", "}", '\\');
        return substitutor.replace(template);
    }
}
