package com.rsl.leavemanagement.repository;

import com.rsl.leavemanagement.model.LeaveRequest;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe in-memory store backed by a {@link ConcurrentHashMap}.
 */
@Repository
public class InMemoryLeaveRepository implements LeaveRepository {

    private final Map<Long, LeaveRequest> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    @Override
    public List<LeaveRequest> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(LeaveRequest::getId))
                .toList();
    }

    @Override
    public Optional<LeaveRequest> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public LeaveRequest save(LeaveRequest leaveRequest) {
        if (leaveRequest.getId() == null) {
            leaveRequest.setId(idSequence.incrementAndGet());
        }
        store.put(leaveRequest.getId(), leaveRequest);
        return leaveRequest;
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
