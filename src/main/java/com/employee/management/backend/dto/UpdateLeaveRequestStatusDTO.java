package com.employee.management.backend.dto;

public class UpdateLeaveRequestStatusDTO {
    private String status;
    private String reason;
    private Integer days;

    public UpdateLeaveRequestStatusDTO() {
    }

    public UpdateLeaveRequestStatusDTO(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public UpdateLeaveRequestStatusDTO(String status, String reason, Integer days) {
        this.status = status;
        this.reason = reason;
        this.days = days;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }
}
