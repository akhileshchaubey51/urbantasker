package com.urbantasker.controller;

import com.urbantasker.dto.ApiResponse;
import com.urbantasker.dto.DashboardStatsDto;
import com.urbantasker.entity.*;
import com.urbantasker.service.AdminService;
import com.urbantasker.service.BookingService;
import com.urbantasker.service.ProviderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller Owner: Meghna
 * Responsibilities: Admin Dashboard, Provider Verification Workflow (Approve/Reject with Reason),
 * User & Provider Management, Service Catalog, Complaints, Activity Logs, Provider Verification Form Endpoint
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ProviderService providerService;
    private final BookingService bookingService;

    public AdminController(AdminService adminService,
                           ProviderService providerService,
                           BookingService bookingService) {
        this.adminService = adminService;
        this.providerService = providerService;
        this.bookingService = bookingService;
    }

    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return "admin".equalsIgnoreCase(role);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboard(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        DashboardStatsDto stats = adminService.getAdminStatistics();
        return ResponseEntity.ok(ApiResponse.ok("Admin stats loaded", stats));
    }

    @GetMapping("/providers/pending")
    public ResponseEntity<ApiResponse<List<Provider>>> getPendingProviders(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        List<Provider> pending = adminService.getPendingProviderApplications();
        return ResponseEntity.ok(ApiResponse.ok("Fetched pending provider applications", pending));
    }

    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<Provider>>> getAllProviders(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        List<Provider> all = adminService.getAllProviders();
        return ResponseEntity.ok(ApiResponse.ok("Fetched all providers", all));
    }

    @GetMapping("/providers/{id}")
    public ResponseEntity<ApiResponse<Provider>> getProviderDetails(@PathVariable("id") Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        try {
            Provider p = adminService.getProviderDetails(id);
            return ResponseEntity.ok(ApiResponse.ok("Fetched provider details", p));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PBL Rule 4: Admin Approves provider
     */
    @PutMapping("/providers/{id}/approve")
    public ResponseEntity<ApiResponse<Provider>> approveProvider(@PathVariable("id") Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        String adminEmail = (String) session.getAttribute("name");
        if (adminEmail == null) adminEmail = "admin@gmail.com";

        try {
            Provider approved = adminService.approveProvider(id, adminEmail);
            return ResponseEntity.ok(ApiResponse.ok("Provider '" + approved.getProviderName() + "' approved successfully!", approved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PBL Rule 4: Admin Rejects provider with reason
     */
    @PutMapping("/providers/{id}/reject")
    public ResponseEntity<ApiResponse<Provider>> rejectProvider(@PathVariable("id") Long id,
                                                               @RequestBody(required = false) Map<String, String> body,
                                                               HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        String reason = body != null ? body.get("reason") : "Verification documents could not be validated";
        String adminEmail = (String) session.getAttribute("name");
        if (adminEmail == null) adminEmail = "admin@gmail.com";

        try {
            Provider rejected = adminService.rejectProvider(id, reason, adminEmail);
            return ResponseEntity.ok(ApiResponse.ok("Provider application rejected", rejected));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Fetched all users", adminService.getAllUsers()));
    }

    @PutMapping("/users/{id}/toggle-block")
    public ResponseEntity<ApiResponse<User>> toggleUserBlock(@PathVariable("id") Long userId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        String adminEmail = (String) session.getAttribute("name");
        if (adminEmail == null) adminEmail = "admin@gmail.com";

        try {
            User user = adminService.toggleUserBlockStatus(userId, adminEmail);
            return ResponseEntity.ok(ApiResponse.ok("User status updated to " + user.getStatus(), user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Fetched all platform bookings", bookingService.getAllBookings()));
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceEntity>>> getAllServices() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched all services", adminService.getAllServices()));
    }

    @PostMapping("/services")
    public ResponseEntity<ApiResponse<ServiceEntity>> createService(@RequestBody ServiceEntity service, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        String adminEmail = (String) session.getAttribute("name");
        if (adminEmail == null) adminEmail = "admin@gmail.com";

        ServiceEntity saved = adminService.saveService(service, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("Service created successfully", saved));
    }

    @PutMapping("/services/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleService(@PathVariable("id") Long serviceId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        String adminEmail = (String) session.getAttribute("name");
        if (adminEmail == null) adminEmail = "admin@gmail.com";

        adminService.toggleServiceStatus(serviceId, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("Service status toggled"));
    }

    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<Complaint>>> getComplaints(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Fetched complaints", adminService.getAllComplaints()));
    }

    @PutMapping("/complaints/{id}/resolve")
    public ResponseEntity<ApiResponse<Complaint>> resolveComplaint(@PathVariable("id") Long id,
                                                                   @RequestBody Map<String, String> body,
                                                                   HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        String response = body.get("adminResponse");
        String adminEmail = (String) session.getAttribute("name");
        if (adminEmail == null) adminEmail = "admin@gmail.com";

        Complaint resolved = adminService.resolveComplaint(id, response, adminEmail);
        return ResponseEntity.ok(ApiResponse.ok("Complaint resolved", resolved));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<AdminLog>>> getLogs(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Fetched admin logs", adminService.getAdminLogs()));
    }

    /**
     * Provider Verification Form Submission
     * Used by Meghna's provider.html to register provider details and upload verification documents
     */
    @PostMapping(value = "/provider-verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Provider>> submitProviderVerification(
            @RequestParam("providerName") String providerName,
            @RequestParam("serviceType") String serviceType,
            @RequestParam("serviceDetails") String serviceDetails,
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam("experienceYears") Integer experienceYears,
            @RequestParam("hourlyRate") BigDecimal hourlyRate,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            @RequestParam(value = "aadhaarFile", required = false) MultipartFile aadhaarFile,
            @RequestParam(value = "policeFile", required = false) MultipartFile policeFile,
            @RequestParam(value = "addressProofFile", required = false) MultipartFile addressProofFile,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Please login before submitting provider verification"));
        }

        try {
            Provider provider = providerService.submitVerification(
                    userId, providerName, serviceType, serviceDetails, address, city,
                    experienceYears, hourlyRate, photoFile, aadhaarFile, policeFile, addressProofFile
            );

            session.setAttribute("role", "provider");
            session.setAttribute("providerId", provider.getId());

            return ResponseEntity.ok(ApiResponse.ok("Verification documents submitted successfully! Status: Pending Admin Approval.", provider));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Submission failed: " + e.getMessage()));
        }
    }
}
