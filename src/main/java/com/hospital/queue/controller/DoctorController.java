package com.hospital.queue.controller;

import com.hospital.queue.domain.enums.DoctorStatus;
import com.hospital.queue.domain.enums.QueueStatus;
import com.hospital.queue.dto.response.ApiResponse;
import com.hospital.queue.service.DoctorService;
import com.hospital.queue.service.QueueService;
import com.hospital.queue.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/doctor")
@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService  doctorService;
    private final QueueService   queueService;
    private final SecurityUtils  securityUtils;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var cu     = securityUtils.requireCurrentUser();
        var doctor = doctorService.findByUserId(cu.getId());
        var queue  = queueService.findByDoctor(doctor.getId());

        model.addAttribute("doctor",      doctor);
        model.addAttribute("queueEntries",queue);
        model.addAttribute("statuses",    DoctorStatus.values());
        model.addAttribute("pageTitle",   "Doctor Dashboard");
        return "doctor/dashboard";
    }

    @GetMapping("/queue")
    public String myQueue(Model model) {
        var cu     = securityUtils.requireCurrentUser();
        var doctor = doctorService.findByUserId(cu.getId());
        var queue  = queueService.findActiveByDepartment(doctor.getDepartment().getId());

        model.addAttribute("doctor",      doctor);
        model.addAttribute("queueEntries",queue);
        model.addAttribute("pageTitle",   "Department Queue");
        return "doctor/queue";
    }

    @PostMapping("/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @RequestParam DoctorStatus status) {
        var cu     = securityUtils.requireCurrentUser();
        var doctor = doctorService.findByUserId(cu.getId());
        doctorService.updateStatus(doctor.getId(), status, cu.getUsername(), cu.getId());
        return ResponseEntity.ok(ApiResponse.ok("Status updated to " + status.getLabel(), null));
    }

    @PostMapping("/queue/{entryId}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> updateQueueStatus(
            @PathVariable Long entryId,
            @RequestParam QueueStatus status) {
        queueService.updateStatus(entryId, status);
        return ResponseEntity.ok(ApiResponse.ok("Queue entry updated", null));
    }
}
