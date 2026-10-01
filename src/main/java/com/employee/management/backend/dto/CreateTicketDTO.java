package com.employee.management.backend.dto;

public class CreateTicketDTO {
    private String subject;
    private String description;

    public CreateTicketDTO() {
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
