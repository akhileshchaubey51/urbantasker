package com.urbantasker.service;

import com.urbantasker.dto.DashboardStatsDto;
import com.urbantasker.entity.*;
import com.urbantasker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final ComplaintRepository complaintRepository;
    private final AdminLogRepository adminLogRepository;
    private final NotificationRepository notificationRepository;

    public AdminService(UserRepository userRepository,
                        ProviderRepository providerRepository,
                        BookingRepository bookingRepository,
                        ServiceRepository serviceRepository,
                        ComplaintRepository complaintRepository,
                        AdminLogRepository adminLogRepository,
                        NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.complaintRepository = complaintRepository;
        this.adminLogRepository = adminLogRepository;
        this.notificationRepository = notificationRepository;
    }

    public DashboardStatsDto getAdminStatistics() {
        DashboardStatsDto stats = new DashboardStatsDto();

        stats.setTotalCustomers(userRepository.countByRole("user"));
        stats.setTotalProviders(providerRepository.count());
        stats.setPendingProviders(providerRepository.countByStatus("Pending"));
        stats.setApprovedProviders(providerRepository.countByStatus("Approved"));
        stats.setRejectedProviders(providerRepository.countByStatus("Rejected"));

        stats.setTotalBookings(bookingRepository.count());
        stats.setPendingBookings(bookingRepository.countByStatus("Pending"));
        stats.setAcceptedBookings(bookingRepository.countByStatus("Accepted"));
        stats.setCompletedBookings(bookingRepository.countByStatus("Completed"));
        stats.setCancelledBookings(bookingRepository.countByStatus("Cancelled"));

        BigDecimal revenue = bookingRepository.calculatePlatformRevenue();
        stats.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        // Category distribution for Chart.js
        List<ServiceEntity> services = serviceRepository.findAll();
        Map<String, Long> categoryCount = services.stream()
                .collect(Collectors.groupingBy(ServiceEntity::getCategory, Collectors.counting()));
        stats.setCategoryDistribution(categoryCount);

        return stats;
    }

    public List<Provider> getPendingProviderApplications() {
        return providerRepository.findByStatus("Pending");
    }

    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }

    public Provider getProviderDetails(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found with ID: " + id));
    }

    @Transactional
    public Provider approveProvider(Long providerId, String adminEmail) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

        provider.setStatus("Approved");
        provider.setRejectReason(null);
        Provider saved = providerRepository.save(provider);

        // Record Admin Activity Log
        adminLogRepository.save(new AdminLog(
                adminEmail,
                "PROVIDER_APPROVED",
                "Approved provider application for '" + provider.getProviderName() + "' (Provider ID: " + provider.getId() + ")",
                "127.0.0.1"
        ));

        // Notify Provider
        if (provider.getUserId() != null) {
            notificationRepository.save(new Notification(
                    provider.getUserId(),
                    "Application Approved!",
                    "Congratulations! Your provider profile has been verified and approved by the administrator. You are now active and can receive service bookings.",
                    "STATUS"
            ));
        }

        return saved;
    }

    @Transactional
    public Provider rejectProvider(Long providerId, String reason, String adminEmail) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

        provider.setStatus("Rejected");
        provider.setRejectReason(reason);
        Provider saved = providerRepository.save(provider);

        // Record Admin Activity Log
        adminLogRepository.save(new AdminLog(
                adminEmail,
                "PROVIDER_REJECTED",
                "Rejected provider application for '" + provider.getProviderName() + "' (ID: " + provider.getId() + "). Reason: " + reason,
                "127.0.0.1"
        ));

        // Notify Provider
        if (provider.getUserId() != null) {
            notificationRepository.save(new Notification(
                    provider.getUserId(),
                    "Application Update: Action Required",
                    "Your provider application was not approved. Reason: " + (reason != null ? reason : "Documents could not be verified") + ". You may re-submit your documents.",
                    "STATUS"
            ));
        }

        return saved;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User toggleUserBlockStatus(Long userId, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newStatus = "ACTIVE".equalsIgnoreCase(user.getStatus()) ? "BLOCKED" : "ACTIVE";
        user.setStatus(newStatus);
        User saved = userRepository.save(user);

        adminLogRepository.save(new AdminLog(
                adminEmail,
                "USER_STATUS_CHANGE",
                "Changed status of user '" + user.getUsername() + "' to " + newStatus,
                "127.0.0.1"
        ));

        return saved;
    }

    // Service Management
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }

    @Transactional
    public ServiceEntity saveService(ServiceEntity service, String adminEmail) {
        ServiceEntity saved = serviceRepository.save(service);
        adminLogRepository.save(new AdminLog(
                adminEmail,
                "SERVICE_SAVED",
                "Added/Updated service: " + saved.getServiceName(),
                "127.0.0.1"
        ));
        return saved;
    }

    @Transactional
    public void toggleServiceStatus(Long serviceId, String adminEmail) {
        ServiceEntity s = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        s.setIsActive(!Boolean.TRUE.equals(s.getIsActive()));
        serviceRepository.save(s);
        adminLogRepository.save(new AdminLog(
                adminEmail,
                "SERVICE_TOGGLE",
                "Toggled active state of service: " + s.getServiceName() + " to " + s.getIsActive(),
                "127.0.0.1"
        ));
    }

    // Complaint Management
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Complaint resolveComplaint(Long complaintId, String adminResponse, String adminEmail) {
        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        c.setStatus("RESOLVED");
        c.setAdminResponse(adminResponse);
        Complaint saved = complaintRepository.save(c);

        adminLogRepository.save(new AdminLog(
                adminEmail,
                "COMPLAINT_RESOLVED",
                "Resolved complaint #" + c.getId() + " from user " + c.getUserName(),
                "127.0.0.1"
        ));

        if (c.getUserId() != null) {
            notificationRepository.save(new Notification(
                    c.getUserId(),
                    "Complaint #" + c.getId() + " Resolved",
                    "Admin Response: " + adminResponse,
                    "SYSTEM"
            ));
        }

        return saved;
    }

    public List<AdminLog> getAdminLogs() {
        return adminLogRepository.findAllByOrderByCreatedAtDesc();
    }
}
