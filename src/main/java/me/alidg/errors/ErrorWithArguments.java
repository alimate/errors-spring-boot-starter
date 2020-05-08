package me.alidg.errors;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * This object represents an error code with to-be-exposed arguments.
 * <p>
 * For example, suppose
 * we have a bean like:
 * <pre>
 *     public class User {
 *
 *         &#64;Size(min=1, max=7, message="interests.range_limit")
 *         private List&lt;String&gt; interests;
 *         // omitted for the sake of brevity
 *     }
 * </pre>
 * If the given interest list wasn't valid, then this object would contain
 * {@code interests.range_limit} as the <code>errorCode</code> and {@code List(Argument(min, 1), Argument(max, 7))}
 * as the <code>arguments</code>. Later on we can use those exposed values in our message, for example,
 * the following error template:
 * <pre>
 *     You should define between {0} and {1} interests.
 * </pre>
 * Would be translated to:
 * <pre>
 *     You should define between 1 and 7 interests.
 * </pre>
 */
public class ErrorWithArguments {
    private final String errorCode;
    private final List<Argument> arguments;

    /**
     * Constructor
     *
     * @param errorCode the error code
     * @param arguments the arguments to use when interpolating the message of the error code
     */
    public ErrorWithArguments(String errorCode, List<Argument> arguments) {
        this.errorCode = errorCode;
        this.arguments = arguments == null ? Collections.emptyList() : arguments;
    }

    /**
     * Factory method when an error code has no arguments.
     *
     * @param errorCode the error code
     * @return a new {@link ErrorWithArguments} instance
     */
    public static ErrorWithArguments noArgumentError(String errorCode) {
        requireNonNull(errorCode, "The single error code can't be null");
        return new ErrorWithArguments(errorCode, emptyList());
    }

    /**
     * The error code that will be looked up in the messages.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * The arguments to use when interpolating the message of the error code.
     *
     * @return a List of {@link Argument} objects
     */
    public List<Argument> getArguments() {
        return arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ErrorWithArguments that = (ErrorWithArguments) o;
        return errorCode.equals(that.errorCode) &&
            arguments.equals(that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, arguments);
    }


}
