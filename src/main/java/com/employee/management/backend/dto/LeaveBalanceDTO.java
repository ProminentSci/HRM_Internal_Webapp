package com.employee.management.backend.dto;

public class LeaveBalanceDTO {
    private Long id;
    private Long empId;
    private String leaveType;
    private Integer balance;

    public LeaveBalanceDTO() {
    }

    public LeaveBalanceDTO(Long id, Long empId, String leaveType, Integer balance) {
        this.id = id;
        this.empId = empId;
        this.leaveType = leaveType;
        this.balance = balance;
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

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }
}
