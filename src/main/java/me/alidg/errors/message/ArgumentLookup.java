package me.alidg.errors.message;

import me.alidg.errors.Argument;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.lookup.StringLookup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ArgumentLookup implements StringLookup {

    private final List<Argument> arguments;
    private final Map<String, Object> argumentMap;

    ArgumentLookup(List<Argument> arguments) {
        this.arguments = arguments;
        this.argumentMap = arguments.stream().collect(Collectors.toMap(Argument::getName, Argument::getValue));
    }

    @Override
    public String lookup(String key) {
        Object value = argumentMap.get(key);
        if (value != null) return String.valueOf(value);

        if (NumberUtils.isParsable(key)) {
            try {
                int position = Integer.parseInt(key);
                if (position < 0 || position >= arguments.size()) return null;
                return String.valueOf(arguments.get(position).getValue());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
