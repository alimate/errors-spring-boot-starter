package me.alidg.errors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * Represents the error details that should be serialized inside a HTTP
 * response body.
 *
 * @author Ali Dehghani
 */
@JsonAutoDetect(fieldVisibility = ANY)
public class HttpError {

    /**
     * Collection of error codes alongside with their corresponding messages.
     */
    private List<CodedMessage> errors;

    /**
     * The expected status code for the HTTP response.
     */
    private HttpStatus httpStatus;

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
     * @return The expected status code.
     * @see #httpStatus
     */
    @JsonIgnore
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Represents an error code paired with its appropriate error message.
     */
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class CodedMessage {

        /**
         * The error code.
         */
        private String code;

        /**
         * The error message.
         */
        private String message;

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
    }
}
