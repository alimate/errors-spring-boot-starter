package me.alidg.errors;

import java.util.Objects;

public class Argument {
    private final String name;
    private final Object value;

    public static Argument arg(String name, Object value) {
        return new Argument(name, value);
    }

    private Argument(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

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
