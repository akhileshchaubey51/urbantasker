package com.urbantasker.repository;

import com.urbantasker.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Booking> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    List<Booking> findByProviderIdAndStatus(Long providerId, String status);

    List<Booking> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    long countByProviderId(Long providerId);

    long countByProviderIdAndStatus(Long providerId, String status);

    @Query("SELECT COALESCE(SUM(b.estimatedPrice), 0) FROM Booking b WHERE b.providerId = :providerId AND b.status = 'Completed'")
    BigDecimal calculateProviderEarnings(Long providerId);

    @Query("SELECT COALESCE(SUM(b.estimatedPrice), 0) FROM Booking b WHERE b.status = 'Completed' AND b.paymentStatus = 'PAID'")
    BigDecimal calculatePlatformRevenue();
}
