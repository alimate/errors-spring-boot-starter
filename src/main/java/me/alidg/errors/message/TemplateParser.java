package me.alidg.errors.message;

import me.alidg.errors.Argument;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for parsing string templates and replacing the named or positional arguments
 * with their corresponding values. The parser treats { and } characters as the variable delimiters.
 *
 * <h3>Named Arguments</h3>
 * When the parser sees a templated expression, first it considers that expression as a named argument, even
 * if it looks like a positional one, e.g. {0}. If it could find an argument matching with that template name,
 * it would replace the placeholder with its corresponding argument value. For example, the following expression:
 * <pre>
 *     Your age should be at least {minAge} but you're {invalid} years old.
 * </pre>
 * Would be translated to the following text when {@code minAge=18} and {@code invalid 17}:
 * <pre>
 *     Your age should be at least 18 but you're 17 years old.
 * </pre>
 *
 * <h3>Positional Arguments</h3>
 * If template parser couldn't find a matched argument name for any given placeholder, then it will try positional
 * arguments iff the placeholder is an integer. For example, the following template message:
 * <pre>
 *     The minimum age is {0}
 * </pre>
 * When there is no named argument for code {@code 0}, the first argument will replace the {0} placeholder:
 * <pre>
 *     The minimum age is 18
 * </pre>
 *
 * <h3>Escape Character</h3>
 * If we need to use delimiting characters in a plain text, we can escape them using a backslash. For example, the
 * following text does not contain any placeholder:
 * <pre>
 *     Plain \{text}
 * </pre>
 *
 * <h3>Placeholders with No Values</h3>
 * When there is no corresponding value for a placeholder, the placeholder would remain intact. For example,
 * if there is no corresponding value for {min}, then the placeholder would be in the final outcome.
 *
 * @author Ali Dehghani
 */
final class TemplateParser {

    /**
     * A regex to find all '{...}' patterns which are not escaped with a backslash.
     */
    private final Pattern pattern = Pattern.compile("(?<!\\\\)(\\{[^}^{]*})");

    /**
     * Parses the given templated string and replaces '{...}' placeholders with their corresponding value.
     *
     * <p>Please note that we had to use the thread-safe {@link StringBuffer} here because the
     * {@link Matcher#appendReplacement(StringBuffer, String)} API needs it. But because JVM elides unnecessary
     * locks, there shouldn't be any performance issue.
     *
     * @param template  The templated string to parse.
     * @param arguments The arguments source to read placeholder values from.
     * @return The final interpolated string.
     * @see <a href="https://shipilev.net/jvm/anatomy-quarks/19-lock-elision/">Lock Elision</a>
     */
    String parse(String template, List<Argument> arguments) {
        if (template == null || arguments == null || arguments.isEmpty()) return template;

        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group();

            Object value = extractValue(placeholder, arguments);
            if (value != null) matcher.appendReplacement(sb, value.toString());
        }
        matcher.appendTail(sb);

        return sb.toString().replace("\\{", "{").replace("\\}", "}");
    }

    private Object extractValue(String placeholder, List<Argument> arguments) {
        String variable = getPlaceholderVariable(placeholder);

        // Tries to resolve it as a named argument
        Optional<Argument> argument = arguments.stream().filter(a -> a.getName().equals(variable)).findFirst();
        if (argument.isPresent()) return argument.map(this::argumentValue).get();

        // Otherwise, tries to resolve it as a positional argument
        try {
            int index = Integer.parseInt(variable);
            return argumentValue(arguments.get(index));
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Extracts the placeholder variable by removing the delimiters.
     *
     * @param placeholder The placeholder in {...} format.
     * @return The extracted variable name.
     */
    private String getPlaceholderVariable(String placeholder) {
        return placeholder.substring(1, placeholder.length() - 1);
    }

    private Object argumentValue(Argument argument) {
        Object value = argument.getValue();
        return value == null ? "null" : value;
    }
}
