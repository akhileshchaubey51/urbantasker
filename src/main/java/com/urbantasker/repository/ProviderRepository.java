package com.urbantasker.repository;

import com.urbantasker.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    
    // Rule 3: Only approved providers show to customers. Akansha's list must use WHERE status='Approved'
    List<Provider> findByStatus(String status);
    
    Optional<Provider> findByUserId(Long userId);

    long countByStatus(String status);

    @Query("SELECT p FROM Provider p WHERE p.status = 'Approved' " +
           "AND (:serviceType IS NULL OR LOWER(p.serviceType) LIKE LOWER(CONCAT('%', :serviceType, '%'))) " +
           "AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))")
    List<Provider> searchApprovedProviders(@Param("serviceType") String serviceType, 
                                          @Param("city") String city);
}
