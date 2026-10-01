package com.employee.management.backend.dto;

public class PayrollEmployeeResponseDTO {
    private Long payrollId;
    private Long employeeId;
    private String employeeName;
    private String requestedEmployeeName;
    private String dateOfJoining;
    private String status;
    private String message;
    private Integer lop;
    private Double salary;
    private String panNumber;
    private String accountNumber;
    private String ifsc;
    private String uan;
    private String pf;
    private String creditStatus;
    private Integer month;
    private Integer year;
    private Double basicSalary;
    private Double bonus;
    private Double ctc;
    private Double monthlyGrossSalary;
    private Integer paidLeaveDays;
    private Integer unpaidLeaveDays;
    private Double leaveDeduction;
    private Double variablePay;
    private Double netSalary;
    private Boolean manualPayslip;
    private Boolean hasPayslipFile;

    public PayrollEmployeeResponseDTO() {
    }

    public Long getPayrollId() {
        return payrollId;
    }

    public void setPayrollId(Long payrollId) {
        this.payrollId = payrollId;
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

    public String getRequestedEmployeeName() {
        return requestedEmployeeName;
    }

    public void setRequestedEmployeeName(String requestedEmployeeName) {
        this.requestedEmployeeName = requestedEmployeeName;
    }

    public String getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(String dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public Double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public Double getBonus() {
        return bonus;
    }

    public void setBonus(Double bonus) {
        this.bonus = bonus;
    }

    public Double getCtc() {
        return ctc;
    }

    public void setCtc(Double ctc) {
        this.ctc = ctc;
    }

    public Double getMonthlyGrossSalary() {
        return monthlyGrossSalary;
    }

    public void setMonthlyGrossSalary(Double monthlyGrossSalary) {
        this.monthlyGrossSalary = monthlyGrossSalary;
    }

    public Integer getPaidLeaveDays() {
        return paidLeaveDays;
    }

    public void setPaidLeaveDays(Integer paidLeaveDays) {
        this.paidLeaveDays = paidLeaveDays;
    }

    public Integer getUnpaidLeaveDays() {
        return unpaidLeaveDays;
    }

    public void setUnpaidLeaveDays(Integer unpaidLeaveDays) {
        this.unpaidLeaveDays = unpaidLeaveDays;
    }

    public Double getLeaveDeduction() {
        return leaveDeduction;
    }

    public void setLeaveDeduction(Double leaveDeduction) {
        this.leaveDeduction = leaveDeduction;
    }

    public Double getVariablePay() {
        return variablePay;
    }

    public void setVariablePay(Double variablePay) {
        this.variablePay = variablePay;
    }

    public Double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(Double netSalary) {
        this.netSalary = netSalary;
    }

    public Boolean getManualPayslip() {
        return manualPayslip;
    }

    public void setManualPayslip(Boolean manualPayslip) {
        this.manualPayslip = manualPayslip;
    }

    public Boolean getHasPayslipFile() {
        return hasPayslipFile;
    }

    public void setHasPayslipFile(Boolean hasPayslipFile) {
        this.hasPayslipFile = hasPayslipFile;
    }
}
