package com.employee.management.backend.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payslip_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"client_id"})
})
public class PayslipSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "address_line3")
    private String addressLine3;

    @Column(name = "phone")
    private String phone;

    @Column(name = "website")
    private String website;

    @Column(name = "email")
    private String email;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo_data", columnDefinition = "LONGBLOB")
    private byte[] logoData;

    @Column(name = "logo_file_name")
    private String logoFileName;

    @Column(name = "logo_content_type")
    private String logoContentType;

    @Column(name = "basic_percent", nullable = false)
    private Double basicPercent = 50.0;

    @Column(name = "hra_percent", nullable = false)
    private Double hraPercent = 20.0;

    @Column(name = "special_allowance_percent", nullable = false)
    private Double specialAllowancePercent = 30.0;

    @Column(name = "pf_percent", nullable = false)
    private Double pfPercent = 12.0;

    @Column(name = "professional_tax", nullable = false)
    private Double professionalTax = 200.0;

    @Column(name = "esi_amount", nullable = false)
    private Double esiAmount = 600.0;

    // Client-defined extra earning/deduction rows, additive on top of the fixed fields above -
    // same shared-per-company model as everything else on this entity, no per-employee overrides.
    @ElementCollection
    @CollectionTable(name = "payslip_custom_earnings", joinColumns = @JoinColumn(name = "payslip_settings_id"))
    @OrderColumn(name = "item_order")
    private List<PayslipLineItem> customEarnings = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "payslip_custom_deductions", joinColumns = @JoinColumn(name = "payslip_settings_id"))
    @OrderColumn(name = "item_order")
    private List<PayslipLineItem> customDeductions = new ArrayList<>();

    public PayslipSettings() {
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressLine3() {
        return addressLine3;
    }

    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public byte[] getLogoData() {
        return logoData;
    }

    public void setLogoData(byte[] logoData) {
        this.logoData = logoData;
    }

    public String getLogoFileName() {
        return logoFileName;
    }

    public void setLogoFileName(String logoFileName) {
        this.logoFileName = logoFileName;
    }

    public String getLogoContentType() {
        return logoContentType;
    }

    public void setLogoContentType(String logoContentType) {
        this.logoContentType = logoContentType;
    }

    public Double getBasicPercent() {
        return basicPercent;
    }

    public void setBasicPercent(Double basicPercent) {
        this.basicPercent = basicPercent;
    }

    public Double getHraPercent() {
        return hraPercent;
    }

    public void setHraPercent(Double hraPercent) {
        this.hraPercent = hraPercent;
    }

    public Double getSpecialAllowancePercent() {
        return specialAllowancePercent;
    }

    public void setSpecialAllowancePercent(Double specialAllowancePercent) {
        this.specialAllowancePercent = specialAllowancePercent;
    }

    public Double getPfPercent() {
        return pfPercent;
    }

    public void setPfPercent(Double pfPercent) {
        this.pfPercent = pfPercent;
    }

    public Double getProfessionalTax() {
        return professionalTax;
    }

    public void setProfessionalTax(Double professionalTax) {
        this.professionalTax = professionalTax;
    }

    public Double getEsiAmount() {
        return esiAmount;
    }

    public void setEsiAmount(Double esiAmount) {
        this.esiAmount = esiAmount;
    }

    public List<PayslipLineItem> getCustomEarnings() {
        return customEarnings;
    }

    public void setCustomEarnings(List<PayslipLineItem> customEarnings) {
        this.customEarnings = customEarnings;
    }

    public List<PayslipLineItem> getCustomDeductions() {
        return customDeductions;
    }

    public void setCustomDeductions(List<PayslipLineItem> customDeductions) {
        this.customDeductions = customDeductions;
    }
}
