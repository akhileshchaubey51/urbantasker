package com.urbantasker.dto;

public class StatusUpdateDto {
    private String status;
    private String reason;

    public StatusUpdateDto() {}

    public StatusUpdateDto(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
