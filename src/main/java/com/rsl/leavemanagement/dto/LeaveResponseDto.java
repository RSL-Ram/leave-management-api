package com.rsl.leavemanagement.dto;

import com.rsl.leavemanagement.model.LeaveRequest;
import com.rsl.leavemanagement.model.LeaveStatus;
import com.rsl.leavemanagement.model.LeaveType;

import java.time.LocalDate;

/**
 * Outgoing representation of a leave request.
 */
public record LeaveResponseDto(
        Long id,
        String employeeId,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveStatus status
) {

    public static LeaveResponseDto from(LeaveRequest leave) {
        return new LeaveResponseDto(
                leave.getId(),
                leave.getEmployeeId(),
                leave.getLeaveType(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getReason(),
                leave.getStatus());
    }
}
