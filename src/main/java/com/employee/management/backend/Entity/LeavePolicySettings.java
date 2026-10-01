package com.employee.management.backend.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_policy_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"client_id"})
})
public class LeavePolicySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "casual_leave_allocation", nullable = false)
    private Integer casualLeaveAllocation = 12;

    @Column(name = "sick_leave_allocation", nullable = false)
    private Integer sickLeaveAllocation = 8;

    @Column(name = "paid_leave_allocation", nullable = false)
    private Integer paidLeaveAllocation = 20;

    public LeavePolicySettings() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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
