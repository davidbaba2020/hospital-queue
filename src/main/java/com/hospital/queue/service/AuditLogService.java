package com.hospital.queue.service;

import com.hospital.queue.domain.entity.AuditLog;
import com.hospital.queue.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String username, Long userId, String action,
                    String entityType, String entityId,
                    String oldValue, String newValue) {
        AuditLog entry = AuditLog.builder()
                .username(username)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .status("SUCCESS")
                .performedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(entry);
    }

    @Async
    public void logFailure(String username, String action, String errorMessage) {
        AuditLog entry = AuditLog.builder()
                .username(username != null ? username : "anonymous")
                .action(action)
                .status("FAILURE")
                .errorMessage(errorMessage)
                .performedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(entry);
    }

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);
    }

    public Page<AuditLog> findByUsername(String username, Pageable pageable) {
        return auditLogRepository
                .findByUsernameContainingIgnoreCaseOrderByPerformedAtDesc(username, pageable);
    }

    public long countToday() {
        return auditLogRepository.countByPerformedAtAfter(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
    }
}
