package com.kleadingsolutions.expenseshare.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Logging aspect for performance monitoring at service layer
 * @param enabled
 * @param warnThresholdMs
 * @param includeArgs
 * @param includeResult
 * @param maxArgStringLength
 * @param maxResultStringLength
 */
@Slf4j
@ConfigurationProperties(prefix = "app.logging.aspect")
public record LoggingAspectProperties(
        boolean enabled,
        long warnThresholdMs,
        boolean includeArgs,
        boolean includeResult,
        int maxArgStringLength,
        int maxResultStringLength
) {

    public static LoggingAspectProperties defaults() {
        return new LoggingAspectProperties(
                true,
                500L,
                false,
                false,
                200,
                300
        );
    }

    public LoggingAspectProperties {
        if (warnThresholdMs <= 0) {
            log.error("warnThresholdMs must be greater than 0");
        }
        if (maxArgStringLength <= 0) {
            log.error("maxArgStringLength must be greater than 0");
        }
        if (maxResultStringLength <= 0) {
            log.error("maxResultStringLength must be greater than 0");
        }
    }
}