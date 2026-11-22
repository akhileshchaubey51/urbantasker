package com.urbantasker.controller;

import com.urbantasker.dto.ApiResponse;
import com.urbantasker.entity.Booking;
import com.urbantasker.entity.Provider;
import com.urbantasker.repository.ProviderRepository;
import com.urbantasker.service.BookingService;
import com.urbantasker.service.ProviderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller Owner: Divyansh
 * Responsibilities: Provider Dashboard, Booking Request Management (Accept/Reject/Complete), Provider Earnings, Availability Toggle
 */
@RestController
@RequestMapping("/api/provider")
public class ProviderPanelController {

    private final ProviderService providerService;
    private final BookingService bookingService;
    private final ProviderRepository providerRepository;

    public ProviderPanelController(ProviderService providerService,
                                   BookingService bookingService,
                                   ProviderRepository providerRepository) {
        this.providerService = providerService;
        this.bookingService = bookingService;
        this.providerRepository = providerRepository;
    }

    private Long resolveProviderId(HttpSession session) {
        Object pId = session.getAttribute("providerId");
        if (pId != null) {
            return (Long) pId;
        }

        // If not in session, lookup by userId
        Object uId = session.getAttribute("userId");
        if (uId != null) {
            return providerRepository.findByUserId((Long) uId)
                    .map(Provider::getId)
                    .orElse(null);
        }
        return null;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(HttpSession session) {
        Long providerId = resolveProviderId(session);
        if (providerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Access denied: Not registered or logged in as a provider"));
        }

        try {
            Map<String, Object> data = providerService.getProviderDashboard(providerId);
            return ResponseEntity.ok(ApiResponse.ok("Provider dashboard data loaded", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<Booking>>> getIncomingRequests(HttpSession session) {
        Long providerId = resolveProviderId(session);
        if (providerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized provider access"));
        }

        List<Booking> requests = bookingService.getProviderBookings(providerId);
        return ResponseEntity.ok(ApiResponse.ok("Fetched incoming requests", requests));
    }

    /**
     * PBL Rule 2: Divyansh changes status to Accepted
     */
    @PutMapping("/requests/{id}/accept")
    public ResponseEntity<ApiResponse<Booking>> acceptRequest(@PathVariable("id") Long bookingId,
                                                             HttpSession session) {
        Long providerId = resolveProviderId(session);
        if (providerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized provider access"));
        }

        try {
            Booking updated = bookingService.updateBookingStatusByProvider(bookingId, providerId, "Accepted", null);
            return ResponseEntity.ok(ApiResponse.ok("Booking #" + bookingId + " has been Accepted", updated));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PBL Rule 2: Divyansh changes status to Rejected
     */
    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<ApiResponse<Booking>> rejectRequest(@PathVariable("id") Long bookingId,
                                                             @RequestBody(required = false) Map<String, String> body,
                                                             HttpSession session) {
        Long providerId = resolveProviderId(session);
        if (providerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized provider access"));
        }

        String reason = body != null ? body.get("reason") : "Provider is unavailable at the requested time";

        try {
            Booking updated = bookingService.updateBookingStatusByProvider(bookingId, providerId, "Rejected", reason);
            return ResponseEntity.ok(ApiResponse.ok("Booking #" + bookingId + " has been Rejected", updated));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Divyansh marks booking as Completed
     */
    @PutMapping("/requests/{id}/complete")
    public ResponseEntity<ApiResponse<Booking>> completeBooking(@PathVariable("id") Long bookingId,
                                                               HttpSession session) {
        Long providerId = resolveProviderId(session);
        if (providerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized provider access"));
        }

        try {
            Booking updated = bookingService.updateBookingStatusByProvider(bookingId, providerId, "Completed", null);
            return ResponseEntity.ok(ApiResponse.ok("Booking #" + bookingId + " marked Completed! Payment updated.", updated));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/toggle-availability")
    public ResponseEntity<ApiResponse<Provider>> toggleAvailability(HttpSession session) {
        Long providerId = resolveProviderId(session);
        if (providerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        Provider updated = providerService.toggleAvailability(providerId);
        return ResponseEntity.ok(ApiResponse.ok("Availability updated: " + (Boolean.TRUE.equals(updated.getIsAvailable()) ? "Available" : "Busy"), updated));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Provider>> updateProfile(@RequestBody Map<String, Object> body,
                                                              HttpSession session) {
        Long providerId = resolveProviderId(session);
        if (providerId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        String serviceDetails = (String) body.get("serviceDetails");
        String address = (String) body.get("address");
        String city = (String) body.get("city");
        BigDecimal hourlyRate = null;
        if (body.get("hourlyRate") != null) {
            hourlyRate = new BigDecimal(body.get("hourlyRate").toString());
        }

        Provider updated = providerService.updateProfile(providerId, serviceDetails, hourlyRate, address, city);
        return ResponseEntity.ok(ApiResponse.ok("Provider profile updated", updated));
    }
}
