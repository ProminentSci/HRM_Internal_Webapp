package com.employee.management.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateLeaveRequestDTO {
    @JsonProperty("employeeId")
    private Long empId;
    
    @JsonProperty("type")
    private String leaveType;
    
    private String fromDate;
    private String toDate;
    private Integer days;
    private Integer year;
    private String reason;

    public CreateLeaveRequestDTO() {
    }

    public CreateLeaveRequestDTO(Long empId, String leaveType, String fromDate,
                                String toDate, Integer days, Integer year, String reason) {
        this.empId = empId;
        this.leaveType = leaveType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.days = days;
        this.year = year;
        this.reason = reason;
    }

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
