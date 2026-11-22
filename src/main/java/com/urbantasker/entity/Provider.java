package com.urbantasker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Column(name = "service_details", length = 500)
    private String serviceDetails;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city = "New Delhi";

    @Column(name = "experience_years")
    private Integer experienceYears = 1;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate = new BigDecimal("350.00");

    @Column(precision = 2, scale = 1)
    private BigDecimal rating = new BigDecimal("4.8");

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "photo_file")
    private String photoFile;

    @Column(name = "aadhaar_file")
    private String aadhaarFile;

    @Column(name = "police_file")
    private String policeFile;

    @Column(name = "address_proof_file")
    private String addressProofFile;

    @Column(length = 20)
    private String status = "Pending"; // 'Pending', 'Approved', 'Rejected', 'Suspended'

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Provider() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getServiceDetails() { return serviceDetails; }
    public void setServiceDetails(String serviceDetails) { this.serviceDetails = serviceDetails; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getPhotoFile() { return photoFile; }
    public void setPhotoFile(String photoFile) { this.photoFile = photoFile; }

    public String getAadhaarFile() { return aadhaarFile; }
    public void setAadhaarFile(String aadhaarFile) { this.aadhaarFile = aadhaarFile; }

    public String getPoliceFile() { return policeFile; }
    public void setPoliceFile(String policeFile) { this.policeFile = policeFile; }

    public String getAddressProofFile() { return addressProofFile; }
    public void setAddressProofFile(String addressProofFile) { this.addressProofFile = addressProofFile; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
