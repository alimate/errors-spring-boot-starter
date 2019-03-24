package me.alidg.errors.message;

import me.alidg.errors.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Interpolator for resolving arguments in message templates read from {@link #messageSource}.
 *
 * @author zarebski-m
 */
public class ErrorMessageInterpolator {

    /**
     * Plain old logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ErrorMessageInterpolator.class);

    /**
     * The message source implementation to delegate the message resolution process to.
     */
    private final MessageSource messageSource;

    /**
     * Parses templated expressions and replaces placeholders with their corresponding values.
     */
    private final TemplateParser templateParser = new TemplateParser();

    /**
     * Construct an instance of {@link ErrorMessageInterpolator} with the given {@code messageSource}
     * to resolve messages.
     *
     * @param messageSource Source of message templates.
     * @throws NullPointerException When the given message source is null.
     */
    public ErrorMessageInterpolator(@NonNull MessageSource messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource, "The message source is required");
    }

    /**
     * Interpolates message templates resolved from {@link #messageSource}.
     *
     * @param code      Error code a.k.a. message template key from {@link #messageSource}.
     * @param arguments Error arguments to interpolate/substitute in message template.
     * @param locale    Locale.
     * @return String with resolved arguments.
     */
    @Nullable
    public String interpolate(@NonNull String code, @NonNull List<Argument> arguments, @NonNull Locale locale) {
        try {
            String template = messageSource.getMessage(code, null, locale);
            return templateParser.parse(template, arguments);
        } catch (NoSuchMessageException e) {
            return null;
        } catch (Exception e) {
            logger.warn("Failed to interpolate a message", e);
            return null;
        }
    }
}
