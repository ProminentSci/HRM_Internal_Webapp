package com.employee.management.backend.dto;

public class PerformanceReportDTO {
    private Long id;
    private Long empId;
    private String employeeName;
    private Long managerEmpId;
    private String managerName;
    private Integer month;
    private Integer year;
    private Integer rating;
    private String comments;
    private String submittedAt;
    private String updatedAt;

    public PerformanceReportDTO() {
    }

    public PerformanceReportDTO(Long id, Long empId, String employeeName, Long managerEmpId, String managerName,
                                 Integer month, Integer year, Integer rating, String comments,
                                 String submittedAt, String updatedAt) {
        this.id = id;
        this.empId = empId;
        this.employeeName = employeeName;
        this.managerEmpId = managerEmpId;
        this.managerName = managerName;
        this.month = month;
        this.year = year;
        this.rating = rating;
        this.comments = comments;
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
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

    public Long getManagerEmpId() {
        return managerEmpId;
    }

    public void setManagerEmpId(Long managerEmpId) {
        this.managerEmpId = managerEmpId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
