package me.alidg.errors.handlers;

import me.alidg.errors.Argument;

import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static me.alidg.errors.Argument.arg;

/**
 * Utility class for extracting list of named {@link Argument}s from {@link ConstraintViolation}.
 *
 * @author zarebski.m
 */
final class ConstraintViolationArgumentsExtractor {

    /**
     * Collection of Bean Validation attributes to ignore and to not report as arguments.
     */
    private static final Set<String> ignoredArguments;

    static {
        Set<String> s = new HashSet<>(3);
        s.add("groups");
        s.add("payload");
        s.add("message");
        ignoredArguments = Collections.unmodifiableSet(s);
    }

    private ConstraintViolationArgumentsExtractor() {}

    /**
     * Remove mandatory annotation attributes and sort the remaining ones by their key and return their
     * corresponding values as to-be-exposed arguments.
     *
     * @param violation The violation to extract the arguments from.
     * @return To be exposed arguments for the given violation.
     */
    static List<Argument> extract(ConstraintViolation<?> violation) {
        return violation.getConstraintDescriptor()
                .getAttributes()
                .entrySet()
                .stream()
                .filter(e -> !ignoredArguments.contains(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> arg(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
