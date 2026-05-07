package com.hospital.queue.service;

import com.hospital.queue.domain.entity.Doctor;
import com.hospital.queue.domain.entity.User;
import com.hospital.queue.domain.enums.Role;
import com.hospital.queue.dto.request.LoginRequest;
import com.hospital.queue.dto.request.RegisterRequest;
import com.hospital.queue.dto.response.AuthResponse;
import com.hospital.queue.exception.DuplicateResourceException;
import com.hospital.queue.exception.ResourceNotFoundException;
import com.hospital.queue.repository.DepartmentRepository;
import com.hospital.queue.repository.DoctorRepository;
import com.hospital.queue.repository.UserRepository;
import com.hospital.queue.security.CustomUserDetails;
import com.hospital.queue.security.jwt.JwtProperties;
import com.hospital.queue.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository        userRepository;
    private final DoctorRepository      doctorRepository;
    private final DepartmentRepository  departmentRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtTokenProvider      jwtTokenProvider;
    private final JwtProperties         jwtProperties;
    private final AuditLogService       auditLogService;

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletResponse response) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        // Set JWT in HttpOnly cookie (secure for Thymeleaf flow)
        Cookie cookie = new Cookie(jwtProperties.getCookieName(), token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtProperties.getExpirationMs() / 1000));
        // cookie.setSecure(true); // enable in prod with HTTPS
        response.addCookie(cookie);

        auditLogService.log(userDetails.getUsername(), userDetails.getId(),
                "USER_LOGIN", "User", String.valueOf(userDetails.getId()), null, null);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .fullName(userDetails.getFullName())
                .email(userDetails.getEmail())
                .role(userDetails.getRole())
                .build();
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + req.getUsername());
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + req.getEmail());
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .role(req.getRole())
                .enabled(true)
                .locked(false)
                .build();

        user = userRepository.save(user);

        // If registering a doctor, create doctor profile
        if (req.getRole() == Role.DOCTOR) {
            if (req.getDepartmentId() == null || req.getLicenseNumber() == null) {
                throw new IllegalArgumentException(
                        "Department and license number are required for doctor registration");
            }
            if (doctorRepository.existsByLicenseNumber(req.getLicenseNumber())) {
                throw new DuplicateResourceException(
                        "License number already registered: " + req.getLicenseNumber());
            }
            var department = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department", "id", req.getDepartmentId()));

            Doctor doctor = Doctor.builder()
                    .user(user)
                    .department(department)
                    .specialization(req.getSpecialization())
                    .licenseNumber(req.getLicenseNumber())
                    .consultationDurationMin(
                            req.getConsultationDurationMin() != null
                                    ? req.getConsultationDurationMin() : 15)
                    .build();
            doctorRepository.save(doctor);
        }

        log.info("New user registered: {} with role {}", user.getUsername(), user.getRole());
        return user;
    }

    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(jwtProperties.getCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
