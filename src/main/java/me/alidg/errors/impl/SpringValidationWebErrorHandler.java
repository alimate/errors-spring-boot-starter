package me.alidg.errors.impl;

import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
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
    @Override
    public HandledException handle(Throwable exception) {
        BindingResult bindingResult = getBindingResult(exception);
        return bindingResult.getAllErrors()
                .stream()
                .collect(collectingAndThen(
                        toMap(this::errorCode, this::arguments),
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
        return exception instanceof BindException ?
                ((BindException) exception).getBindingResult() :
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
        String message = error.getDefaultMessage() == null ? "" : error.getDefaultMessage();
        return message.replace("{", "").replace("}", "");
    }

    /**
     * Extracts the arguments from the validation meta data and exposes them to the outside
     * would! Apparently, all actual arguments are starting at index 1. So If there is less
     * than or equal to one argument, then we can assume that there is no argument to expose.
     *
     * @param error Encapsulates the error details.
     * @return Collection of all arguments for the given {@code error} details.
     */
    private List<Object> arguments(ObjectError error) {
        Object[] args = error.getArguments();

        return args == null || args.length <= 1 ?
                emptyList() : Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
    }

    /**
     * Drops the empty collection of arguments!
     *
     * @param input The error code to arguments map.
     * @return The filtered map.
     */
    private Map<String, List<Object>> dropEmptyValues(Map<String, List<Object>> input) {
        return input.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
