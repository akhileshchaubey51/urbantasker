package com.urbantasker.dto;

import java.math.BigDecimal;
import java.util.Map;

public class DashboardStatsDto {
    // Admin / Platform stats
    private long totalCustomers;
    private long totalProviders;
    private long pendingProviders;
    private long approvedProviders;
    private long rejectedProviders;
    private long totalBookings;
    private long pendingBookings;
    private long acceptedBookings;
    private long completedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue;

    // Provider specific stats
    private BigDecimal providerEarnings;
    private long providerRequests;

    private Map<String, Long> categoryDistribution;

    public DashboardStatsDto() {}

    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

    public long getTotalProviders() { return totalProviders; }
    public void setTotalProviders(long totalProviders) { this.totalProviders = totalProviders; }

    public long getPendingProviders() { return pendingProviders; }
    public void setPendingProviders(long pendingProviders) { this.pendingProviders = pendingProviders; }

    public long getApprovedProviders() { return approvedProviders; }
    public void setApprovedProviders(long approvedProviders) { this.approvedProviders = approvedProviders; }

    public long getRejectedProviders() { return rejectedProviders; }
    public void setRejectedProviders(long rejectedProviders) { this.rejectedProviders = rejectedProviders; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getPendingBookings() { return pendingBookings; }
    public void setPendingBookings(long pendingBookings) { this.pendingBookings = pendingBookings; }

    public long getAcceptedBookings() { return acceptedBookings; }
    public void setAcceptedBookings(long acceptedBookings) { this.acceptedBookings = acceptedBookings; }

    public long getCompletedBookings() { return completedBookings; }
    public void setCompletedBookings(long completedBookings) { this.completedBookings = completedBookings; }

    public long getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(long cancelledBookings) { this.cancelledBookings = cancelledBookings; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getProviderEarnings() { return providerEarnings; }
    public void setProviderEarnings(BigDecimal providerEarnings) { this.providerEarnings = providerEarnings; }

    public long getProviderRequests() { return providerRequests; }
    public void setProviderRequests(long providerRequests) { this.providerRequests = providerRequests; }

    public Map<String, Long> getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(Map<String, Long> categoryDistribution) { this.categoryDistribution = categoryDistribution; }
}
