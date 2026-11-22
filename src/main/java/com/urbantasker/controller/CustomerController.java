package com.urbantasker.controller;

import com.urbantasker.dto.ApiResponse;
import com.urbantasker.entity.Provider;
import com.urbantasker.entity.ServiceEntity;
import com.urbantasker.entity.User;
import com.urbantasker.repository.ServiceRepository;
import com.urbantasker.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller Owner: Akansha
 * Responsibilities: Customer Dashboard, Approved Provider Browsing & Filtering, Provider Profile, Service Catalog
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService customerService;
    private final ServiceRepository serviceRepository;

    public CustomerController(CustomerService customerService, ServiceRepository serviceRepository) {
        this.customerService = customerService;
        this.serviceRepository = serviceRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Please log in to access your dashboard"));
        }

        try {
            Map<String, Object> dashboard = customerService.getCustomerDashboard(userId);
            return ResponseEntity.ok(ApiResponse.ok("Customer dashboard loaded", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load dashboard: " + e.getMessage()));
        }
    }

    /**
     * PBL Rule 3: Only approved providers show to customers.
     * Akansha's list must use WHERE status='Approved'
     */
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<Provider>>> getApprovedProviders(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false, defaultValue = "rating") String sortBy) {

        List<Provider> list = customerService.searchAndFilterProviders(serviceType, city, maxPrice, minRating, sortBy);
        return ResponseEntity.ok(ApiResponse.ok("Fetched approved providers", list));
    }

    @GetMapping("/providers/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderProfile(@PathVariable("id") Long providerId) {
        try {
            Map<String, Object> data = customerService.getProviderPublicProfile(providerId);
            return ResponseEntity.ok(ApiResponse.ok("Fetched provider profile", data));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceEntity>>> getAvailableServices() {
        List<ServiceEntity> services = serviceRepository.findByIsActiveTrue();
        return ResponseEntity.ok(ApiResponse.ok("Fetched active services", services));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        String name = body.get("name");
        String mobile = body.get("mobile");
        String address = body.get("address");

        try {
            User updated = customerService.updateProfile(userId, name, mobile, address);
            session.setAttribute("name", updated.getName());
            return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Password must be at least 6 characters"));
        }

        try {
            customerService.changePassword(userId, currentPassword, newPassword);
            return ResponseEntity.ok(ApiResponse.ok("Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
