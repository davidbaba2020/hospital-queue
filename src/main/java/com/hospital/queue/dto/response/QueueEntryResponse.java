package com.hospital.queue.dto.response;

import com.hospital.queue.domain.enums.Priority;
import com.hospital.queue.domain.enums.QueueStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class QueueEntryResponse {
    private Long          id;
    private String        ticketNumber;
    private String        patientName;
    private String        patientNumber;
    private String        departmentName;
    private String        doctorName;
    private Priority      priority;
    private QueueStatus   status;
    private String        chiefComplaint;
    private String        vitals;
    private String        notes;
    private LocalDateTime registeredAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;
    private Integer       estimatedWaitMin;
    private int           positionInQueue;
}
