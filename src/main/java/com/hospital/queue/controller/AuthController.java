package com.hospital.queue.controller;

import com.hospital.queue.dto.request.LoginRequest;
import com.hospital.queue.dto.request.RegisterRequest;
import com.hospital.queue.dto.response.ApiResponse;
import com.hospital.queue.dto.response.AuthResponse;
import com.hospital.queue.repository.DepartmentRepository;
import com.hospital.queue.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService         authService;
    private final DepartmentRepository departmentRepository;

    // ─── Thymeleaf views ──────────────────────────────────────────────────────

    @GetMapping("/auth/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null)  model.addAttribute("errorMsg", resolveError(error));
        if (logout != null) model.addAttribute("successMsg", "You have been logged out successfully.");
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String login(@Valid @ModelAttribute LoginRequest req,
                        BindingResult br,
                        HttpServletResponse response,
                        RedirectAttributes ra,
                        Model model) {
        if (br.hasErrors()) {
            model.addAttribute("loginRequest", req);
            return "auth/login";
        }
        try {
            AuthResponse auth = authService.login(req, response);
            return switch (auth.getRole()) {
                case ADMIN        -> "redirect:/admin/dashboard";
                case DOCTOR       -> "redirect:/doctor/dashboard";
                case RECEPTIONIST -> "redirect:/receptionist/dashboard";
                default           -> "redirect:/queue/status";
            };
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Invalid username or password.");
            model.addAttribute("loginRequest", req);
            return "auth/login";
        }
    }

    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("departments", departmentRepository.findByActiveTrue());
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@Valid @ModelAttribute RegisterRequest req,
                           BindingResult br,
                           RedirectAttributes ra,
                           Model model) {
        if (br.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            return "auth/register";
        }
        try {
            authService.register(req);
            ra.addFlashAttribute("successMsg",
                    "Registration successful! You can now log in.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("departments", departmentRepository.findByActiveTrue());
            return "auth/register";
        }
    }

    @PostMapping("/auth/logout")
    public String logout(HttpServletResponse response) {
        authService.logout(response);
        return "redirect:/auth/login?logout";
    }

    // ─── REST API ─────────────────────────────────────────────────────────────

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<ApiResponse<AuthResponse>> apiLogin(
            @Valid @RequestBody LoginRequest req,
            HttpServletResponse response) {
        AuthResponse auth = authService.login(req, response);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", auth));
    }

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> apiRegister(
            @Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", null));
    }

    // ─── Root redirect ────────────────────────────────────────────────────────

    @GetMapping("/")
    public String root() {
        return "redirect:/auth/login";
    }

    private String resolveError(String error) {
        return switch (error) {
            case "session"  -> "Your session has expired. Please log in again.";
            case "disabled" -> "Your account is disabled. Contact an administrator.";
            case "locked"   -> "Your account is locked. Contact an administrator.";
            default         -> "Login failed. Please check your credentials.";
        };
    }
}
