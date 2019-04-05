package me.alidg.errors;

import java.util.Objects;

/**
 * Represents a single named exception argument.
 *
 * @author zarebski-m
 */
public final class Argument {

    /**
     * Name of the argument.
     */
    private final String name;

    /**
     * Value of the argument.
     */
    private final Object value;

    private Argument(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Creates an {@link Argument} instance based on the given name-value pair.
     *
     * @param name  Name of the argument.
     * @param value Value of the argument.
     * @return Instance of {@link Argument}.
     */
    public static Argument arg(String name, Object value) {
        return new Argument(name, value);
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

    /**
     * Two arguments are equal iff they share the same name and value.
     *
     * @param other The other one to compare.
     * @return {@code true} if they share the same name and value, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Argument argument = (Argument) other;

        return Objects.equals(getName(), argument.getName()) &&
            Objects.equals(getValue(), argument.getValue());
    }

    /**
     * @return The equals compatible hashcode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getValue());
    }
}
