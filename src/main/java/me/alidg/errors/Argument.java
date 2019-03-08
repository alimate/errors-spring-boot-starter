package me.alidg.errors;

import java.util.Objects;

/**
 * Represents single named exception argument.
 */
public class Argument {
    /**
     * Name of the argument.
     */
    private final String name;

    /**
     * Value of the argument.
     */
    private final Object value;

    /**
     * Named constructor of {@link Argument} instance.
     *
     * @param name  Name of the argument.
     * @param value Value of the argument.
     *
     * @return Instance of {@link Argument}.
     */
    public static Argument arg(String name, Object value) {
        return new Argument(name, value);
    }

    private Argument(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * @return Name of the argument.
     * @see #name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Value of the argument.
     * @see #value
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Argument argument = (Argument) o;
        return Objects.equals(name, argument.name) &&
                Objects.equals(value, argument.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
