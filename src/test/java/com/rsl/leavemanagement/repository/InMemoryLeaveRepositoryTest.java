package com.rsl.leavemanagement.repository;

import com.rsl.leavemanagement.model.LeaveRequest;
import com.rsl.leavemanagement.model.LeaveStatus;
import com.rsl.leavemanagement.model.LeaveType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLeaveRepositoryTest {

    private final InMemoryLeaveRepository repository = new InMemoryLeaveRepository();

    @Test
    void save_assignsSequentialIds() {
        LeaveRequest first = repository.save(newLeave());
        LeaveRequest second = repository.save(newLeave());

        assertThat(first.getId()).isEqualTo(1L);
        assertThat(second.getId()).isEqualTo(2L);
        assertThat(repository.findAll()).extracting(LeaveRequest::getId).containsExactly(1L, 2L);
    }

    @Test
    void save_withExistingId_replacesEntry() {
        LeaveRequest saved = repository.save(newLeave());
        saved.setReason("Updated");

        repository.save(saved);

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(saved.getId())).get()
                .extracting(LeaveRequest::getReason).isEqualTo("Updated");
    }

    @Test
    void deleteById_removesEntry() {
        LeaveRequest saved = repository.save(newLeave());

        repository.deleteById(saved.getId());

        assertThat(repository.existsById(saved.getId())).isFalse();
        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    private static LeaveRequest newLeave() {
        return new LeaveRequest(null, "EMP-001", LeaveType.CASUAL,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6), "Personal work", LeaveStatus.PENDING);
    }
}
