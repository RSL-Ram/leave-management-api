package com.rsl.leavemanagement.dto;

import com.rsl.leavemanagement.model.LeaveStatus;
import com.rsl.leavemanagement.model.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Incoming payload for creating or updating a leave request.
 * {@code status} is optional: new requests always start as PENDING,
 * and on update the status is only changed when supplied.
 */
public record LeaveRequestDto(

        @NotBlank(message = "employeeId is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{3,20}$",
                message = "employeeId must be 3-20 characters (letters, digits or hyphens)")
        String employeeId,

        @NotNull(message = "leaveType is required")
        LeaveType leaveType,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must not exceed 500 characters")
        String reason,

        LeaveStatus status
) {
}
