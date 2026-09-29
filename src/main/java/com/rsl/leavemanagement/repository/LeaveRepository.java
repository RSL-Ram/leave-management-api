package com.rsl.leavemanagement.repository;

import com.rsl.leavemanagement.model.LeaveRequest;

import java.util.List;
import java.util.Optional;

public interface LeaveRepository {

    List<LeaveRequest> findAll();

    Optional<LeaveRequest> findById(Long id);

    LeaveRequest save(LeaveRequest leaveRequest);

    boolean existsById(Long id);

    void deleteById(Long id);
}
