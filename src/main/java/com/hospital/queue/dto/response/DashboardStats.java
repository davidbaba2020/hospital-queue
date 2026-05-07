package com.hospital.queue.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DashboardStats {
    private long totalPatientsToday;
    private long activeQueueCount;
    private long completedToday;
    private long waitingCount;
    private long inProgressCount;
    private long availableDoctors;
    private long totalDoctors;
    private long totalPatients;
    private Double averageWaitMinutes;
    private long auditEventsToday;
}
