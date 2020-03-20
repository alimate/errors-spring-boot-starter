package me.alidg.errors.handlers;

import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ClassNameHelper {
    private ClassNameHelper() {}

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
