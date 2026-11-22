package com.urbantasker.service;

import com.urbantasker.dto.BookingRequestDto;
import com.urbantasker.dto.ReviewRequestDto;
import com.urbantasker.entity.*;
import com.urbantasker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    public BookingService(BookingRepository bookingRepository,
                          ProviderRepository providerRepository,
                          UserRepository userRepository,
                          ServiceRepository serviceRepository,
                          ReviewRepository reviewRepository,
                          NotificationRepository notificationRepository) {
        this.bookingRepository = bookingRepository;
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Booking createBooking(BookingRequestDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Provider provider = providerRepository.findById(dto.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

        if (!"Approved".equalsIgnoreCase(provider.getStatus())) {
            throw new IllegalStateException("Selected provider is not approved to receive bookings");
        }

        if (dto.getBookingDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Booking date cannot be in the past");
        }

        Booking booking = new Booking();
        booking.setUserId(user.getId());
        booking.setUserName(user.getName());
        booking.setProviderId(provider.getId());
        booking.setProviderName(provider.getProviderName());
        booking.setServiceId(dto.getServiceId());
        booking.setServiceName(dto.getServiceName() != null ? dto.getServiceName() : provider.getServiceType());
        booking.setBookingDate(dto.getBookingDate());
        booking.setBookingTime(dto.getBookingTime());
        booking.setServiceAddress(dto.getServiceAddress());
        booking.setContactNumber(dto.getContactNumber() != null ? dto.getContactNumber() : user.getMobile());
        booking.setInstructions(dto.getInstructions());

        BigDecimal price = dto.getEstimatedPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            price = provider.getHourlyRate() != null ? provider.getHourlyRate() : new BigDecimal("399.00");
        }
        booking.setEstimatedPrice(price);

        // "Payment system: Cash + Online दोनों"
        String paymentMode = dto.getPaymentMode() != null ? dto.getPaymentMode().toUpperCase() : "CASH";
        booking.setPaymentMode(paymentMode);
        if ("ONLINE".equals(paymentMode)) {
            booking.setPaymentStatus("PAID");
        } else {
            booking.setPaymentStatus("PENDING");
        }

        // Image 2 Rule 2: Booking status words. Use exactly Pending, Accepted and Rejected
        booking.setStatus("Pending");

        Booking saved = bookingRepository.save(booking);

        // Notify Provider
        if (provider.getUserId() != null) {
            notificationRepository.save(new Notification(
                    provider.getUserId(),
                    "New Service Request #" + saved.getId(),
                    "You have received a new booking from " + user.getName() + " for " + saved.getServiceName() + " on " + saved.getBookingDate() + " at " + saved.getBookingTime() + ".",
                    "BOOKING"
            ));
        }

        // Notify Customer
        notificationRepository.save(new Notification(
                user.getId(),
                "Booking Placed Successfully",
                "Your booking #" + saved.getId() + " with " + provider.getProviderName() + " has been placed and is currently Pending provider confirmation.",
                "BOOKING"
        ));

        return saved;
    }

    public List<Booking> getCustomerBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Booking> getProviderBookings(Long providerId) {
        return bookingRepository.findByProviderIdOrderByCreatedAtDesc(providerId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional
    public Booking updateBookingStatusByProvider(Long bookingId, Long providerId, String newStatus, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getProviderId().equals(providerId)) {
            throw new SecurityException("Unauthorized: You do not own this booking");
        }

        // Rule 2: Use exactly Accepted, Rejected, Completed
        if ("Accepted".equalsIgnoreCase(newStatus)) {
            booking.setStatus("Accepted");
        } else if ("Rejected".equalsIgnoreCase(newStatus)) {
            booking.setStatus("Rejected");
            booking.setRejectReason(reason);
        } else if ("Completed".equalsIgnoreCase(newStatus)) {
            booking.setStatus("Completed");
            if ("CASH".equalsIgnoreCase(booking.getPaymentMode())) {
                booking.setPaymentStatus("PAID"); // Cash collected upon completion
            }
        } else {
            throw new IllegalArgumentException("Invalid status update");
        }

        Booking updated = bookingRepository.save(booking);

        // Notify Customer of status update
        if (booking.getUserId() != null) {
            notificationRepository.save(new Notification(
                    booking.getUserId(),
                    "Booking #" + booking.getId() + " Status: " + updated.getStatus(),
                    "Your booking for " + booking.getServiceName() + " has been " + updated.getStatus() + (reason != null && !reason.isBlank() ? " (Reason: " + reason + ")" : "") + ".",
                    "STATUS"
            ));
        }

        return updated;
    }

    @Transactional
    public Booking cancelBookingByCustomer(Long bookingId, Long userId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized: You do not own this booking");
        }

        if ("Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Cannot cancel a completed booking");
        }

        booking.setStatus("Cancelled");
        booking.setCancellationReason(reason);
        Booking updated = bookingRepository.save(booking);

        // Notify Provider
        Provider provider = providerRepository.findById(booking.getProviderId()).orElse(null);
        if (provider != null && provider.getUserId() != null) {
            notificationRepository.save(new Notification(
                    provider.getUserId(),
                    "Booking #" + booking.getId() + " Cancelled",
                    "Customer " + booking.getUserName() + " has cancelled booking #" + booking.getId() + (reason != null ? ". Reason: " + reason : "."),
                    "STATUS"
            ));
        }

        return updated;
    }

    @Transactional
    public Review addReview(ReviewRequestDto dto, Long userId) {
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("You can only review your own bookings");
        }

        if (!"Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("You can only submit reviews for Completed bookings");
        }

        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new IllegalStateException("You have already reviewed this booking");
        }

        Review review = new Review();
        review.setBookingId(booking.getId());
        review.setCustomerId(userId);
        review.setCustomerName(booking.getUserName());
        review.setProviderId(booking.getProviderId());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        Review savedReview = reviewRepository.save(review);

        // Update Provider rating and total reviews
        Provider provider = providerRepository.findById(booking.getProviderId()).orElse(null);
        if (provider != null) {
            Double avg = reviewRepository.getAverageRatingForProvider(provider.getId());
            long count = reviewRepository.countByProviderId(provider.getId());
            if (avg != null) {
                provider.setRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            }
            provider.setTotalReviews((int) count);
            providerRepository.save(provider);
        }

        return savedReview;
    }
}
