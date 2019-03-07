package me.alidg.errors.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.errors")
public class ErrorsProperties {
    public enum ArgumentExposure {never, non_empty, always}

    private ArgumentExposure exposeArguments = ArgumentExposure.never;

    public ArgumentExposure getExposeArguments() {
        return exposeArguments;
    }

    public void setExposeArguments(ArgumentExposure exposeArguments) {
        this.exposeArguments = exposeArguments;
    }
}
