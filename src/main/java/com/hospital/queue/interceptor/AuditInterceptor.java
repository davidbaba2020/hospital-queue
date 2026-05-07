package com.hospital.queue.interceptor;

import com.hospital.queue.domain.entity.AuditLog;
import com.hospital.queue.repository.AuditLogRepository;
import com.hospital.queue.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {

    private static final Set<String> AUDITED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuditLogRepository auditLogRepository;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!AUDITED_METHODS.contains(request.getMethod())) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        try {
            String username = auth.getName();
            Long userId = null;
            if (auth.getPrincipal() instanceof CustomUserDetails ud) {
                userId = ud.getId();
            }

            String action = resolveAction(request);
            String entityType = resolveEntityType(request.getRequestURI());

            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .entityType(entityType)
                    .ipAddress(getClientIp(request))
                    .userAgent(truncate(request.getHeader("User-Agent"), 500))
                    .performedAt(LocalDateTime.now())
                    .status("SUCCESS")
                    .build();

            auditLogRepository.save(log);
        } catch (Exception e) {
            log.warn("Audit logging failed: {}", e.getMessage());
        }
        return true;
    }

    private String resolveAction(HttpServletRequest request) {
        String method = request.getMethod();
        String uri    = request.getRequestURI();
        return switch (method) {
            case "POST"   -> "CREATE_" + resolveEntityType(uri);
            case "PUT",
                 "PATCH"  -> "UPDATE_" + resolveEntityType(uri);
            case "DELETE" -> "DELETE_" + resolveEntityType(uri);
            default       -> method + " " + uri;
        };
    }

    private String resolveEntityType(String uri) {
        if (uri.contains("/queue"))        return "QUEUE_ENTRY";
        if (uri.contains("/patient"))      return "PATIENT";
        if (uri.contains("/doctor"))       return "DOCTOR";
        if (uri.contains("/user"))         return "USER";
        if (uri.contains("/department"))   return "DEPARTMENT";
        if (uri.contains("/report"))       return "REPORT";
        if (uri.contains("/auth/login"))   return "AUTH_LOGIN";
        return "RESOURCE";
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
