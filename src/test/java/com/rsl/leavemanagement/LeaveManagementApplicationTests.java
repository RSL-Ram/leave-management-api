package com.rsl.leavemanagement;

import com.rsl.leavemanagement.config.LeaveProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class LeaveManagementApplicationTests {

    @Autowired
    private LeaveProperties leaveProperties;

    @Test
    void contextLoads_withDevProfileConfiguration() {
        assertThat(leaveProperties.maxDaysPerRequest()).isEqualTo(45);
        assertThat(leaveProperties.seedSampleData()).isTrue();
    }
}
