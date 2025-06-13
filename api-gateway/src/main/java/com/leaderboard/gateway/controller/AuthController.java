package com.leaderboard.gateway.controller;

import com.leaderboard.gateway.dto.LoginRequest;
import com.leaderboard.gateway.dto.LoginResponse;
import com.leaderboard.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 *
 * Handles authentication requests and JWT token generation.
 * In a real application, this would integrate with a user service.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Simple in-memory user store for demo purposes
    // In production, this would be replaced with a proper user service
    private final Map<String, String> users = new HashMap<>();

    @PostConstruct
    public void initUsers() {
        users.put("player1", passwordEncoder.encode("password1"));
        users.put("player2", passwordEncoder.encode("password2"));
        users.put("admin", passwordEncoder.encode("admin123"));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        return Mono.fromCallable(() -> {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            // Validate credentials
            if (users.containsKey(username) &&
                    passwordEncoder.matches(password, users.get(username))) {

                // Generate JWT token
                Map<String, Object> claims = new HashMap<>();
                claims.put("role", username.equals("admin") ? "ADMIN" : "PLAYER");

                String token = jwtUtil.generateToken(username, claims);

                LoginResponse response = new LoginResponse();
                response.setToken(token);
                response.setUsername(username);
                response.setExpiresIn(24 * 60 * 60 * 1000L); // 24 hours

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponse("Invalid credentials"));
            }
        });
    }

    @PostMapping("/validate")
    public Mono<ResponseEntity<Map<String, Object>>> validateToken(@RequestHeader("Authorization") String authHeader) {
        return Mono.fromCallable(() -> {
            try {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);

                    if (jwtUtil.validateToken(token)) {
                        String username = jwtUtil.getUsernameFromToken(token);

                        Map<String, Object> response = new HashMap<>();
                        response.put("valid", true);
                        response.put("username", username);
                        response.put("expiresAt", jwtUtil.getExpirationDateFromToken(token));

                        return ResponseEntity.ok(response);
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Invalid or expired token");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Token validation error");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.fromCallable(() -> {
            Map<String, String> response = new HashMap<>();
            response.put("status", "UP");
            response.put("service", "auth-service");
            return ResponseEntity.ok(response);
        });
    }
}