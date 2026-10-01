package com.employee.management.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "education_details")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EducationDetails {
    @Id
    private Long empId;

    private String highestQualification;
    private String universityCollege;
    private String yearOfPassing;
    private String percentageCGPA;

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

    public String getHighestQualification() {
        return highestQualification;
    }

    public void setHighestQualification(String highestQualification) {
        this.highestQualification = highestQualification;
    }

    public String getUniversityCollege() {
        return universityCollege;
    }

    public void setUniversityCollege(String universityCollege) {
        this.universityCollege = universityCollege;
    }

    public String getYearOfPassing() {
        return yearOfPassing;
    }

    public void setYearOfPassing(String yearOfPassing) {
        this.yearOfPassing = yearOfPassing;
    }

    public String getPercentageCGPA() {
        return percentageCGPA;
    }

    public void setPercentageCGPA(String percentageCGPA) {
        this.percentageCGPA = percentageCGPA;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}
