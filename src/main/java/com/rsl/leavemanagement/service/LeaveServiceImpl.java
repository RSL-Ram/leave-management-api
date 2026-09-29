package com.rsl.leavemanagement.service;

import com.rsl.leavemanagement.config.LeaveProperties;
import com.rsl.leavemanagement.dto.LeaveRequestDto;
import com.rsl.leavemanagement.dto.LeaveResponseDto;
import com.rsl.leavemanagement.exception.InvalidLeaveRequestException;
import com.rsl.leavemanagement.exception.LeaveNotFoundException;
import com.rsl.leavemanagement.model.LeaveRequest;
import com.rsl.leavemanagement.model.LeaveStatus;
import com.rsl.leavemanagement.repository.LeaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveServiceImpl.class);

    private final LeaveRepository leaveRepository;
    private final LeaveProperties leaveProperties;

    public LeaveServiceImpl(LeaveRepository leaveRepository, LeaveProperties leaveProperties) {
        this.leaveRepository = leaveRepository;
        this.leaveProperties = leaveProperties;
    }

    @Override
    public List<LeaveResponseDto> getAllLeaves() {
        return leaveRepository.findAll().stream()
                .map(LeaveResponseDto::from)
                .toList();
    }

    @Override
    public LeaveResponseDto getLeaveById(Long id) {
        return LeaveResponseDto.from(findExisting(id));
    }

    @Override
    public LeaveResponseDto createLeave(LeaveRequestDto request) {
        validateBusinessRules(request);
        LeaveRequest leave = new LeaveRequest(
                null,
                request.employeeId(),
                request.leaveType(),
                request.startDate(),
                request.endDate(),
                request.reason(),
                LeaveStatus.PENDING);
        LeaveRequest saved = leaveRepository.save(leave);
        log.debug("Created leave request {} for employee {}", saved.getId(), saved.getEmployeeId());
        return LeaveResponseDto.from(saved);
    }

    @Override
    public LeaveResponseDto updateLeave(Long id, LeaveRequestDto request) {
        LeaveRequest existing = findExisting(id);
        validateBusinessRules(request);

        existing.setEmployeeId(request.employeeId());
        existing.setLeaveType(request.leaveType());
        existing.setStartDate(request.startDate());
        existing.setEndDate(request.endDate());
        existing.setReason(request.reason());
        if (request.status() != null) {
            existing.setStatus(request.status());
        }

        LeaveRequest saved = leaveRepository.save(existing);
        log.debug("Updated leave request {}", id);
        return LeaveResponseDto.from(saved);
    }

    @Override
    public void deleteLeave(Long id) {
        if (!leaveRepository.existsById(id)) {
            throw new LeaveNotFoundException(id);
        }
        leaveRepository.deleteById(id);
        log.debug("Deleted leave request {}", id);
    }

    private LeaveRequest findExisting(Long id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> new LeaveNotFoundException(id));
    }

    private void validateBusinessRules(LeaveRequestDto request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidLeaveRequestException("endDate must be on or after startDate");
        }
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (days > leaveProperties.maxDaysPerRequest()) {
            throw new InvalidLeaveRequestException(
                    "Leave duration of " + days + " days exceeds the maximum of "
                            + leaveProperties.maxDaysPerRequest() + " days per request");
        }
    }
}
