package me.alidg.errors.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.errors")
public class ErrorsProperties {
    public enum ArgumentExposure {never, non_empty, always}

    private ArgumentExposure exposeArguments = ArgumentExposure.never;

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
