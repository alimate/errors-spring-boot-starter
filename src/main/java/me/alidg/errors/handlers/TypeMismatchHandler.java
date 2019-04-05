package me.alidg.errors.handlers;

import me.alidg.errors.Argument;
import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.beans.TypeMismatchException;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonMap;
import static me.alidg.errors.Argument.arg;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * A {@link WebErrorHandler} implementation responsible for handling {@link TypeMismatchException}s.
 * it raised while resolving a controller method argument.
 *
 * @author Mona Mohamadinia
 */
public class TypeMismatchHandler implements WebErrorHandler {

    /**
     * Basic error code for all type mismatches.
     */
    public static final String TYPE_MISMATCH = "binding.type_mismatch";

    /**
     * Only can handle exceptions of type {@link TypeMismatchException}.
     *
     * @param exception The exception to examine.
     * @return {@code true} if the {@code exception} is {@link TypeMismatchException}, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof TypeMismatchException;
    }

    @NonNull
    @Override
    public HandledException handle(Throwable exception) {

        TypeMismatchException mismatchException = (TypeMismatchException) exception;
        List<Argument> arguments = getArguments(mismatchException);

        String errorCode = getErrorCode(mismatchException);
        return new HandledException(errorCode, BAD_REQUEST, singletonMap(errorCode, arguments));
    }

    static List<Argument> getArguments(TypeMismatchException mismatchException) {
        List<Argument> arguments = new ArrayList<>();
        arguments.add(arg("property", mismatchException.getPropertyName()));
        arguments.add(arg("invalid", mismatchException.getValue()));
        if (mismatchException.getRequiredType() != null) {
            arguments.add(arg("expected", mismatchException.getRequiredType().getSimpleName()));
        }
        return arguments;
    }

    static String getErrorCode(TypeMismatchException mismatchException) {
        return TYPE_MISMATCH + "." + mismatchException.getPropertyName();
    }
}
