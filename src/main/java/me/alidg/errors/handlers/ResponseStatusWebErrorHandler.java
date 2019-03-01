package me.alidg.errors.handlers;

import me.alidg.errors.HandledException;
import me.alidg.errors.WebErrorHandler;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static me.alidg.errors.handlers.LastResortWebErrorHandler.UNKNOWN_ERROR_CODE;
import static me.alidg.errors.handlers.MissingRequestParametersWebErrorHandler.*;
import static me.alidg.errors.handlers.ServletWebErrorHandler.METHOD_NOT_ALLOWED;
import static me.alidg.errors.handlers.ServletWebErrorHandler.NOT_ACCEPTABLE;
import static me.alidg.errors.handlers.ServletWebErrorHandler.*;
import static org.springframework.http.HttpStatus.*;

/**
 * {@link WebErrorHandler} implementation expert at handling exceptions of type
 * {@link ResponseStatusException}.
 *
 * @author Ali Dehghani
 */
public class ResponseStatusWebErrorHandler implements WebErrorHandler {

    /**
     * To delegate new validations exceptions like {@link WebExchangeBindException} to our old
     * binding result handler.
     */
    private final SpringValidationWebErrorHandler validationWebErrorHandler = new SpringValidationWebErrorHandler();

    /**
     * Only can handle exceptions of type {@link ResponseStatusException}.
     *
     * @param exception The exception to examine.
     * @return {@code true} for {@link ResponseStatusException}s, {@code false} for others.
     */
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ResponseStatusException;
    }

    /**
     * Handle each subtype of {@link ResponseStatusException} class in its own unique and appropriate way.
     *
     * @param exception The exception to handle.
     * @return A handled exception.
     */
    @NonNull
    @Override
    public HandledException handle(Throwable exception) {
        if (exception instanceof MediaTypeNotSupportedStatusException) {
            Map<String, List<?>> arguments =
                    arguments(NOT_SUPPORTED, ((MediaTypeNotSupportedStatusException) exception).getSupportedMediaTypes());
            return new HandledException(NOT_SUPPORTED, UNSUPPORTED_MEDIA_TYPE, arguments);
        }

        if (exception instanceof UnsupportedMediaTypeStatusException) {
            Map<String, List<?>> arguments =
                    arguments(NOT_SUPPORTED, ((UnsupportedMediaTypeStatusException) exception).getSupportedMediaTypes());
            return new HandledException(NOT_SUPPORTED, UNSUPPORTED_MEDIA_TYPE, arguments);
        }

        if (exception instanceof MethodNotAllowedException) {
            List<String> arguments = singletonList(((MethodNotAllowedException) exception).getHttpMethod());
            return new HandledException(METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED, arguments(METHOD_NOT_ALLOWED, arguments));
        }

        if (exception instanceof NotAcceptableStatusException) {
            List<List<MediaType>> arguments = singletonList(((NotAcceptableStatusException) exception).getSupportedMediaTypes());
            return new HandledException(NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE, arguments(NOT_ACCEPTABLE, arguments));
        }

        if (exception instanceof WebExchangeBindException) {
            return validationWebErrorHandler.handle(exception);
        }

        if (exception instanceof ServerWebInputException) {
            MethodParameter parameter = ((ServerWebInputException) exception).getMethodParameter();
            HandledException handledException = handleMissingParameters(parameter);
            if (handledException != null) return handledException;

            return new HandledException(INVALID_OR_MISSING_BODY, BAD_REQUEST, null);
        }

        if (exception instanceof ResponseStatusException) {
            HttpStatus status = ((ResponseStatusException) exception).getStatus();
            if (status == NOT_FOUND) return new HandledException(NO_HANDLER, status, null);

            return new HandledException(UNKNOWN_ERROR_CODE, status, null);
        }

        return new HandledException(UNKNOWN_ERROR_CODE, INTERNAL_SERVER_ERROR, null);
    }

    /**
     * Creates a map of arguments to exposed under the given {@code code} as a key.
     *
     * @param code The map key.
     * @param arguments The to-be-exposed arguments.
     * @return The intended map.
     */
    private Map<String, List<?>> arguments(String code, List<?> arguments) {
        return singletonMap(code, arguments);
    }

    /**
     * Spring WebFlux throw just one exception, i.e. {@link WebExchangeBindException} for
     * all request body binding failures, i.e. missing required parameter or missing matrix
     * variables. On the contrary, Traditional web stack throw one specific exception for
     * each scenario. In order to provide a consistent API for both stacks, we chose to
     * throw a bunch of if-else es to determines the actual cause and provide explicit feedback
     * to the client.
     *
     * @param parameter The invalid method parameter.
     * @return Possibly a handled exception.
     */
    private HandledException handleMissingParameters(MethodParameter parameter) {
        if (parameter == null) return null;

        String code = null;
        String parameterName = null;

        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        if (requestHeader != null) {
            code = MISSING_HEADER;
            parameterName = extractParameterName(requestHeader, parameter);
        }

        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            code = MISSING_PARAMETER;
            parameterName = extractParameterName(requestParam, parameter);
        }

        RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
        if (requestPart != null) {
            code = MISSING_PART;
            parameterName = extractParameterName(requestPart, parameter);
        }

        CookieValue cookieValue = parameter.getParameterAnnotation(CookieValue.class);
        if (cookieValue != null) {
            code = MISSING_COOKIE;
            parameterName = extractParameterName(cookieValue, parameter);
        }

        MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
        if (matrixVariable != null) {
            code = MISSING_MATRIX_VARIABLE;
            parameterName = extractParameterName(matrixVariable, parameter);
        }

        if (code != null) {
            List<String> arguments = asList(parameterName, parameter.getParameterType().getSimpleName());
            return new HandledException(code, BAD_REQUEST, singletonMap(code, arguments));
        }

        return null;
    }

    private String extractParameterName(Annotation annotation, MethodParameter parameter) {
        String name = getNameAttribute(annotation);

        return name.isEmpty() ? parameter.getParameterName() : name;
    }

    private String getNameAttribute(Annotation annotation) {
        try {
            Method method = annotation.getClass().getMethod("name");
            return (String) method.invoke(method);
        } catch (Exception e) {
            return "";
        }
    }
}
