package com.hospital.queue.dto.request;

import com.hospital.queue.domain.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 80, message = "Username must be 3-80 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, digits, dots, underscores, hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
             message = "Password must contain uppercase, digit and special character")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name max 150 characters")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "Invalid phone number")
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    // Doctor-specific
    private Long   departmentId;
    private String specialization;
    private String licenseNumber;
    private Integer consultationDurationMin;
}
