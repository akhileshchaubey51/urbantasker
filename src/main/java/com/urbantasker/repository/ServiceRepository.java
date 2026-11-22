package com.urbantasker.repository;

import com.urbantasker.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByIsActiveTrue();
    Optional<ServiceEntity> findByServiceName(String serviceName);
    boolean existsByServiceName(String serviceName);
    List<ServiceEntity> findByCategory(String category);
}
