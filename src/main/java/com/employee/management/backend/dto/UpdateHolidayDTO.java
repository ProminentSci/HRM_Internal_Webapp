package com.employee.management.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateHolidayDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Size(max = 255, message = "title must be 255 characters or less")
    private String title;

    @Size(max = 1000, message = "description must be 1000 characters or less")
    private String description;

    public UpdateHolidayDTO() {
    }

    public UpdateHolidayDTO(LocalDate date, String title, String description) {
        this.date = date;
        this.title = title;
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
