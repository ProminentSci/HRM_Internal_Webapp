package com.employee.management.backend.dto;

public class UpdateTicketStatusDTO {
    private String status;
    private String adminResponse;

    public UpdateTicketStatusDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }
}
