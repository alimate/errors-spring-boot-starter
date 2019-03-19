package me.alidg.errors.message;

import me.alidg.errors.Argument;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.lookup.StringLookup;

import java.util.List;

/**
 * Lookup argument value by name or position in {@link #arguments} list.
 *
 * @author zarebski.m
 */
class ArgumentLookup implements StringLookup {

    private final List<Argument> arguments;

    /**
     * @param arguments List of arguments to lookup.
     */
    ArgumentLookup(List<Argument> arguments) {
        this.arguments = arguments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String lookup(String key) {
        return arguments
                .stream()
                .filter(a -> a.getName().equals(key))
                .map(Argument::getValue)
                .map(v -> v != null ? v.toString() : null)
                .findFirst()
                .orElseGet(() -> lookupPositional(key));
    }

    private String lookupPositional(String key) {
        if (!NumberUtils.isParsable(key)) {
            return null;
        }

        try {
            int position = Integer.parseInt(key);
            if (position < 0 || position >= arguments.size()) {
                return null;
            }
            return String.valueOf(arguments.get(position).getValue());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
