package com.rsl.leavemanagement.service;

import com.rsl.leavemanagement.dto.LeaveRequestDto;
import com.rsl.leavemanagement.dto.LeaveResponseDto;

import java.util.List;

public interface LeaveService {

    List<LeaveResponseDto> getAllLeaves();

    LeaveResponseDto getLeaveById(Long id);

    LeaveResponseDto createLeave(LeaveRequestDto request);

    LeaveResponseDto updateLeave(Long id, LeaveRequestDto request);

    void deleteLeave(Long id);
}
