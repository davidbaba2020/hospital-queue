package com.hospital.queue.dto.response;

import com.hospital.queue.domain.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private long   expiresIn;
    private Long   userId;
    private String username;
    private String fullName;
    private String email;
    private Role   role;
}
