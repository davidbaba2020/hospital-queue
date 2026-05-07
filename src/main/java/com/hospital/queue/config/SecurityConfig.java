package com.hospital.queue.config;

import com.hospital.queue.security.CustomUserDetailsService;
import com.hospital.queue.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter  jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Stateless JWT — no CSRF for API, but enable for Thymeleaf forms ──
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/api/**",
                    "/h2-console/**",
                    "/ws/**"
                )
            )
            // ── Headers: allow H2 console frames in dev ──
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )
            // ── Session: STATELESS; JWT in cookie handles auth ──
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // ══════════════════ URL Authorization Rules ══════════════════
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers(
                    "/auth/**",
                    "/api/auth/**",
                    "/css/**", "/js/**", "/images/**", "/favicon.ico",
                    "/h2-console/**",
                    "/error/**",
                    "/actuator/health"
                ).permitAll()

                // Admin only
                .requestMatchers("/admin/**")
                    .hasRole("ADMIN")

                // Doctor
                .requestMatchers("/doctor/**")
                    .hasAnyRole("DOCTOR", "ADMIN")

                // Receptionist + Admin
                .requestMatchers("/receptionist/**")
                    .hasAnyRole("RECEPTIONIST", "ADMIN")

                // Reports
                .requestMatchers("/reports/**")
                    .hasAnyRole("ADMIN", "DOCTOR")

                // Chat — authenticated users
                .requestMatchers("/chat/**", "/ws/**")
                    .authenticated()

                // Queue status — patients and above
                .requestMatchers("/queue/**")
                    .authenticated()

                // API
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/doctor/**").hasAnyRole("DOCTOR", "ADMIN")
                .requestMatchers("/api/receptionist/**").hasAnyRole("RECEPTIONIST", "ADMIN")
                .requestMatchers("/api/**").authenticated()

                .anyRequest().authenticated()
            )
            // ── Login / Logout (Thymeleaf forms) ──
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout", "POST"))
                .logoutSuccessUrl("/auth/login?logout")
                .deleteCookies("HQ_TOKEN")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            // ── Exception handling ──
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.sendError(401, "Unauthorized");
                    } else {
                        res.sendRedirect("/auth/login?error=session");
                    }
                })
                .accessDeniedHandler((req, res, e) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.sendError(403, "Forbidden");
                    } else {
                        res.sendRedirect("/error/403");
                    }
                })
            )
            // ── JWT filter before Spring's auth filter ──
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
