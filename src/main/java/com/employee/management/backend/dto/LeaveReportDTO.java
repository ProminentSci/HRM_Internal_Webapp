package com.employee.management.backend.dto;

import java.util.HashMap;
import java.util.Map;

public class LeaveReportDTO {
    private Long empId;
    private String employeeName;
    private Map<String, Integer> remainingLeaves;
    private Integer totalRemainingDays;

    public LeaveReportDTO() {
        this.remainingLeaves = new HashMap<>();
    }

    public LeaveReportDTO(Long empId, String employeeName) {
        this.empId = empId;
        this.employeeName = employeeName;
        this.remainingLeaves = new HashMap<>();
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

    public Map<String, Integer> getRemainingLeaves() {
        return remainingLeaves;
    }

    public void setRemainingLeaves(Map<String, Integer> remainingLeaves) {
        this.remainingLeaves = remainingLeaves;
    }

    public void addLeaveType(String leaveType, Integer days) {
        this.remainingLeaves.put(leaveType, days);
    }

    public Integer getTotalRemainingDays() {
        return remainingLeaves.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void setTotalRemainingDays(Integer totalRemainingDays) {
        this.totalRemainingDays = totalRemainingDays;
    }
}
