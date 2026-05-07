package com.hospital.queue.service;

import com.hospital.queue.domain.entity.Doctor;
import com.hospital.queue.domain.enums.DoctorStatus;
import com.hospital.queue.exception.ResourceNotFoundException;
import com.hospital.queue.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository      doctorRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditLogService       auditLogService;

    @Cacheable("doctors")
    @Transactional(readOnly = true)
    public List<Doctor> findAll() {
        return doctorRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Doctor findById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    @Transactional(readOnly = true)
    public Doctor findByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile", "userId", userId));
    }

    @Transactional(readOnly = true)
    public List<Doctor> findByDepartment(Long deptId) {
        return doctorRepository.findByDepartmentId(deptId);
    }

    @CacheEvict(value = "doctors", allEntries = true)
    @Transactional
    public Doctor updateStatus(Long doctorId, DoctorStatus newStatus, String username, Long userId) {
        Doctor doctor = findById(doctorId);
        DoctorStatus oldStatus = doctor.getStatus();
        doctor.setStatus(newStatus);
        doctor = doctorRepository.save(doctor);

        // Broadcast status change via WebSocket
        messagingTemplate.convertAndSend("/topic/doctor-status", Map.of(
                "doctorId", doctorId,
                "doctorName", doctor.getUser().getFullName(),
                "oldStatus", oldStatus.name(),
                "newStatus", newStatus.name(),
                "statusLabel", newStatus.getLabel()
        ));

        auditLogService.log(username, userId, "UPDATE_DOCTOR_STATUS",
                "Doctor", String.valueOf(doctorId), oldStatus.name(), newStatus.name());

        return doctor;
    }

    public long countAvailable() {
        return doctorRepository.findByStatus(DoctorStatus.AVAILABLE).size();
    }

    public long countAll() {
        return doctorRepository.count();
    }
}
