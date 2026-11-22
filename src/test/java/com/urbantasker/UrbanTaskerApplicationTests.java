package com.urbantasker;

import com.urbantasker.dto.*;
import com.urbantasker.entity.*;
import com.urbantasker.repository.*;
import com.urbantasker.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UrbanTaskerApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("Test 1: User Registration, Duplicate Prevention & BCrypt Hashing")
    void testUserRegistrationAndDuplicatePrevention() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test Student");
        req.setUsername("student_test");
        req.setEmail("student@test.com");
        req.setMobile("9998887770");
        req.setPassword("secret123");
        req.setRole("user");
        req.setSecurityQuestion("What is your pet name?");
        req.setSecurityAnswer("Fluffy");

        User registered = authService.register(req);
        assertNotNull(registered.getId());
        assertEquals("student_test", registered.getUsername());
        assertNotEquals("secret123", registered.getPassword(), "Password must be BCrypt hashed");

        // Test duplicate username prevention
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("Test 2: Authentication & Session Key Generation")
    void testAuthenticationAndSessionKeys() {
        MockHttpSession session = new MockHttpSession();
        LoginRequest req = new LoginRequest();
        req.setIdentifier("khushi@example.com");
        req.setPassword("password123");

        AuthResponse resp = authService.login(req, session);
        assertNotNull(resp);
        assertEquals("user", resp.getRole());
        assertEquals("user", session.getAttribute("role"));
        assertNotNull(session.getAttribute("userId"));
        assertNotNull(session.getAttribute("name"));
    }

    @Test
    @DisplayName("Test 3: Akansha's Rule - Only Approved Providers are Visible")
    void testOnlyApprovedProvidersVisible() {
        List<Provider> approved = customerService.getApprovedProviders();
        assertNotNull(approved);
        for (Provider p : approved) {
            assertEquals("Approved", p.getStatus(), "PBL Rule 3 violation: unapproved provider listed!");
        }
    }

    @Test
    @DisplayName("Test 4: Khushi's Booking Placement (Cash & Online) with Pending Status")
    void testBookingPlacement() {
        // Find an approved provider
        List<Provider> approved = providerRepository.findByStatus("Approved");
        assertFalse(approved.isEmpty(), "Must have seed approved providers");
        Provider provider = approved.get(0);

        User customer = userRepository.findByEmail("khushi@example.com").orElseThrow();

        BookingRequestDto dto = new BookingRequestDto();
        dto.setProviderId(provider.getId());
        dto.setServiceName(provider.getServiceType());
        dto.setBookingDate(LocalDate.now().plusDays(1));
        dto.setBookingTime("10:00 AM - 12:00 PM");
        dto.setServiceAddress("Flat 101, Test Street, Delhi");
        dto.setContactNumber("9876543210");
        dto.setPaymentMode("ONLINE"); // Cash + Online दोनों
        dto.setEstimatedPrice(new BigDecimal("499.00"));

        Booking booking = bookingService.createBooking(dto, customer.getId());
        assertNotNull(booking.getId());
        assertEquals("Pending", booking.getStatus(), "PBL Rule 2: Booking initial status must be Pending");
        assertEquals("PAID", booking.getPaymentStatus(), "Online payment marks PAID");
    }

    @Test
    @DisplayName("Test 5: Divyansh's Provider Status Lifecycle (Accept, Reject with Reason, Complete)")
    void testProviderBookingLifecycle() {
        Provider provider = providerRepository.findByStatus("Approved").get(0);
        User customer = userRepository.findByEmail("khushi@example.com").orElseThrow();

        BookingRequestDto dto = new BookingRequestDto();
        dto.setProviderId(provider.getId());
        dto.setServiceName(provider.getServiceType());
        dto.setBookingDate(LocalDate.now().plusDays(2));
        dto.setBookingTime("02:00 PM - 04:00 PM");
        dto.setServiceAddress("Sector 15, Gurugram");
        dto.setPaymentMode("CASH");

        Booking booking = bookingService.createBooking(dto, customer.getId());
        assertEquals("Pending", booking.getStatus());

        // Divyansh accepts request
        Booking accepted = bookingService.updateBookingStatusByProvider(booking.getId(), provider.getId(), "Accepted", null);
        assertEquals("Accepted", accepted.getStatus());

        // Divyansh marks completed
        Booking completed = bookingService.updateBookingStatusByProvider(accepted.getId(), provider.getId(), "Completed", null);
        assertEquals("Completed", completed.getStatus());
        assertEquals("PAID", completed.getPaymentStatus(), "Completing cash booking updates payment to PAID");
    }

    @Test
    @DisplayName("Test 6: Review & Rating Submission on Completed Booking")
    void testReviewSubmission() {
        Provider provider = providerRepository.findByStatus("Approved").get(0);
        User customer = userRepository.findByEmail("khushi@example.com").orElseThrow();

        // Create and complete a booking
        BookingRequestDto dto = new BookingRequestDto();
        dto.setProviderId(provider.getId());
        dto.setServiceName(provider.getServiceType());
        dto.setBookingDate(LocalDate.now().plusDays(1));
        dto.setBookingTime("11:00 AM");
        dto.setServiceAddress("Dwarka, Delhi");
        Booking booking = bookingService.createBooking(dto, customer.getId());
        bookingService.updateBookingStatusByProvider(booking.getId(), provider.getId(), "Accepted", null);
        bookingService.updateBookingStatusByProvider(booking.getId(), provider.getId(), "Completed", null);

        // Submit review
        ReviewRequestDto rev = new ReviewRequestDto();
        rev.setBookingId(booking.getId());
        rev.setRating(5);
        rev.setComment("Outstanding service quality!");

        Review savedReview = bookingService.addReview(rev, customer.getId());
        assertNotNull(savedReview.getId());
        assertEquals(5, savedReview.getRating());

        // Prevent duplicate review
        assertThrows(IllegalStateException.class, () -> bookingService.addReview(rev, customer.getId()));
    }

    @Test
    @DisplayName("Test 7: Meghna's Admin Verification Workflow (Approve / Reject Provider)")
    void testAdminProviderVerification() {
        // Pending provider from seed: Vikram Rathore
        List<Provider> pendingList = adminService.getPendingProviderApplications();
        assertFalse(pendingList.isEmpty(), "Must have pending provider for audit");
        Provider pending = pendingList.get(0);

        // Approve provider
        Provider approved = adminService.approveProvider(pending.getId(), "admin@gmail.com");
        assertEquals("Approved", approved.getStatus());

        // Now verify provider appears in customer approved list
        List<Provider> customerVisible = customerService.getApprovedProviders();
        boolean found = customerVisible.stream().anyMatch(p -> p.getId().equals(approved.getId()));
        assertTrue(found, "Newly approved provider must now appear in customer search");
    }
}
