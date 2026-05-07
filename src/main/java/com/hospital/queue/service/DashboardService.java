package com.hospital.queue.service;

import com.hospital.queue.dto.response.DashboardStats;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final QueueService    queueService;
    private final DoctorService   doctorService;
    private final PatientService  patientService;
    private final AuditLogService auditLogService;

    public DashboardStats getStats() {
        return DashboardStats.builder()
                .totalPatientsToday(queueService.countCompletedToday())
                .activeQueueCount(queueService.countWaiting() + queueService.countInProgress())
                .completedToday(queueService.countCompletedToday())
                .waitingCount(queueService.countWaiting())
                .inProgressCount(queueService.countInProgress())
                .availableDoctors(doctorService.countAvailable())
                .totalDoctors(doctorService.countAll())
                .totalPatients(patientService.count())
                .averageWaitMinutes(queueService.avgWaitMinutes())
                .auditEventsToday(auditLogService.countToday())
                .build();
    }
}
