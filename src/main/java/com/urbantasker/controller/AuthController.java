package com.urbantasker.controller;

import com.urbantasker.dto.ApiResponse;
import com.urbantasker.dto.AuthResponse;
import com.urbantasker.dto.LoginRequest;
import com.urbantasker.dto.RegisterRequest;
import com.urbantasker.entity.User;
import com.urbantasker.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller Owner: Khushi
 * Responsibilities: User Authentication, Registration, Session Management, Password Recovery
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        try {
            AuthResponse response = authService.login(request, session);
            return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User registered = authService.register(request);
            Map<String, Object> data = new HashMap<>();
            data.put("id", registered.getId());
            data.put("username", registered.getUsername());
            data.put("name", registered.getName());
            data.put("email", registered.getEmail());
            data.put("role", registered.getRole());

            return ResponseEntity.ok(ApiResponse.ok("Registration successful! Please log in.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.ok("No active session", null));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", session.getAttribute("userId"));
        data.put("name", session.getAttribute("name"));
        data.put("role", session.getAttribute("role"));
        data.put("providerId", session.getAttribute("providerId"));

        return ResponseEntity.ok(ApiResponse.ok("Active session found", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> payload) {
        String identifier = payload.get("identifier");
        String securityAnswer = payload.get("securityAnswer");
        String newPassword = payload.get("newPassword");

        if (identifier == null || securityAnswer == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing fields or password too short (min 6 chars)"));
        }

        try {
            authService.resetPassword(identifier, securityAnswer, newPassword);
            return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully. Please log in."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
