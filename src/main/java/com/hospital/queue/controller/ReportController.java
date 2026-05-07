package com.hospital.queue.controller;

import com.hospital.queue.service.ReportService;
import com.hospital.queue.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService  reportService;
    private final SecurityUtils  securityUtils;

    @GetMapping
    public String reportsPage(Model model) {
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("pageTitle", "Reports");
        return "reports/index";
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] data = reportService.generateExcelReport(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=queue-report-" + from + ".xlsx")
                .body(data);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] data = reportService.generatePdfReport(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=queue-report-" + from + ".pdf")
                .body(data);
    }

    @PostMapping("/email")
    public String emailReport(
            @RequestParam String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            RedirectAttributes ra) {
        var cu = securityUtils.requireCurrentUser();
        reportService.emailReport(cu.getEmail(), cu.getFullName(), format,
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        ra.addFlashAttribute("successMsg",
                "Report is being sent to " + cu.getEmail());
        return "redirect:/reports";
    }
}
