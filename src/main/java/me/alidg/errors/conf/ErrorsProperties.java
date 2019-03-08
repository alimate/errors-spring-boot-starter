package me.alidg.errors.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * General configuration properties for this library.
 */
@ConfigurationProperties("spring.errors")
public class ErrorsProperties {
    public enum ArgumentExposure {
        /**
         * Never expose error arguments.
         */
        never,

        /**
         * Expose error arguments only when there is anything to expose.
         */
        non_empty,

        /**
         * Always expose {@code "arguments"} element, even when there are no error arguments.
         */
        always
    }

    /**
     * Type of named arguments exposure.
     */
    private ArgumentExposure exposeArguments = ArgumentExposure.never;

    /**
     * Tells whether error fingerprint should be added to {@link me.alidg.errors.HttpError} instance.
     */
    private boolean addFingerprint = false;

    public ArgumentExposure getExposeArguments() {
        return exposeArguments;
    }

    public void setExposeArguments(ArgumentExposure exposeArguments) {
        this.exposeArguments = exposeArguments;
    }

    public boolean isAddFingerprint() {
        return addFingerprint;
    }

    public void setAddFingerprint(boolean addFingerprint) {
        this.addFingerprint = addFingerprint;
    }
}
