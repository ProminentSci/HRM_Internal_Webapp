package com.employee.management.backend.dto;

import java.time.LocalDate;

public class EmployeeLetterResponseDTO {
    private String employeeId;
    private String employeeFullName;
    private String designation;
    private LocalDate joiningDate;
    private LocalDate relievingDate;

    public EmployeeLetterResponseDTO() {
    }

    public EmployeeLetterResponseDTO(String employeeId, String employeeFullName, String designation,
            LocalDate joiningDate, LocalDate relievingDate) {
        this.employeeId = employeeId;
        this.employeeFullName = employeeFullName;
        this.designation = designation;
        this.joiningDate = joiningDate;
        this.relievingDate = relievingDate;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeFullName() {
        return employeeFullName;
    }

    public void setEmployeeFullName(String employeeFullName) {
        this.employeeFullName = employeeFullName;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public LocalDate getRelievingDate() {
        return relievingDate;
    }

    public void setRelievingDate(LocalDate relievingDate) {
        this.relievingDate = relievingDate;
    }
}
