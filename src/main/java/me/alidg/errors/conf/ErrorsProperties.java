package me.alidg.errors.conf;

import me.alidg.errors.Argument;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties to configure the error handling mechanism.
 *
 * @author zarebski-m
 */
@ConfigurationProperties("errors")
public class ErrorsProperties {

    /**
     * Determines how we're gonna expose the arguments for each error code.
     */
    @NonNull
    private ArgumentExposure exposeArguments = ArgumentExposure.NEVER;

    /**
     * Determines whether we should add the error fingerprint to the error representation.
     */
    private boolean addFingerprint = false;

    /**
     * @return {@code exposeArguments}
     * @see #exposeArguments
     */
    public ArgumentExposure getExposeArguments() {
        return exposeArguments;
    }

    /**
     * @param exposeArguments {@code exposeArguments}
     * @see #exposeArguments
     */
    public void setExposeArguments(ArgumentExposure exposeArguments) {
        this.exposeArguments = exposeArguments;
    }

    /**
     * @return {@code addFingerprint}
     * @see #addFingerprint
     */
    public boolean isAddFingerprint() {
        return addFingerprint;
    }

    /**
     * @param addFingerprint {@code addFingerprint}
     * @see #isAddFingerprint()
     */
    public void setAddFingerprint(boolean addFingerprint) {
        this.addFingerprint = addFingerprint;
    }

    /**
     * Determines how we're gonna expose the arguments parameter for each error code.
     */
    public enum ArgumentExposure {

        /**
         * Never expose error arguments.
         */
        NEVER,

        /**
         * Expose error arguments only when there is anything to expose.
         */
        NON_EMPTY {
            @Override
            public void expose(Map<String, Object> error, List<Argument> arguments) {
                if (!arguments.isEmpty()) {
                    error.put("arguments", argumentsMap(arguments));
                }
            }
        },

        /**
         * Always expose {@code "arguments"} element, even when there are no error arguments.
         */
        ALWAYS {
            @Override
            public void expose(Map<String, Object> error, List<Argument> arguments) {
                error.put("arguments", argumentsMap(arguments));
            }
        };

        private static Map<String, Object> argumentsMap(List<Argument> arguments) {
            if (arguments == null) return Collections.emptyMap();

            Map<String, Object> argumentMap = new HashMap<>();
            for (Argument argument : arguments) {
                argumentMap.put(argument.getName(), argument.getValue());
            }

            return argumentMap;
        }

        /**
         * Exposes the given {@code arguments} into the given {@code error} representation
         *
         * @param error     The current error representation.
         * @param arguments Collection of to be exposed arguments.
         */
        public void expose(Map<String, Object> error, List<Argument> arguments) {
        }
    }
}
