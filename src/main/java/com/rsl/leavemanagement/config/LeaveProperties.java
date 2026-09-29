package com.rsl.leavemanagement.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings bound from the {@code leave.*} keys in application.yml.
 *
 * @param maxDaysPerRequest maximum number of calendar days a single request may span
 * @param seedSampleData    whether to load sample leave requests at startup
 */
@Validated
@ConfigurationProperties(prefix = "leave")
public record LeaveProperties(
        @Min(1) int maxDaysPerRequest,
        boolean seedSampleData
) {
}
