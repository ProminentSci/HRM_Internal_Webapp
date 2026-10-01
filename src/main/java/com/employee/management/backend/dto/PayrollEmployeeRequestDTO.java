package com.employee.management.backend.dto;

public class PayrollEmployeeRequestDTO {
    private Long employeeId;
    private String employeeName;
    private Double variablePay;

    public PayrollEmployeeRequestDTO() {
    }

    public PayrollEmployeeRequestDTO(Long employeeId, String employeeName) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    // One-off top-up entered by the admin for this specific payroll run - added on top of the
    // computed net salary, not prorated against LOP like the recurring basic/bonus components.
    public Double getVariablePay() {
        return variablePay;
    }

    public void setVariablePay(Double variablePay) {
        this.variablePay = variablePay;
    }
}
