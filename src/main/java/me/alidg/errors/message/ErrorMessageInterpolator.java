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
 */
public class ErrorMessageInterpolator {

    private final MessageSource messageSource;

    /**
     * @param messageSource Source of message templates.
     */
    public ErrorMessageInterpolator(@NonNull MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Interpolate message template read from {@link #messageSource}.
     *
     * @param code      Error code a.k.a. message template key from {@link #messageSource}.
     * @param arguments Error arguments to interpolate/substitute in message template.
     * @param locale    Locale.
     * @return String with resolved arguments.
     */
    @Nullable
    public String interpolate(@NonNull String code, @NonNull List<Argument> arguments, @NonNull Locale locale) {
        String template = messageSource.getMessage(code, null, locale);
        StringSubstitutor substitutor = new StringSubstitutor(new ArgumentLookup(arguments), "{", "}", '\\');
        return substitutor.replace(template);
    }
}
