package com.employee.management.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class PayrollProcessRequestDTO {
    private List<PayrollEmployeeRequestDTO> employees = new ArrayList<>();
    private Integer month;
    private Integer year;

    public PayrollProcessRequestDTO() {
    }

    public List<PayrollEmployeeRequestDTO> getEmployees() {
        return employees;
    }

    public void setEmployees(List<PayrollEmployeeRequestDTO> employees) {
        this.employees = employees;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
