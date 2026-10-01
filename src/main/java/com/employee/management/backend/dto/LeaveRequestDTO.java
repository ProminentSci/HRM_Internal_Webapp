package com.employee.management.backend.dto;

public class LeaveRequestDTO {
    private Long id;
    private Long empId;
    private String employeeName;
    private String leaveType;
    private Integer days;
    private Integer year;
    private String createdAt;
    private String fromDate;
    private String toDate;
    private String status;
    private String reason;

    public LeaveRequestDTO() {
    }

    public LeaveRequestDTO(Long id, Long empId, String employeeName, String leaveType,
                          Integer days, Integer year, String createdAt, String fromDate,
                          String toDate, String status, String reason) {
        this.id = id;
        this.empId = empId;
        this.employeeName = employeeName;
        this.leaveType = leaveType;
        this.days = days;
        this.year = year;
        this.createdAt = createdAt;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.status = status;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
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
}
