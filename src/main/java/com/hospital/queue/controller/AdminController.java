package com.hospital.queue.controller;

import com.hospital.queue.domain.enums.Role;
import com.hospital.queue.dto.request.RegisterRequest;
import com.hospital.queue.dto.response.ApiResponse;
import com.hospital.queue.exception.ResourceNotFoundException;
import com.hospital.queue.repository.DepartmentRepository;
import com.hospital.queue.repository.UserRepository;
import com.hospital.queue.service.*;
import com.hospital.queue.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService   dashboardService;
    private final AuditLogService    auditLogService;
    private final AuthService        authService;
    private final DoctorService      doctorService;
    private final PatientService     patientService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository     userRepository;
    private final SecurityUtils      securityUtils;

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats",   dashboardService.getStats());
        model.addAttribute("doctors", doctorService.findAll());
        model.addAttribute("pageTitle", "Admin Dashboard");
        return "admin/dashboard";
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("departments", departmentRepository.findByActiveTrue());
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageTitle", "User Management");
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@Valid @ModelAttribute RegisterRequest req,
                             BindingResult br,
                             RedirectAttributes ra,
                             Model model) {
        if (br.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            model.addAttribute("roles", Role.values());
            model.addAttribute("errorMsg", "Validation errors. Please check the form.");
            return "admin/users";
        }
        try {
            authService.register(req);
            ra.addFlashAttribute("successMsg", "User created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresentOrElse(user -> {
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
            auditLogService.log(securityUtils.getCurrentUsername(),
                    securityUtils.getCurrentUserId(),
                    user.isEnabled() ? "ENABLE_USER" : "DISABLE_USER",
                    "User", String.valueOf(id), null, String.valueOf(user.isEnabled()));
            ra.addFlashAttribute("successMsg",
                    "User " + user.getUsername() + (user.isEnabled() ? " enabled." : " disabled."));
        }, () -> ra.addFlashAttribute("errorMsg", "User not found."));
        return "redirect:/admin/users";
    }

    @DeleteMapping("/users/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
        auditLogService.log(securityUtils.getCurrentUsername(),
                securityUtils.getCurrentUserId(), "DELETE_USER", "User", String.valueOf(id), null, null);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    // ─── Audit Logs ───────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String username,
                            Model model) {
        var pageable = PageRequest.of(page, size, Sort.by("performedAt").descending());
        var logs = username != null && !username.isBlank()
                ? auditLogService.findByUsername(username, pageable)
                : auditLogService.findAll(pageable);

        model.addAttribute("auditLogs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("usernameFilter", username);
        model.addAttribute("pageTitle", "Audit Logs");
        return "admin/audit-logs";
    }

    // ─── Departments ─────────────────────────────────────────────────────────

    @GetMapping("/departments")
    public String departments(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("pageTitle", "Departments");
        return "admin/departments";
    }
}
