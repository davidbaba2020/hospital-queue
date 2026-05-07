package com.hospital.queue.controller;

import com.hospital.queue.dto.request.PatientRequest;
import com.hospital.queue.dto.request.QueueEntryRequest;
import com.hospital.queue.dto.response.ApiResponse;
import com.hospital.queue.dto.response.QueueEntryResponse;
import com.hospital.queue.service.DoctorService;
import com.hospital.queue.service.PatientService;
import com.hospital.queue.service.QueueService;
import com.hospital.queue.repository.DepartmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/receptionist")
@PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
@RequiredArgsConstructor
public class ReceptionistController {

    private final PatientService       patientService;
    private final QueueService         queueService;
    private final DoctorService        doctorService;
    private final DepartmentRepository departmentRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("queuePage",   queueService.findAll(PageRequest.of(0, 20)));
        model.addAttribute("departments", departmentRepository.findByActiveTrue());
        model.addAttribute("pageTitle",   "Receptionist Dashboard");
        return "receptionist/dashboard";
    }

    @GetMapping("/register-patient")
    public String registerPatientPage(Model model) {
        model.addAttribute("patientRequest",    new PatientRequest());
        model.addAttribute("queueEntryRequest", new QueueEntryRequest());
        model.addAttribute("departments",       departmentRepository.findByActiveTrue());
        model.addAttribute("pageTitle",         "Register Patient");
        return "receptionist/register-patient";
    }

    @PostMapping("/patients")
    public String registerPatient(@Valid @ModelAttribute PatientRequest req,
                                  BindingResult br,
                                  RedirectAttributes ra,
                                  Model model) {
        if (br.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            model.addAttribute("queueEntryRequest", new QueueEntryRequest());
            return "receptionist/register-patient";
        }
        try {
            var patient = patientService.register(req);
            ra.addFlashAttribute("successMsg",
                    "Patient registered: " + patient.getPatientNumber());
            ra.addFlashAttribute("newPatientId", patient.getId());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/receptionist/register-patient";
    }

    @PostMapping("/queue")
    @ResponseBody
    public ResponseEntity<ApiResponse<QueueEntryResponse>> addToQueue(
            @Valid @RequestBody QueueEntryRequest req) {
        var entry = queueService.enqueue(req);
        var active = queueService.findActiveByDepartment(entry.getDepartment().getId());
        return ResponseEntity.ok(ApiResponse.ok("Patient added to queue",
                queueService.toResponse(entry, active)));
    }

    @PostMapping("/queue/{deptId}/call-next")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> callNext(@PathVariable Long deptId) {
        var entry = queueService.callNext(deptId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Called: " + entry.getPatient().getFullName() + " — " + entry.getTicketNumber(),
                null));
    }

    @GetMapping("/patients/search")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> searchPatients(@RequestParam String q) {
        var result = patientService.search(q, PageRequest.of(0, 10));
        return ResponseEntity.ok(ApiResponse.ok(result.getContent()));
    }
}
