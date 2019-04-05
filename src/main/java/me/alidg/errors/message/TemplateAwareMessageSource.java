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
 * A simple wrapper for {@link MessageSource} capable of resolving templated expressions. Basically, we first
 * resolve the message template using the wrapped {@link #messageSource}. Then {@link #templateParser} would
 * help us to replace all placeholders with their corresponding argument value.
 *
 * @author zarebski-m
 * @see TemplateParser
 */
public class TemplateAwareMessageSource {

    /**
     * Plain old logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(TemplateAwareMessageSource.class);

    /**
     * The message source implementation to delegate the message resolution process to.
     */
    private final MessageSource messageSource;

    /**
     * Parses templated expressions and replaces placeholders with their corresponding values.
     */
    private final TemplateParser templateParser = new TemplateParser();

    /**
     * Construct an instance of {@link TemplateAwareMessageSource} with the given {@code messageSource}
     * to resolve messages.
     *
     * @param messageSource Source of message templates.
     * @throws NullPointerException When the given message source is null.
     */
    public TemplateAwareMessageSource(@NonNull MessageSource messageSource) {
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
