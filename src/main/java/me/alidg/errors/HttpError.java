package me.alidg.errors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents the error details that should be serialized inside a HTTP
 * response body.
 *
 * @author Ali Dehghani
 */
public class HttpError {

    /**
     * Collection of error codes alongside with their corresponding messages.
     */
    private final List<CodedMessage> errors;

    /**
     * The expected status code for the HTTP response.
     */
    private final HttpStatus httpStatus;

    /**
     * Encapsulates the current and probably failed HTTP request. It's either a
     * {@link org.springframework.web.context.request.WebRequest} for Servlet requests
     * or {@link org.springframework.web.reactive.function.server.ServerRequest} for reactive
     * stack requests.
     */
    @Nullable private Object request;

    /**
     * Encapsulates the original exception raised while processing the HTTP request.
     */
    @Nullable private Throwable originalException;

    /**
     * Encapsulates the possible refined exception, if any.
     */
    @Nullable private Throwable refinedException;

    /**
     * Constructing a HTTP error instance.
     *
     * @param errors Collection of codes/messages combinations.
     * @param httpStatus The expected status code.
     */
    public HttpError(List<CodedMessage> errors, HttpStatus httpStatus) {
        this.errors = errors;
        this.httpStatus = httpStatus;
    }

    /**
     * @return Collection of error codes/messages combinations.
     * @see #errors
     */
    public List<CodedMessage> getErrors() {
        return errors;
    }

    /**
     * Returns the current and probably failed HTTP request. It's either a
     * {@link org.springframework.web.context.request.WebRequest} for Servlet requests
     * or {@link org.springframework.web.reactive.function.server.ServerRequest} for reactive
     * stack requests.
     *
     * @return The current HTTP request.
     */
    public Object getRequest() {
        return request;
    }

    /**
     * @param request {@link #request}.
     */
    public void setRequest(Object request) {
        this.request = request;
    }

    /**
     * @return The original exception raised while processing the HTTP request.
     */
    public Throwable getOriginalException() {
        return originalException;
    }

    /**
     * @param originalException {@link #originalException}.
     */
    public void setOriginalException(Throwable originalException) {
        this.originalException = originalException;
    }

    /**
     * @return The possible refined exception, if any.
     */
    public Throwable getRefinedException() {
        return refinedException;
    }

    /**
     * @param refinedException {@link #refinedException}.
     */
    public void setRefinedException(Throwable refinedException) {
        this.refinedException = refinedException;
    }

    /**
     * @return The expected status code.
     * @see #httpStatus
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String toString() {
        return "HttpError{" +
                "errors=" + errors +
                ", httpStatus=" + httpStatus +
                '}';
    }

    /**
     * Represents an error code paired with its appropriate error message.
     */
    public static class CodedMessage {

        /**
         * The error code.
         */
        private final String code;

        /**
         * The error message.
         */
        private final String message;

        /**
         * @param code The error code.
         * @param message The error message.
         */
        public CodedMessage(String code, String message) {
            this.code = code;
            this.message = message;
        }

        /**
         * @return The error code.
         * @see #code
         */
        public String getCode() {
            return code;
        }

        /**
         * @return The error message.
         * @see #message
         */
        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof CodedMessage)) return false;

            CodedMessage that = (CodedMessage) o;
            return Objects.equals(getCode(), that.getCode()) &&
                    Objects.equals(getMessage(), that.getMessage());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getCode(), getMessage());
        }

        @Override
        public String toString() {
            return "CodedMessage{" +
                    "code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
