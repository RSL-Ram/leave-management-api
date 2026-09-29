package com.rsl.leavemanagement.service;

import com.rsl.leavemanagement.config.LeaveProperties;
import com.rsl.leavemanagement.dto.LeaveRequestDto;
import com.rsl.leavemanagement.dto.LeaveResponseDto;
import com.rsl.leavemanagement.exception.InvalidLeaveRequestException;
import com.rsl.leavemanagement.exception.LeaveNotFoundException;
import com.rsl.leavemanagement.model.LeaveRequest;
import com.rsl.leavemanagement.model.LeaveStatus;
import com.rsl.leavemanagement.model.LeaveType;
import com.rsl.leavemanagement.repository.LeaveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate END = LocalDate.of(2026, 10, 7);

    @Mock
    private LeaveRepository leaveRepository;

    private LeaveServiceImpl leaveService;

    @BeforeEach
    void setUp() {
        leaveService = new LeaveServiceImpl(leaveRepository, new LeaveProperties(10, false));
    }

    @Test
    @DisplayName("getAllLeaves maps every stored entity to a response DTO")
    void getAllLeaves_returnsAllLeaves() {
        when(leaveRepository.findAll()).thenReturn(List.of(
                leave(1L, LeaveStatus.PENDING),
                leave(2L, LeaveStatus.APPROVED)));

        List<LeaveResponseDto> result = leaveService.getAllLeaves();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LeaveResponseDto::id).containsExactly(1L, 2L);
        assertThat(result.get(1).status()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    @DisplayName("getAllLeaves returns an empty list when nothing is stored")
    void getAllLeaves_returnsEmptyList() {
        when(leaveRepository.findAll()).thenReturn(List.of());

        assertThat(leaveService.getAllLeaves()).isEmpty();
    }

    @Test
    @DisplayName("getLeaveById returns the matching leave")
    void getLeaveById_found() {
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave(1L, LeaveStatus.PENDING)));

        LeaveResponseDto result = leaveService.getLeaveById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.employeeId()).isEqualTo("EMP-001");
        assertThat(result.leaveType()).isEqualTo(LeaveType.CASUAL);
    }

    @Test
    @DisplayName("getLeaveById throws LeaveNotFoundException for an unknown id")
    void getLeaveById_notFound() {
        when(leaveRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.getLeaveById(99L))
                .isInstanceOf(LeaveNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("createLeave saves a new leave with PENDING status, ignoring any supplied status")
    void createLeave_setsPendingStatus() {
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });

        LeaveResponseDto result = leaveService.createLeave(request(START, END, LeaveStatus.APPROVED));

        ArgumentCaptor<LeaveRequest> captor = ArgumentCaptor.forClass(LeaveRequest.class);
        verify(leaveRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(result.reason()).isEqualTo("Personal work");
    }

    @Test
    @DisplayName("createLeave rejects an end date before the start date")
    void createLeave_endBeforeStart() {
        assertThatThrownBy(() -> leaveService.createLeave(request(END, START, null)))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("endDate");
        verify(leaveRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLeave accepts a single-day leave")
    void createLeave_singleDay() {
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveResponseDto result = leaveService.createLeave(request(START, START, null));

        assertThat(result.startDate()).isEqualTo(result.endDate());
    }

    @Test
    @DisplayName("createLeave rejects a duration longer than the configured maximum")
    void createLeave_exceedsMaxDays() {
        LeaveRequestDto tooLong = request(START, START.plusDays(10), null); // 11 days, max is 10

        assertThatThrownBy(() -> leaveService.createLeave(tooLong))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("exceeds the maximum of 10 days");
        verify(leaveRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateLeave overwrites fields and applies the supplied status")
    void updateLeave_updatesFields() {
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave(1L, LeaveStatus.PENDING)));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestDto update = new LeaveRequestDto("EMP-001", LeaveType.SICK,
                START.plusDays(1), END.plusDays(1), "Medical appointment", LeaveStatus.APPROVED);

        LeaveResponseDto result = leaveService.updateLeave(1L, update);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.leaveType()).isEqualTo(LeaveType.SICK);
        assertThat(result.startDate()).isEqualTo(START.plusDays(1));
        assertThat(result.reason()).isEqualTo("Medical appointment");
        assertThat(result.status()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    @DisplayName("updateLeave keeps the existing status when none is supplied")
    void updateLeave_keepsStatusWhenNull() {
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave(1L, LeaveStatus.APPROVED)));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveResponseDto result = leaveService.updateLeave(1L, request(START, END, null));

        assertThat(result.status()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    @DisplayName("updateLeave throws LeaveNotFoundException for an unknown id")
    void updateLeave_notFound() {
        when(leaveRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.updateLeave(99L, request(START, END, null)))
                .isInstanceOf(LeaveNotFoundException.class);
        verify(leaveRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateLeave rejects an invalid date range")
    void updateLeave_invalidDates() {
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave(1L, LeaveStatus.PENDING)));

        assertThatThrownBy(() -> leaveService.updateLeave(1L, request(END, START, null)))
                .isInstanceOf(InvalidLeaveRequestException.class);
        verify(leaveRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteLeave removes an existing leave")
    void deleteLeave_existing() {
        when(leaveRepository.existsById(1L)).thenReturn(true);

        leaveService.deleteLeave(1L);

        verify(leaveRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteLeave throws LeaveNotFoundException for an unknown id")
    void deleteLeave_notFound() {
        when(leaveRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> leaveService.deleteLeave(99L))
                .isInstanceOf(LeaveNotFoundException.class);
        verify(leaveRepository, never()).deleteById(any());
    }

    private static LeaveRequest leave(Long id, LeaveStatus status) {
        return new LeaveRequest(id, "EMP-001", LeaveType.CASUAL, START, END, "Personal work", status);
    }

    private static LeaveRequestDto request(LocalDate start, LocalDate end, LeaveStatus status) {
        return new LeaveRequestDto("EMP-001", LeaveType.CASUAL, start, end, "Personal work", status);
    }
}
