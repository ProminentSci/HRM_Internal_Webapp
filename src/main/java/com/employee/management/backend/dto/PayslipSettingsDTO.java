package com.employee.management.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class PayslipSettingsDTO {
    private String companyName;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String phone;
    private String website;
    private String email;
    private Double basicPercent;
    private Double hraPercent;
    private Double specialAllowancePercent;
    private Double pfPercent;
    private Double professionalTax;
    private Double esiAmount;
    private boolean hasLogo;
    private List<PayslipLineItemDTO> customEarnings = new ArrayList<>();
    private List<PayslipLineItemDTO> customDeductions = new ArrayList<>();

    public PayslipSettingsDTO() {
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

    public boolean isHasLogo() {
        return hasLogo;
    }

    public void setHasLogo(boolean hasLogo) {
        this.hasLogo = hasLogo;
    }

    public List<PayslipLineItemDTO> getCustomEarnings() {
        return customEarnings;
    }

    public void setCustomEarnings(List<PayslipLineItemDTO> customEarnings) {
        this.customEarnings = customEarnings;
    }

    public List<PayslipLineItemDTO> getCustomDeductions() {
        return customDeductions;
    }

    public void setCustomDeductions(List<PayslipLineItemDTO> customDeductions) {
        this.customDeductions = customDeductions;
    }
}
