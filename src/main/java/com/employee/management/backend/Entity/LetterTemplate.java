package com.employee.management.backend.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "letter_templates")
public class LetterTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    // One of OFFER, LETTER_OF_INTENT, EXPERIENCE, RELIEVING - see LetterTemplateService.LETTER_TYPES.
    @Column(name = "letter_type", nullable = false)
    private String letterType;

    @Column(name = "name", nullable = false)
    private String name;

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

    @Column(name = "hr_name")
    private String hrName;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo_data", columnDefinition = "LONGBLOB")
    private byte[] logoData;

    @Column(name = "logo_file_name")
    private String logoFileName;

    @Column(name = "logo_content_type")
    private String logoContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "signature_data", columnDefinition = "LONGBLOB")
    private byte[] signatureData;

    @Column(name = "signature_file_name")
    private String signatureFileName;

    @Column(name = "signature_content_type")
    private String signatureContentType;

    // Paragraphs joined by "\n\n", with {{fieldKey}} tokens for each entry in `fields` below -
    // set only for templates built from an uploaded document (see LetterTemplateService.parseDocument).
    // Null/blank for branding-only templates, which keep using the generator page's own fixed wording.
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "body_content", columnDefinition = "LONGTEXT")
    private String bodyContent;

    @ElementCollection
    @CollectionTable(name = "letter_template_fields", joinColumns = @JoinColumn(name = "letter_template_id"))
    @OrderColumn(name = "field_order")
    private List<TemplateField> fields = new ArrayList<>();

    public LetterTemplate() {
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

    public String getLetterType() {
        return letterType;
    }

    public void setLetterType(String letterType) {
        this.letterType = letterType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getHrName() {
        return hrName;
    }

    public void setHrName(String hrName) {
        this.hrName = hrName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
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

    public byte[] getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(byte[] signatureData) {
        this.signatureData = signatureData;
    }

    public String getSignatureFileName() {
        return signatureFileName;
    }

    public void setSignatureFileName(String signatureFileName) {
        this.signatureFileName = signatureFileName;
    }

    public String getSignatureContentType() {
        return signatureContentType;
    }

    public void setSignatureContentType(String signatureContentType) {
        this.signatureContentType = signatureContentType;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }

    public List<TemplateField> getFields() {
        return fields;
    }

    public void setFields(List<TemplateField> fields) {
        this.fields = fields;
    }
}
