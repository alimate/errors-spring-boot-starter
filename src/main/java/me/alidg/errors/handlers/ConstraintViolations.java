package me.alidg.errors.handlers;

import me.alidg.errors.Argument;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.UniqueElements;

import javax.validation.ConstraintViolation;
import javax.validation.constraints.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static me.alidg.errors.Argument.arg;

/**
 * Utility class for extracting list of named {@link Argument}s from {@link ConstraintViolation}.
 *
 * @author zarebski.m
 */
final class ConstraintViolations {

    /**
     * Collection of prefixes representing default validation error codes.
     */
    private static final List<String> DEFAULT_ERROR_CODES_PREFIX = asList("{javax.validation.", "{org.hibernate.validator");

    /**
     * A mapping between constraint annotations and error codes.
     */
    private static final Map<Class<? extends Annotation>, String> ERROR_CODE_MAPPING = initErrorCodeMapping();

    /**
     * Collection of Bean Validation attributes to ignore and to not report as arguments.
     */
    private static final Collection<String> IGNORE_ATTRIBUTES = asList("groups", "payload", "message");

    private ConstraintViolations() {
    }

    /**
     * Remove mandatory annotation attributes and sort the remaining ones by their key and return their
     * corresponding values as to-be-exposed arguments.
     *
     * @param violation The violation to extract the arguments from.
     * @return To be exposed arguments for the given violation.
     */
    static List<Argument> getArguments(ConstraintViolation<?> violation) {
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

    /**
     * Extracts the error code for this particular constraint violation. If the {@link ConstraintViolation#getMessageTemplate()}
     * seems to contain a customized error code, then we return that value as the error code. Otherwise, the default error
     * code generator comes to play. For each constraint violation, we're going to generate a sensible default  error code.
     *
     * @param violation The constraint violation representing a validation error.
     * @return The custom or default error code.
     */
    static String getErrorCode(ConstraintViolation<?> violation) {
        String code = violation.getMessageTemplate();

        boolean shouldGenerateDefaultErrorCode = code == null || code.trim().isEmpty() ||
            DEFAULT_ERROR_CODES_PREFIX.stream().anyMatch(code::startsWith);
        if (shouldGenerateDefaultErrorCode) {
            String prefix = violation.getPropertyPath().toString();
            Class<? extends Annotation> annotation = violation.getConstraintDescriptor().getAnnotation().annotationType();
            String suffix = ERROR_CODE_MAPPING.getOrDefault(annotation, annotation.getSimpleName());

            return prefix + "." + suffix;
        }

        return code.replace("{", "").replace("}", "");
    }

    private static Map<Class<? extends Annotation>, String> initErrorCodeMapping() {
        Map<Class<? extends Annotation>, String> codes = new HashMap<>();

        // Standard Constraints
        codes.put(AssertFalse.class, "shouldBeFalse");
        codes.put(AssertTrue.class, "shouldBeTrue");
        codes.put(DecimalMax.class, "exceedsMax");
        codes.put(DecimalMin.class, "lessThanMin");
        codes.put(Digits.class, "tooManyDigits");
        codes.put(Email.class, "invalidEmail");
        codes.put(Future.class, "shouldBeInFuture");
        codes.put(FutureOrPresent.class, "shouldBeInFutureOrPresent");
        codes.put(Max.class, "exceedsMax");
        codes.put(Min.class, "lessThanMin");
        codes.put(Negative.class, "shouldBeNegative");
        codes.put(NegativeOrZero.class, "shouldBeNegativeOrZero");
        codes.put(NotBlank.class, "shouldNotBeBlank");
        codes.put(NotEmpty.class, "shouldNotBeEmpty");
        codes.put(NotNull.class, "isRequired");
        codes.put(Null.class, "shouldBeMissing");
        codes.put(Past.class, "shouldBeInPast");
        codes.put(PastOrPresent.class, "shouldBeInPastOrPresent");
        codes.put(Pattern.class, "invalidPattern");
        codes.put(Positive.class, "shouldBePositive");
        codes.put(PositiveOrZero.class, "shouldBePositiveOrZero");
        codes.put(Size.class, "invalidSize");

        // Hibernate Validator Specific Constraints
        codes.put(URL.class, "invalidUrl");
        codes.put(UniqueElements.class, "shouldBeUnique");
        codes.put(Range.class, "outOfRange");
        codes.put(Length.class, "invalidSize");

        return codes;
    }
}
