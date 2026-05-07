package com.hospital.queue.dto.request;

import com.hospital.queue.domain.enums.Priority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QueueEntryRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private Long doctorId;

    private Priority priority = Priority.NORMAL;

    @Size(max = 500, message = "Chief complaint max 500 characters")
    private String chiefComplaint;

    @Size(max = 1000, message = "Notes max 1000 characters")
    private String notes;

    @Size(max = 500, message = "Vitals max 500 characters")
    private String vitals;
}
