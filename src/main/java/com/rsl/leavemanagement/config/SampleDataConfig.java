package com.rsl.leavemanagement.config;

import com.rsl.leavemanagement.model.LeaveRequest;
import com.rsl.leavemanagement.model.LeaveStatus;
import com.rsl.leavemanagement.model.LeaveType;
import com.rsl.leavemanagement.repository.LeaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

/**
 * Seeds a few leave requests when running with the {@code dev} profile
 * so the API can be explored immediately after startup.
 */
@Configuration
@Profile("dev")
public class SampleDataConfig {

    private static final Logger log = LoggerFactory.getLogger(SampleDataConfig.class);

    @Bean
    @ConditionalOnProperty(prefix = "leave", name = "seed-sample-data", havingValue = "true")
    CommandLineRunner seedLeaveRequests(LeaveRepository repository) {
        return args -> {
            LocalDate today = LocalDate.now();
            repository.save(new LeaveRequest(null, "EMP-001", LeaveType.CASUAL,
                    today.plusDays(3), today.plusDays(4), "Family function", LeaveStatus.PENDING));
            repository.save(new LeaveRequest(null, "EMP-002", LeaveType.SICK,
                    today.minusDays(2), today.minusDays(1), "Fever", LeaveStatus.APPROVED));
            repository.save(new LeaveRequest(null, "EMP-003", LeaveType.EARNED,
                    today.plusDays(10), today.plusDays(17), "Vacation", LeaveStatus.PENDING));
            log.info("Dev profile: seeded {} sample leave requests", repository.findAll().size());
        };
    }
}
