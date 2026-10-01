package com.employee.management.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// A Project Manager's weekly rollup of their team's daily timesheet entries, sent to HR once a
// week instead of HR seeing every project employee's daily update individually (bench employees'
// updates skip this entirely and go straight to HR/admin each day - see Timesheet.submittedTo).
@Entity
@Table(name = "weekly_reports")
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_emp_id", nullable = false)
    @JsonIgnore
    private Employee manager;

    private LocalDate weekStartDate;
    private LocalDate weekEndDate;

    @Column(length = 2000)
    private String notes;

    private LocalDate submittedAt;

    @OneToMany(mappedBy = "weeklyReport", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Timesheet> entries = new ArrayList<>();

    public WeeklyReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDate submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<Timesheet> getEntries() {
        return entries;
    }

    public void setEntries(List<Timesheet> entries) {
        this.entries = entries;
    }
}
