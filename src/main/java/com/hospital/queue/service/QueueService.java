package com.hospital.queue.service;

import com.hospital.queue.domain.entity.QueueEntry;
import com.hospital.queue.domain.entity.User;
import com.hospital.queue.domain.enums.DoctorStatus;
import com.hospital.queue.domain.enums.QueueStatus;
import com.hospital.queue.dto.request.QueueEntryRequest;
import com.hospital.queue.dto.response.QueueEntryResponse;
import com.hospital.queue.exception.InvalidOperationException;
import com.hospital.queue.exception.QueueFullException;
import com.hospital.queue.exception.ResourceNotFoundException;
import com.hospital.queue.repository.*;
import com.hospital.queue.util.QueueNumberGenerator;
import com.hospital.queue.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueEntryRepository  queueEntryRepository;
    private final PatientRepository     patientRepository;
    private final DoctorRepository      doctorRepository;
    private final DepartmentRepository  departmentRepository;
    private final UserRepository        userRepository;
    private final QueueNumberGenerator  generator;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService          emailService;
    private final AuditLogService       auditLogService;
    private final SecurityUtils         securityUtils;

    // ─── Register a patient into the queue ────────────────────────────────────

    @Transactional
    public QueueEntry enqueue(QueueEntryRequest req) {
        var department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", req.getDepartmentId()));

        // Check queue capacity
        long activeCount = queueEntryRepository.countByDepartmentIdAndStatus(
                req.getDepartmentId(), QueueStatus.WAITING);
        if (activeCount >= department.getMaxQueue()) {
            throw new QueueFullException(department.getName());
        }

        var patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", req.getPatientId()));

        var currentUser = securityUtils.requireCurrentUser();
        var registeredBy = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        QueueEntry entry = QueueEntry.builder()
                .ticketNumber(generator.nextTicket(department.getCode()))
                .patient(patient)
                .department(department)
                .priority(req.getPriority())
                .status(QueueStatus.WAITING)
                .chiefComplaint(req.getChiefComplaint())
                .notes(req.getNotes())
                .vitals(req.getVitals())
                .registeredBy(registeredBy)
                .registeredAt(LocalDateTime.now())
                .build();

        if (req.getDoctorId() != null) {
            doctorRepository.findById(req.getDoctorId())
                    .ifPresent(entry::setDoctor);
        }

        // Estimate wait time
        int position = (int) activeCount + 1;
        entry.setEstimatedWaitMin(position * 15);

        entry = queueEntryRepository.save(entry);

        // Notify patient via email
        if (patient.getEmail() != null) {
            emailService.sendQueueConfirmation(patient, entry, position);
        }

        // Broadcast to all connected clients
        broadcastQueueUpdate(req.getDepartmentId(), "PATIENT_ADDED");
        auditLogService.log(currentUser.getUsername(), currentUser.getId(),
                "ENQUEUE_PATIENT", "QueueEntry", entry.getTicketNumber(),
                null, patient.getFullName());

        log.info("Patient {} added to queue [{}] ticket={}", patient.getFullName(),
                department.getCode(), entry.getTicketNumber());
        return entry;
    }

    // ─── Update queue entry status ─────────────────────────────────────────────

    @Transactional
    public QueueEntry updateStatus(Long entryId, QueueStatus newStatus) {
        QueueEntry entry = queueEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("QueueEntry", "id", entryId));

        if (entry.getStatus().isTerminal()) {
            throw new InvalidOperationException(
                    "Cannot update a terminal queue entry (status=" + entry.getStatus() + ")");
        }

        QueueStatus oldStatus = entry.getStatus();
        entry.setStatus(newStatus);

        switch (newStatus) {
            case CALLED      -> entry.setCalledAt(LocalDateTime.now());
            case IN_PROGRESS -> entry.setSeenAt(LocalDateTime.now());
            case COMPLETED, CANCELLED, NO_SHOW -> {
                entry.setCompletedAt(LocalDateTime.now());
                // Free up doctor status if they had one
                if (entry.getDoctor() != null && newStatus == QueueStatus.COMPLETED) {
                    var doctor = entry.getDoctor();
                    doctor.setStatus(DoctorStatus.AVAILABLE);
                    doctorRepository.save(doctor);
                }
            }
            default -> { }
        }

        entry = queueEntryRepository.save(entry);

        broadcastQueueUpdate(entry.getDepartment().getId(), "STATUS_CHANGED");
        var cu = securityUtils.requireCurrentUser();
        auditLogService.log(cu.getUsername(), cu.getId(), "UPDATE_QUEUE_STATUS",
                "QueueEntry", String.valueOf(entryId), oldStatus.name(), newStatus.name());

        return entry;
    }

    // ─── Call next patient ─────────────────────────────────────────────────────

    @Transactional
    public QueueEntry callNext(Long departmentId) {
        List<QueueEntry> waiting = queueEntryRepository
                .findByDepartmentIdAndStatusInOrderByPriorityAscRegisteredAtAsc(
                        departmentId, List.of(QueueStatus.WAITING));

        if (waiting.isEmpty()) {
            throw new InvalidOperationException("No patients waiting in this department");
        }

        return updateStatus(waiting.get(0).getId(), QueueStatus.CALLED);
    }

    // ─── Reads ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<QueueEntry> findActiveByDepartment(Long deptId) {
        return queueEntryRepository.findActiveByDepartment(
                deptId, List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_PROGRESS));
    }

    @Transactional(readOnly = true)
    public List<QueueEntry> findByDoctor(Long doctorId) {
        return queueEntryRepository.findByDoctorIdAndStatusIn(
                doctorId, List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_PROGRESS));
    }

    @Transactional(readOnly = true)
    public QueueEntry findByTicket(String ticket) {
        return queueEntryRepository.findByTicketNumber(ticket)
                .orElseThrow(() -> new ResourceNotFoundException("QueueEntry", "ticket", ticket));
    }

    @Transactional(readOnly = true)
    public Page<QueueEntry> findAll(Pageable pageable) {
        return queueEntryRepository.findAllWithDetails(pageable);
    }

    @Transactional(readOnly = true)
    public List<QueueEntry> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return queueEntryRepository.findByDateRange(start, end);
    }

    @Transactional(readOnly = true)
    public QueueEntryResponse toResponse(QueueEntry e, List<QueueEntry> allActive) {
        int pos = IntStream.range(0, allActive.size())
                .filter(i -> allActive.get(i).getId().equals(e.getId()))
                .findFirst().orElse(-1) + 1;

        return QueueEntryResponse.builder()
                .id(e.getId())
                .ticketNumber(e.getTicketNumber())
                .patientName(e.getPatient().getFullName())
                .patientNumber(e.getPatient().getPatientNumber())
                .departmentName(e.getDepartment().getName())
                .doctorName(e.getDoctor() != null ? e.getDoctor().getUser().getFullName() : null)
                .priority(e.getPriority())
                .status(e.getStatus())
                .chiefComplaint(e.getChiefComplaint())
                .vitals(e.getVitals())
                .notes(e.getNotes())
                .registeredAt(e.getRegisteredAt())
                .calledAt(e.getCalledAt())
                .completedAt(e.getCompletedAt())
                .estimatedWaitMin(e.getEstimatedWaitMin())
                .positionInQueue(pos)
                .build();
    }

    public long countWaiting()    { return queueEntryRepository.countByStatus(QueueStatus.WAITING); }
    public long countInProgress() { return queueEntryRepository.countByStatus(QueueStatus.IN_PROGRESS); }
    public long countCompletedToday() {
        return queueEntryRepository.countByRegisteredAtBetween(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0), LocalDateTime.now());
    }
    public Double avgWaitMinutes() { return queueEntryRepository.findAverageWaitTimeMinutes(); }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void broadcastQueueUpdate(Long departmentId, String event) {
        messagingTemplate.convertAndSend("/topic/queue/" + departmentId,
                Map.of("event", event, "departmentId", departmentId,
                        "timestamp", LocalDateTime.now().toString()));
        // Also broadcast globally
        messagingTemplate.convertAndSend("/topic/queue/global",
                Map.of("event", event, "departmentId", departmentId));
    }
}
