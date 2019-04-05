package me.alidg.errors.handlers;

import me.alidg.errors.Argument;

import javax.validation.ConstraintViolation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private static final Collection<String> IGNORE_ATTRIBUTES = Arrays.asList("groups", "payload", "message");

    private ConstraintViolationArgumentsExtractor() {
    }

    /**
     * Remove mandatory annotation attributes and sort the remaining ones by their key and return their
     * corresponding values as to-be-exposed arguments.
     *
     * @param violation The violation to extract the arguments from.
     * @return To be exposed arguments for the given violation.
     */
    static List<Argument> extract(ConstraintViolation<?> violation) {
        List<Argument> args = violation.getConstraintDescriptor()
            .getAttributes()
            .entrySet()
            .stream()
            .filter(e -> !IGNORE_ATTRIBUTES.contains(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .map(e -> arg(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        args.add(arg("invalid", violation.getInvalidValue()));
        args.add(arg("property", violation.getPropertyPath().toString()));

        return args;
    }
}
