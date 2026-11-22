package com.urbantasker.dto;

public class AuthResponse {
    private Long userId;
    private String name;
    private String username;
    private String email;
    private String role; // 'user', 'provider', 'admin'
    private Long providerId; // Only present if role is 'provider'
    private String token;
    private String message;

    public AuthResponse() {}

    public AuthResponse(Long userId, String name, String username, String email, String role, Long providerId, String message) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.role = role;
        this.providerId = providerId;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
