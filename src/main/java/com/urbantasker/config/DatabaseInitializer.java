package com.urbantasker.config;

import com.urbantasker.entity.*;
import com.urbantasker.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Automatic Database Initializer for SQLite
 * Populates seed data (Admin, Customers, Approved Providers, Services, Bookings, Reviews)
 * automatically on first run when urbantasker.db is created.
 */
@Configuration
public class DatabaseInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final AdminLogRepository adminLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(AdminRepository adminRepository,
                               UserRepository userRepository,
                               ProviderRepository providerRepository,
                               ServiceRepository serviceRepository,
                               BookingRepository bookingRepository,
                               ReviewRepository reviewRepository,
                               NotificationRepository notificationRepository,
                               AdminLogRepository adminLogRepository,
                               PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
        this.adminLogRepository = adminLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initAdmin();
        initServices();
        initUsersAndProviders();
        initBookingsAndReviews();
    }

    private void initAdmin() {
        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("admin");
            adminRepository.save(admin);

            adminLogRepository.save(new AdminLog(
                    "admin@gmail.com",
                    "SYSTEM_INIT",
                    "UrbanTasker SQLite database initialized successfully.",
                    "127.0.0.1"
            ));
        }
    }

    private void initServices() {
        if (serviceRepository.count() == 0) {
            serviceRepository.saveAll(List.of(
                    new ServiceEntity("Electrician Services", "Electrical", "Expert electrical repair, wiring, fan/light installations & circuit maintenance.", new BigDecimal("299.00"), "/images/services/electrician.jpg"),
                    new ServiceEntity("Plumbing & Pipe Repair", "Plumbing", "Complete leak detection, pipe fixing, tap & sink repair, bathroom fittings.", new BigDecimal("349.00"), "/images/services/plumber.jpg"),
                    new ServiceEntity("Deep Home Cleaning", "Cleaning", "Thorough sanitation of living rooms, bedrooms, kitchens, and washrooms.", new BigDecimal("899.00"), "/images/services/cleaning.jpg"),
                    new ServiceEntity("AC Repair & Gas Refill", "Appliance", "Comprehensive split/window AC servicing, jet pump cleaning, refrigerant refill.", new BigDecimal("599.00"), "/images/services/ac_repair.jpg"),
                    new ServiceEntity("Carpentry & Woodwork", "Carpentry", "Furniture assembly, hinge repairs, custom shelf installations, door fixing.", new BigDecimal("399.00"), "/images/services/carpentry.jpg"),
                    new ServiceEntity("Interior Home Painting", "Painting", "Premium wall painting, waterproof primer coats, texture designs & touch-ups.", new BigDecimal("1499.00"), "/images/services/painting.jpg"),
                    new ServiceEntity("Appliance Repair", "Appliance", "Washing machines, microwave ovens, refrigerators, RO water purifiers repair.", new BigDecimal("449.00"), "/images/services/appliance.jpg"),
                    new ServiceEntity("Salon & Spa at Home", "Wellness", "Professional haircuts, grooming, facial care, manicures, and relaxing massages.", new BigDecimal("699.00"), "/images/services/salon.jpg")
            ));
        }
    }

    private void initUsersAndProviders() {
        if (userRepository.count() == 0) {
            String defaultPassword = passwordEncoder.encode("password123");

            // Seed Customers
            User khushi = new User("Khushi Verma", "khushi_v", "khushi@example.com", "9876543210", defaultPassword, "user");
            khushi.setSecurityQuestion("What is your pet name?");
            khushi.setSecurityAnswer("buddy");
            khushi.setAddress("Flat 402, Green Glen Apartments, Sector 14, Gurugram");
            userRepository.save(khushi);

            User akansha = new User("Akansha Sharma", "akansha_s", "akansha@example.com", "9876543211", defaultPassword, "user");
            akansha.setSecurityQuestion("What city were you born in?");
            akansha.setSecurityAnswer("jaipur");
            akansha.setAddress("B-12, Malviya Nagar, South Delhi");
            userRepository.save(akansha);

            // Seed Providers
            User divyanshUser = new User("Divyansh Singh", "divyansh_tech", "divyansh@example.com", "9876543212", defaultPassword, "provider");
            divyanshUser.setSecurityQuestion("What is your favorite food?");
            divyanshUser.setSecurityAnswer("paneer");
            divyanshUser.setAddress("Shop 7, Main Market, Lajpat Nagar, New Delhi");
            userRepository.save(divyanshUser);

            Provider divyansh = new Provider();
            divyansh.setUserId(divyanshUser.getId());
            divyansh.setProviderName("Divyansh Singh");
            divyansh.setServiceType("AC Repair & Gas Refill");
            divyansh.setServiceDetails("Certified HVAC technician with 5+ years servicing LG, Daikin, Voltas & Samsung AC units.");
            divyansh.setAddress("Shop 7, Main Market, Lajpat Nagar");
            divyansh.setCity("New Delhi");
            divyansh.setExperienceYears(5);
            divyansh.setHourlyRate(new BigDecimal("499.00"));
            divyansh.setRating(new BigDecimal("4.9"));
            divyansh.setTotalReviews(28);
            divyansh.setStatus("Approved");
            providerRepository.save(divyansh);

            User rajeshUser = new User("Rajesh Kumar", "rajesh_elec", "rajesh@example.com", "9876543213", defaultPassword, "provider");
            rajeshUser.setAddress("Sector 22, Noida");
            userRepository.save(rajeshUser);

            Provider rajesh = new Provider();
            rajesh.setUserId(rajeshUser.getId());
            rajesh.setProviderName("Rajesh Kumar");
            rajesh.setServiceType("Electrician Services");
            rajesh.setServiceDetails("Licensed master electrician specialized in wiring, MCB tripping issues, and inverter setups.");
            rajesh.setAddress("Sector 22, Noida");
            rajesh.setCity("Noida");
            rajesh.setExperienceYears(7);
            rajesh.setHourlyRate(new BigDecimal("349.00"));
            rajesh.setRating(new BigDecimal("4.8"));
            rajesh.setTotalReviews(42);
            rajesh.setStatus("Approved");
            providerRepository.save(rajesh);

            User sureshUser = new User("Suresh Mehta", "suresh_plumb", "suresh@example.com", "9876543214", defaultPassword, "provider");
            userRepository.save(sureshUser);

            Provider suresh = new Provider();
            suresh.setUserId(sureshUser.getId());
            suresh.setProviderName("Suresh Mehta");
            suresh.setServiceType("Plumbing & Pipe Repair");
            suresh.setServiceDetails("Expert plumber resolving stubborn blockages, high-pressure pipeline fittings, and sanitary installations.");
            suresh.setAddress("Indirapuram, Ghaziabad");
            suresh.setCity("Ghaziabad");
            suresh.setExperienceYears(6);
            suresh.setHourlyRate(new BigDecimal("399.00"));
            suresh.setRating(new BigDecimal("4.7"));
            suresh.setTotalReviews(35);
            suresh.setStatus("Approved");
            providerRepository.save(suresh);

            User poojaUser = new User("Pooja Roy", "pooja_clean", "pooja@example.com", "9876543215", defaultPassword, "provider");
            userRepository.save(poojaUser);

            Provider pooja = new Provider();
            pooja.setUserId(poojaUser.getId());
            pooja.setProviderName("Pooja Roy");
            pooja.setServiceType("Deep Home Cleaning");
            pooja.setServiceDetails("Professional home hygiene specialist. Eco-friendly cleaning chemicals and industrial steam vacuums used.");
            pooja.setAddress("Rohini Sector 9, Delhi");
            pooja.setCity("New Delhi");
            pooja.setExperienceYears(4);
            pooja.setHourlyRate(new BigDecimal("799.00"));
            pooja.setRating(new BigDecimal("4.9"));
            pooja.setTotalReviews(19);
            pooja.setStatus("Approved");
            providerRepository.save(pooja);

            // Pending provider for Admin verification testing
            User vikramUser = new User("Vikram Rathore", "vikram_pend", "vikram@example.com", "9876543216", defaultPassword, "provider");
            userRepository.save(vikramUser);

            Provider vikram = new Provider();
            vikram.setUserId(vikramUser.getId());
            vikram.setProviderName("Vikram Rathore");
            vikram.setServiceType("Carpentry & Woodwork");
            vikram.setServiceDetails("Custom furniture crafting, modular kitchen hinge adjustments, wooden door repair.");
            vikram.setAddress("Dwarka Sector 11, Delhi");
            vikram.setCity("New Delhi");
            vikram.setExperienceYears(3);
            vikram.setHourlyRate(new BigDecimal("350.00"));
            vikram.setStatus("Pending"); // Seed pending for Meghna's admin approval test
            providerRepository.save(vikram);
        }
    }

    private void initBookingsAndReviews() {
        if (bookingRepository.count() == 0) {
            Provider divyansh = providerRepository.findByStatus("Approved").get(0);
            User khushi = userRepository.findByEmail("khushi@example.com").orElse(null);

            if (khushi != null && divyansh != null) {
                // Completed booking
                Booking b1 = new Booking();
                b1.setUserId(khushi.getId());
                b1.setUserName(khushi.getName());
                b1.setProviderId(divyansh.getId());
                b1.setProviderName(divyansh.getProviderName());
                b1.setServiceName(divyansh.getServiceType());
                b1.setBookingDate(LocalDate.now().minusDays(2));
                b1.setBookingTime("10:00 AM - 12:00 PM");
                b1.setServiceAddress("Flat 402, Green Glen Apartments, Gurugram");
                b1.setContactNumber("9876543210");
                b1.setEstimatedPrice(new BigDecimal("599.00"));
                b1.setPaymentMode("ONLINE");
                b1.setPaymentStatus("PAID");
                b1.setStatus("Completed");
                bookingRepository.save(b1);

                // Review for completed booking
                Review r1 = new Review(b1.getId(), khushi.getId(), khushi.getName(), divyansh.getId(), 5, "Divyansh arrived on time, was polite and quickly repaired the cooling coil. Very professional work!");
                reviewRepository.save(r1);

                // Accepted booking
                Booking b2 = new Booking();
                b2.setUserId(khushi.getId());
                b2.setUserName(khushi.getName());
                b2.setProviderId(divyansh.getId());
                b2.setProviderName(divyansh.getProviderName());
                b2.setServiceName(divyansh.getServiceType());
                b2.setBookingDate(LocalDate.now().plusDays(1));
                b2.setBookingTime("02:00 PM - 04:00 PM");
                b2.setServiceAddress("Flat 402, Green Glen Apartments, Gurugram");
                b2.setContactNumber("9876543210");
                b2.setEstimatedPrice(new BigDecimal("499.00"));
                b2.setPaymentMode("CASH");
                b2.setPaymentStatus("PENDING");
                b2.setStatus("Accepted");
                bookingRepository.save(b2);

                // Notifications
                notificationRepository.save(new Notification(
                        khushi.getId(),
                        "Booking Confirmed",
                        "Your AC Repair service was marked completed by Divyansh Singh.",
                        "BOOKING"
                ));
            }
        }
    }
}
