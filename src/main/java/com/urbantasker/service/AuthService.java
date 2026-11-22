package com.urbantasker.service;

import com.urbantasker.dto.AuthResponse;
import com.urbantasker.dto.LoginRequest;
import com.urbantasker.dto.RegisterRequest;
import com.urbantasker.entity.Admin;
import com.urbantasker.entity.Provider;
import com.urbantasker.entity.User;
import com.urbantasker.repository.AdminRepository;
import com.urbantasker.repository.ProviderRepository;
import com.urbantasker.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       AdminRepository adminRepository,
                       ProviderRepository providerRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.providerRepository = providerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        String identifier = request.getIdentifier().trim();
        String rawPassword = request.getPassword().trim();

        // 1. Check Admin Table first if identifier matches admin email or role is admin
        Optional<Admin> adminOpt = adminRepository.findByEmail(identifier);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (matchesPassword(rawPassword, admin.getPassword())) {
                // Set session attributes matching Image 2 rules
                session.setAttribute("userId", admin.getId());
                session.setAttribute("name", "Administrator");
                session.setAttribute("role", "admin");
                session.removeAttribute("providerId");

                return new AuthResponse(admin.getId(), "Administrator", "admin", admin.getEmail(), "admin", null, "Admin login successful");
            }
        }

        // 2. Check Users Table (Customers and Providers)
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(identifier, identifier);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if ("BLOCKED".equalsIgnoreCase(user.getStatus())) {
                throw new IllegalStateException("Your account has been blocked by administrator. Please contact support.");
            }

            if (matchesPassword(rawPassword, user.getPassword())) {
                Long providerId = null;
                if ("provider".equalsIgnoreCase(user.getRole())) {
                    Optional<Provider> pOpt = providerRepository.findByUserId(user.getId());
                    if (pOpt.isPresent()) {
                        providerId = pOpt.get().getId();
                    }
                }

                // Set session keys strictly as required by PBL Group Rule 1
                session.setAttribute("userId", user.getId());
                session.setAttribute("name", user.getName());
                session.setAttribute("role", user.getRole().toLowerCase());
                if (providerId != null) {
                    session.setAttribute("providerId", providerId);
                } else {
                    session.removeAttribute("providerId");
                }

                return new AuthResponse(user.getId(), user.getName(), user.getUsername(), user.getEmail(), user.getRole().toLowerCase(), providerId, "Login successful");
            }
        }

        throw new IllegalArgumentException("Invalid username/email or password");
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already registered");
        }

        // Security rule: users must not be able to register themselves as ADMIN
        String role = "user";
        if ("provider".equalsIgnoreCase(request.getRole())) {
            role = "provider";
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setMobile(request.getMobile().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setRole(role);
        user.setSecurityQuestion(request.getSecurityQuestion());
        user.setSecurityAnswer(request.getSecurityAnswer() != null ? request.getSecurityAnswer().trim().toLowerCase() : null);
        user.setAddress(request.getAddress());
        user.setStatus("ACTIVE");

        return userRepository.save(user);
    }

    public boolean resetPassword(String identifier, String securityAnswer, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(identifier, identifier);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with provided identifier");
        }

        User user = userOpt.get();
        if (user.getSecurityAnswer() == null || !user.getSecurityAnswer().equalsIgnoreCase(securityAnswer.trim())) {
            throw new IllegalArgumentException("Incorrect security answer");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
        return true;
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    private boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || rawPassword == null) return false;
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        }
        // Fallback for legacy plain-text seed entries (e.g. admin123)
        return rawPassword.equals(encodedPassword);
    }
}
