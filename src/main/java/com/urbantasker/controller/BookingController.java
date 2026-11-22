package com.urbantasker.controller;

import com.urbantasker.dto.ApiResponse;
import com.urbantasker.dto.BookingRequestDto;
import com.urbantasker.dto.ReviewRequestDto;
import com.urbantasker.entity.Booking;
import com.urbantasker.entity.Review;
import com.urbantasker.service.BookingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller Owner: Khushi
 * Responsibilities: Booking Creation, Customer Bookings History, Booking Cancellation, Reviews
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@Valid @RequestBody BookingRequestDto dto,
                                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Please log in to book a service"));
        }

        try {
            Booking booking = bookingService.createBooking(dto, userId);
            return ResponseEntity.ok(ApiResponse.ok("Booking request submitted successfully! Status: Pending", booking));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Booking failed: " + e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyBookings(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Please log in to view your bookings"));
        }

        List<Booking> list = bookingService.getCustomerBookings(userId);
        return ResponseEntity.ok(ApiResponse.ok("Fetched customer bookings", list));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(@PathVariable("id") Long bookingId,
                                                             @RequestBody(required = false) Map<String, String> body,
                                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        String reason = body != null ? body.get("reason") : "Cancelled by customer";
        try {
            Booking cancelled = bookingService.cancelBookingByCustomer(bookingId, userId, reason);
            return ResponseEntity.ok(ApiResponse.ok("Booking cancelled successfully", cancelled));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/review")
    public ResponseEntity<ApiResponse<Review>> addReview(@Valid @RequestBody ReviewRequestDto dto,
                                                        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Please log in to submit a review"));
        }

        try {
            Review review = bookingService.addReview(dto, userId);
            return ResponseEntity.ok(ApiResponse.ok("Thank you! Your review has been published.", review));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
