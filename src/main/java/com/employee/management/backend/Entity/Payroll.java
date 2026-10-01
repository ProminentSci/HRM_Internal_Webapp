package com.employee.management.backend.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "payroll", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "payroll_month", "payroll_year"})
})
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "date_of_joining")
    private String dateOfJoining;

    @Column(name = "lop")
    private Integer lop;

    @Column(name = "salary")
    private Double salary;

    @Column(name = "variable_pay")
    private Double variablePay;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc")
    private String ifsc;

    @Column(name = "uan")
    private String uan;

    @Column(name = "pf")
    private String pf;

    @Column(name = "credit_status")
    private String creditStatus;

    @Column(name = "payroll_month", nullable = false)
    private Integer month;

    @Column(name = "payroll_year", nullable = false)
    private Integer year;

    @Column(name = "manual_payslip", nullable = false)
    private boolean manualPayslip;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "payslip_data", columnDefinition = "LONGBLOB")
    private byte[] payslipData;

    @Column(name = "payslip_file_name")
    private String payslipFileName;

    @Column(name = "payslip_content_type")
    private String payslipContentType;

    public Payroll() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(String dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public Integer getLop() {
        return lop;
    }

    public void setLop(Integer lop) {
        this.lop = lop;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public Double getVariablePay() {
        return variablePay;
    }

    public void setVariablePay(Double variablePay) {
        this.variablePay = variablePay;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfsc() {
        return ifsc;
    }

    public void setIfsc(String ifsc) {
        this.ifsc = ifsc;
    }

    public String getUan() {
        return uan;
    }

    public void setUan(String uan) {
        this.uan = uan;
    }

    public String getPf() {
        return pf;
    }

    public void setPf(String pf) {
        this.pf = pf;
    }

    public String getCreditStatus() {
        return creditStatus;
    }

    public void setCreditStatus(String creditStatus) {
        this.creditStatus = creditStatus;
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

    public boolean isManualPayslip() {
        return manualPayslip;
    }

    public void setManualPayslip(boolean manualPayslip) {
        this.manualPayslip = manualPayslip;
    }

    public byte[] getPayslipData() {
        return payslipData;
    }

    public void setPayslipData(byte[] payslipData) {
        this.payslipData = payslipData;
    }

    public String getPayslipFileName() {
        return payslipFileName;
    }

    public void setPayslipFileName(String payslipFileName) {
        this.payslipFileName = payslipFileName;
    }

    public String getPayslipContentType() {
        return payslipContentType;
    }

    public void setPayslipContentType(String payslipContentType) {
        this.payslipContentType = payslipContentType;
    }
}
