package me.alidg.errors.handlers;

import me.alidg.errors.ErrorWithArguments;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.*;

/**
 * A {@link WebErrorHandler} implementation responsible for handling {@link ConstraintViolationException}s
 * from bean validation.
 *
 * @author Ali Dehghani
 */
public class ConstraintViolationWebErrorHandler implements WebErrorHandler {

    /**
     * Only can handle {@link ConstraintViolationException}s that contains at least one
     * {@link ConstraintViolation}.
     *
     * @param exception The exception to examine.
     * @return {@code true} if the {@code exception} is a {@link ConstraintViolationException} with at least one
     * violation, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ConstraintViolationException && hasAtLeastOneViolation(exception);
    }

    /**
     * Handles the given {@link ConstraintViolationException}s by extracting the error codes from the message templates
     * and also, extracting the arguments from {@link javax.validation.metadata.ConstraintDescriptor}s.
     *
     * <h3>Constraint Descriptor</h3>
     * All annotation attributes defined in a constrain annotation, e.g. {@link javax.validation.constraints.Size},
     * can be extracted from the {@link javax.validation.metadata.ConstraintDescriptor#getAttributes()} method. The
     * specification of the Bean Validation API demands, that any constraint annotation must define three mandatory
     * attributes, {@code message}, {@code groups} and {@code payload}. Since these three attributes are not that valuable
     * as arguments, we're not going to expose them.
     *
     * @param exception The exception to handle.
     * @return The handled exception
     */
    @NonNull
    @Override
    public HandledException handle(Throwable exception) {
        ConstraintViolationException violationException = (ConstraintViolationException) exception;

        return new HandledException(extractErrorWithArguments(violationException), HttpStatus.BAD_REQUEST);
    }

    /**
     * checks if the given {@link ConstraintViolationException} contains at least one constraint violation.
     *
     * @param exception The exception to examine.
     * @return {@code true} if the exception contains at least one violation, {@code false} otherwise.
     */
    private boolean hasAtLeastOneViolation(Throwable exception) {
        Set<ConstraintViolation<?>> violations = ((ConstraintViolationException) exception).getConstraintViolations();

        return violations != null && !violations.isEmpty();
    }

    private List<ErrorWithArguments> extractErrorWithArguments(ConstraintViolationException exception) {
        return exception
            .getConstraintViolations()
            .stream()
            .map(constraintViolation -> new ErrorWithArguments(ConstraintViolations.getErrorCode(constraintViolation),
                                                               ConstraintViolations.getArguments(constraintViolation)))
            .filter(errorWithArguments -> !errorWithArguments.getArguments().isEmpty())
            .collect(toList());
    }
}
