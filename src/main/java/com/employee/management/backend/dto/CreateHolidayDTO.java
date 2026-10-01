package com.employee.management.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateHolidayDTO {

    @NotNull(message = "date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be 255 characters or less")
    private String title;

    @Size(max = 1000, message = "description must be 1000 characters or less")
    private String description;

    public CreateHolidayDTO() {
    }

    public CreateHolidayDTO(LocalDate date, String title, String description) {
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
