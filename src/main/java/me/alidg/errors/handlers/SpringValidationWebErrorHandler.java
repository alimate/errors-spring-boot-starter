package me.alidg.errors.handlers;

import me.alidg.errors.Argument;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

/**
 * A {@link WebErrorHandler} responsible for handling validation errors thrown by
 * the web layer. Currently, the following exceptions are supported:
 * <ul>
 * <li>{@link BindException}</li>
 * <li>{@link MethodArgumentNotValidException}</li>
 * </ul>
 *
 * @author Ali Dehghani
 */
public class SpringValidationWebErrorHandler implements WebErrorHandler {

    /**
     * Basic error code for unknown binding errors.
     */
    public static final String BINDING_FAILURE = "binding.failure";

    /**
     * Can only handle supported exceptions mentioned above.
     *
     * @param exception The exception to examine.
     * @return {@code true} if it can handle the exception, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof MethodArgumentNotValidException || exception instanceof BindException;
    }

    /**
     * After extracting the {@link BindingResult} from the {@code exception}, would iterate over all errors and
     * pack all validation errors with their corresponding to be exposed arguments.
     *
     * @param exception The exception to handle.
     * @return A {@link HandledException} instance containing the required details about the validation errors.
     */
    @NonNull
    @Override
    public HandledException handle(Throwable exception) {
        BindingResult bindingResult = getBindingResult(exception);
        return bindingResult.getAllErrors()
            .stream()
            .collect(collectingAndThen(
                toMap(this::errorCode, this::arguments, (value1, value2) -> value1),
                m -> new HandledException(m.keySet(), HttpStatus.BAD_REQUEST, dropEmptyValues(m))
            ));
    }

    /**
     * Extract the {@link BindingResult} from the supported exceptions.
     *
     * @param exception The exception to inspect.
     * @return The extracted {@link BindingResult}.
     */
    private BindingResult getBindingResult(Throwable exception) {
        return exception instanceof BindingResult ?
            ((BindingResult) exception) :
            ((MethodArgumentNotValidException) exception).getBindingResult();
    }

    /**
     * Extracting the error code from the given {@link ObjectError}. Also, before returning
     * the error code, would strip the curly braces form the error code, if any (We have our own
     * message interpolation!).
     *
     * @param error Encapsulates the error details.
     * @return The error code.
     */
    private String errorCode(ObjectError error) {
        String code = null;
        try {
            code = error.unwrap(ConstraintViolation.class).getMessageTemplate();
        } catch (Exception ignored) {
        }

        if (code == null) {
            try {
                code = TypeMismatchWebErrorHandler.getErrorCode(error.unwrap(TypeMismatchException.class));
            } catch (Exception ignored) {
            }
        }

        if (code == null) code = BINDING_FAILURE;
        return code.replace("{", "").replace("}", "");
    }

    /**
     * Extracts the arguments from the validation meta data and exposes them to the outside
     * world. First try to unwrap {@link ConstraintViolation} and if successful, use
     * {@link ConstraintViolationArgumentsExtractor#extract(ConstraintViolation)}.
     *
     * @param error Encapsulates the error details.
     * @return Collection of all arguments for the given {@code error} details.
     */
    private List<Argument> arguments(ObjectError error) {
        try {
            ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
            return ConstraintViolationArgumentsExtractor.extract(violation);
        } catch (Exception ignored) {
        }

        try {
            return TypeMismatchWebErrorHandler.getArguments(error.unwrap(TypeMismatchException.class));
        } catch (Exception ignored) {
        }

        return emptyList();
    }

    /**
     * Drops the empty collection of arguments!
     *
     * @param input The error code to arguments map.
     * @return The filtered map.
     */
    private Map<String, List<Argument>> dropEmptyValues(Map<String, List<Argument>> input) {
        return input.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
    }
}
