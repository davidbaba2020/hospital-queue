package com.hospital.queue.util;

import com.hospital.queue.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    public Optional<CustomUserDetails> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails ud) {
            return Optional.of(ud);
        }
        return Optional.empty();
    }

    public CustomUserDetails requireCurrentUser() {
        return getCurrentUser().orElseThrow(
                () -> new IllegalStateException("No authenticated user in security context"));
    }

    public String getCurrentUsername() {
        return requireCurrentUser().getUsername();
    }

    public Long getCurrentUserId() {
        return requireCurrentUser()
                .getId();
    }
}
