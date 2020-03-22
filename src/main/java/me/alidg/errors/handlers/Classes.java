package me.alidg.errors.handlers;

import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple utility class to provide useful functions for different class tokens.
 */
final class Classes {

    private Classes() {
    }

    /**
     * Gets the simple class name for the given class token while preserving the
     * class hierarchies.
     *
     * @param clazz The type token.
     * @return The simple class name.
     */
    @NonNull
    static String getClassName(@NonNull Class<?> clazz) {
        List<String> classNames = new ArrayList<>();
        do {
            classNames.add(clazz.getSimpleName());
            clazz = clazz.getEnclosingClass();
        } while (clazz != null);

        Collections.reverse(classNames);
        return String.join(".", classNames);
    }
}
