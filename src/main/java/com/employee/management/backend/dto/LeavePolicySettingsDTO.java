package com.employee.management.backend.dto;

public class LeavePolicySettingsDTO {
    private Integer casualLeaveAllocation;
    private Integer sickLeaveAllocation;
    private Integer paidLeaveAllocation;

    public LeavePolicySettingsDTO() {
    }

    public LeavePolicySettingsDTO(Integer casualLeaveAllocation, Integer sickLeaveAllocation, Integer paidLeaveAllocation) {
        this.casualLeaveAllocation = casualLeaveAllocation;
        this.sickLeaveAllocation = sickLeaveAllocation;
        this.paidLeaveAllocation = paidLeaveAllocation;
    }

    public Integer getCasualLeaveAllocation() {
        return casualLeaveAllocation;
    }

    public void setCasualLeaveAllocation(Integer casualLeaveAllocation) {
        this.casualLeaveAllocation = casualLeaveAllocation;
    }

    public Integer getSickLeaveAllocation() {
        return sickLeaveAllocation;
    }

    public void setSickLeaveAllocation(Integer sickLeaveAllocation) {
        this.sickLeaveAllocation = sickLeaveAllocation;
    }

    public Integer getPaidLeaveAllocation() {
        return paidLeaveAllocation;
    }

    public void setPaidLeaveAllocation(Integer paidLeaveAllocation) {
        this.paidLeaveAllocation = paidLeaveAllocation;
    }
}
