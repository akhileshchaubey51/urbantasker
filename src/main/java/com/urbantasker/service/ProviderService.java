package com.urbantasker.service;

import com.urbantasker.dto.DashboardStatsDto;
import com.urbantasker.entity.Booking;
import com.urbantasker.entity.Provider;
import com.urbantasker.entity.User;
import com.urbantasker.repository.BookingRepository;
import com.urbantasker.repository.ProviderRepository;
import com.urbantasker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FileUploadService fileUploadService;

    public ProviderService(ProviderRepository providerRepository,
                           UserRepository userRepository,
                           BookingRepository bookingRepository,
                           FileUploadService fileUploadService) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.fileUploadService = fileUploadService;
    }

    @Transactional
    public Provider submitVerification(Long userId,
                                       String providerName,
                                       String serviceType,
                                       String serviceDetails,
                                       String address,
                                       String city,
                                       Integer experienceYears,
                                       BigDecimal hourlyRate,
                                       MultipartFile photoFile,
                                       MultipartFile aadhaarFile,
                                       MultipartFile policeFile,
                                       MultipartFile addressProofFile) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<Provider> existingOpt = providerRepository.findByUserId(userId);
        Provider provider = existingOpt.orElse(new Provider());

        provider.setUserId(user.getId());
        provider.setProviderName(providerName != null && !providerName.isBlank() ? providerName : user.getName());
        provider.setServiceType(serviceType);
        provider.setServiceDetails(serviceDetails);
        provider.setAddress(address);
        if (city != null && !city.isBlank()) provider.setCity(city);
        if (experienceYears != null) provider.setExperienceYears(experienceYears);
        if (hourlyRate != null) provider.setHourlyRate(hourlyRate);

        // Upload verification files
        if (photoFile != null && !photoFile.isEmpty()) {
            provider.setPhotoFile(fileUploadService.storeFile(photoFile, "photos"));
        }
        if (aadhaarFile != null && !aadhaarFile.isEmpty()) {
            provider.setAadhaarFile(fileUploadService.storeFile(aadhaarFile, "documents"));
        }
        if (policeFile != null && !policeFile.isEmpty()) {
            provider.setPoliceFile(fileUploadService.storeFile(policeFile, "documents"));
        }
        if (addressProofFile != null && !addressProofFile.isEmpty()) {
            provider.setAddressProofFile(fileUploadService.storeFile(addressProofFile, "documents"));
        }

        // Workflow state: PENDING -> APPROVED or REJECTED
        provider.setStatus("Pending");
        provider.setRejectReason(null);

        // Ensure user role is updated to 'provider'
        if (!"provider".equalsIgnoreCase(user.getRole())) {
            user.setRole("provider");
            userRepository.save(user);
        }

        return providerRepository.save(provider);
    }

    public Map<String, Object> getProviderDashboard(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider record not found"));

        long totalRequests = bookingRepository.countByProviderId(providerId);
        long pendingRequests = bookingRepository.countByProviderIdAndStatus(providerId, "Pending");
        long acceptedRequests = bookingRepository.countByProviderIdAndStatus(providerId, "Accepted");
        long completedBookings = bookingRepository.countByProviderIdAndStatus(providerId, "Completed");
        long rejectedBookings = bookingRepository.countByProviderIdAndStatus(providerId, "Rejected");

        BigDecimal totalEarnings = bookingRepository.calculateProviderEarnings(providerId);
        if (totalEarnings == null) totalEarnings = BigDecimal.ZERO;

        List<Booking> recentRequests = bookingRepository.findByProviderIdOrderByCreatedAtDesc(providerId);

        Map<String, Object> map = new HashMap<>();
        map.put("provider", provider);
        map.put("totalRequests", totalRequests);
        map.put("pendingRequests", pendingRequests);
        map.put("acceptedRequests", acceptedRequests);
        map.put("completedBookings", completedBookings);
        map.put("rejectedBookings", rejectedBookings);
        map.put("totalEarnings", totalEarnings);
        map.put("recentRequests", recentRequests);

        return map;
    }

    public Provider toggleAvailability(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        provider.setIsAvailable(!Boolean.TRUE.equals(provider.getIsAvailable()));
        return providerRepository.save(provider);
    }

    public Provider updateProfile(Long providerId, String serviceDetails, BigDecimal hourlyRate, String address, String city) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

        if (serviceDetails != null) provider.setServiceDetails(serviceDetails);
        if (hourlyRate != null) provider.setHourlyRate(hourlyRate);
        if (address != null) provider.setAddress(address);
        if (city != null) provider.setCity(city);

        return providerRepository.save(provider);
    }
}
