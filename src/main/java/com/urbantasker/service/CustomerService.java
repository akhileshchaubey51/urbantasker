package com.urbantasker.service;

import com.urbantasker.entity.Booking;
import com.urbantasker.entity.Notification;
import com.urbantasker.entity.Provider;
import com.urbantasker.entity.Review;
import com.urbantasker.entity.ServiceEntity;
import com.urbantasker.entity.User;
import com.urbantasker.repository.BookingRepository;
import com.urbantasker.repository.NotificationRepository;
import com.urbantasker.repository.ProviderRepository;
import com.urbantasker.repository.ReviewRepository;
import com.urbantasker.repository.ServiceRepository;
import com.urbantasker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(UserRepository userRepository,
                           ProviderRepository providerRepository,
                           ServiceRepository serviceRepository,
                           BookingRepository bookingRepository,
                           ReviewRepository reviewRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Rule 3: Only approved providers show to customers. List must use WHERE status='Approved'
    public List<Provider> getApprovedProviders() {
        return providerRepository.findByStatus("Approved");
    }

    public List<Provider> searchAndFilterProviders(String serviceType, String city, BigDecimal maxPrice, BigDecimal minRating, String sortBy) {
        List<Provider> providers = providerRepository.findByStatus("Approved");

        return providers.stream()
                .filter(p -> serviceType == null || serviceType.isBlank() || 
                        p.getServiceType().toLowerCase().contains(serviceType.toLowerCase().trim()))
                .filter(p -> city == null || city.isBlank() || 
                        (p.getCity() != null && p.getCity().toLowerCase().contains(city.toLowerCase().trim())))
                .filter(p -> maxPrice == null || (p.getHourlyRate() != null && p.getHourlyRate().compareTo(maxPrice) <= 0))
                .filter(p -> minRating == null || (p.getRating() != null && p.getRating().compareTo(minRating) >= 0))
                .sorted((p1, p2) -> {
                    if ("price_asc".equalsIgnoreCase(sortBy)) {
                        return p1.getHourlyRate().compareTo(p2.getHourlyRate());
                    } else if ("price_desc".equalsIgnoreCase(sortBy)) {
                        return p2.getHourlyRate().compareTo(p1.getHourlyRate());
                    } else if ("experience".equalsIgnoreCase(sortBy)) {
                        return Integer.compare(p2.getExperienceYears() != null ? p2.getExperienceYears() : 0,
                                               p1.getExperienceYears() != null ? p1.getExperienceYears() : 0);
                    } else {
                        // default: highest rated
                        return p2.getRating().compareTo(p1.getRating());
                    }
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getProviderPublicProfile(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

        if (!"Approved".equalsIgnoreCase(provider.getStatus())) {
            throw new IllegalStateException("Provider is not approved yet");
        }

        List<Review> reviews = reviewRepository.findByProviderIdOrderByCreatedAtDesc(providerId);

        Map<String, Object> map = new HashMap<>();
        map.put("provider", provider);
        map.put("reviews", reviews);
        return map;
    }

    public Map<String, Object> getCustomerDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long totalBookings = bookingRepository.countByUserId(userId);
        long pendingBookings = bookingRepository.countByUserIdAndStatus(userId, "Pending");
        long acceptedBookings = bookingRepository.countByUserIdAndStatus(userId, "Accepted");
        long completedBookings = bookingRepository.countByUserIdAndStatus(userId, "Completed");
        long cancelledBookings = bookingRepository.countByUserIdAndStatus(userId, "Cancelled");

        List<Booking> recentBookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (recentBookings.size() > 5) {
            recentBookings = recentBookings.subList(0, 5);
        }

        List<Provider> recommendedProviders = providerRepository.findByStatus("Approved");
        if (recommendedProviders.size() > 4) {
            recommendedProviders = recommendedProviders.subList(0, 4);
        }

        List<ServiceEntity> popularServices = serviceRepository.findByIsActiveTrue();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("user", user);
        dashboard.put("totalBookings", totalBookings);
        dashboard.put("pendingBookings", pendingBookings);
        dashboard.put("acceptedBookings", acceptedBookings);
        dashboard.put("completedBookings", completedBookings);
        dashboard.put("cancelledBookings", cancelledBookings);
        dashboard.put("recentBookings", recentBookings);
        dashboard.put("recommendedProviders", recommendedProviders);
        dashboard.put("popularServices", popularServices);
        dashboard.put("notifications", notifications);

        return dashboard;
    }

    public User updateProfile(Long userId, String name, String mobile, String address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (name != null && !name.isBlank()) user.setName(name.trim());
        if (mobile != null && !mobile.isBlank()) user.setMobile(mobile.trim());
        if (address != null) user.setAddress(address.trim());
        return userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword()) && !currentPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
