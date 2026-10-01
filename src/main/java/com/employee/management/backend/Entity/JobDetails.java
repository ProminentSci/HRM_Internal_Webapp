package com.employee.management.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "job_details")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class JobDetails {
    @Id
    private Long empId;

    private String department;
    private String designation;
    private String employeeType;
    private String dateOfJoining;
    private String workLocation;
    private String reportingManager;
    private String employeeStatus;
    private String shiftTiming;
    private String experience;
    private String employeeCategory;

    // "Bench" or "Project" - whether the employee is currently allocated to a client project.
    private String workStatus;
    private String currentProjectName;
    private String currentProjectManager;
    private String currentProjectStartDate;

    // "EMPLOYEE" (default), "TEAM_LEAD", or "PROJECT_MANAGER" - who this person is allowed to be
    // assigned as when building a Project's PM/TL hierarchy. Independent of the "Bench"/"Project"
    // workStatus above and of the legacy free-text reportingManager/currentProjectManager fields.
    private String positionLevel;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "emp_id")
    @JsonIgnore
    private Employee employee;

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getEmployeeType() {
        return employeeType;
    }

    public void setEmployeeType(String employeeType) {
        this.employeeType = employeeType;
    }

    public String getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(String dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public String getWorkLocation() {
        return workLocation;
    }

    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }

    public String getReportingManager() {
        return reportingManager;
    }

    public void setReportingManager(String reportingManager) {
        this.reportingManager = reportingManager;
    }

    public String getEmployeeStatus() {
        return employeeStatus;
    }

    public void setEmployeeStatus(String employeeStatus) {
        this.employeeStatus = employeeStatus;
    }

    public String getShiftTiming() {
        return shiftTiming;
    }

    public void setShiftTiming(String shiftTiming) {
        this.shiftTiming = shiftTiming;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getEmployeeCategory() {
        return employeeCategory;
    }

    public void setEmployeeCategory(String employeeCategory) {
        this.employeeCategory = employeeCategory;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(String workStatus) {
        this.workStatus = workStatus;
    }

    public String getCurrentProjectName() {
        return currentProjectName;
    }

    public void setCurrentProjectName(String currentProjectName) {
        this.currentProjectName = currentProjectName;
    }

    public String getCurrentProjectManager() {
        return currentProjectManager;
    }

    public void setCurrentProjectManager(String currentProjectManager) {
        this.currentProjectManager = currentProjectManager;
    }

    public String getCurrentProjectStartDate() {
        return currentProjectStartDate;
    }

    public void setCurrentProjectStartDate(String currentProjectStartDate) {
        this.currentProjectStartDate = currentProjectStartDate;
    }

    public String getPositionLevel() {
        return positionLevel;
    }

    public void setPositionLevel(String positionLevel) {
        this.positionLevel = positionLevel;
    }
}
