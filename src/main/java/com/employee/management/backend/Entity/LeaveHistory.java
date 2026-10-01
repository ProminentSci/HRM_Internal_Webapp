package com.employee.management.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "leave_history")
public class LeaveHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @JsonIgnore
    private Employee employee;
    
    @Column(nullable = false)
    private String leaveType;
    
    @Column(nullable = false)
    private Integer daysUsed;
    
    @Column(nullable = false)
    private Integer year;
    
    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    public LeaveHistory() {
    }

    public LeaveHistory(Employee employee, String leaveType, Integer daysUsed, Integer year) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.daysUsed = daysUsed;
        this.year = year;
        this.createdDate = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public Integer getDaysUsed() {
        return daysUsed;
    }

    public void setDaysUsed(Integer daysUsed) {
        this.daysUsed = daysUsed;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }
}
