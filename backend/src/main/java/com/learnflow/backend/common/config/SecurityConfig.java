package com.learnflow.backend.common.config;

import com.learnflow.backend.auth.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter filter,
        @Qualifier("corsConfigurationSource") CorsConfigurationSource cors
    ) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(value -> value.configurationSource(cors))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth
                    .dispatcherTypeMatchers(
                        jakarta.servlet.DispatcherType.ASYNC,
                        jakarta.servlet.DispatcherType.ERROR
                    )
                    .permitAll()
                    .requestMatchers(
                        "/api/auth/register/email-code",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/auth/reset-password/verify",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/demo-payments/view",
                        "/api/demo-payments/complete",
                        "/api/health"
                    )
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/images/*")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("Admin")
                    .anyRequest()
                    .authenticated()
            )
            .exceptionHandling(errors ->
                errors
                    .authenticationEntryPoint((request, response, exception) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                        response
                            .getWriter()
                            .write(
                                "{\"type\":\"about:blank\",\"title\":\"Chưa xác thực\",\"status\":401,\"detail\":\"Vui lòng đăng nhập để tiếp tục.\"}"
                            );
                    })
                    .accessDeniedHandler((request, response, exception) -> {
                        response.setStatus(403);
                        response.setCharacterEncoding("UTF-8");
                        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                        response
                            .getWriter()
                            .write(
                                "{\"title\":\"Không có quyền truy cập\",\"status\":403,\"detail\":\"Bạn cần quyền quản trị viên để thực hiện thao tác này.\"}"
                            );
                    })
            )
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${application.cors.allowed-origin}") String origin
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin, "http://127.0.0.1:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
