package com.employee.management.backend.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TemplateField {

    // Token name used in bodyContent as {{fieldKey}}.
    @Column(name = "field_key")
    private String fieldKey;

    @Column(name = "label")
    private String label;

    // "TEXT", "DATE" or "CURRENCY" - decides which input type the generator page shows.
    @Column(name = "field_type")
    private String fieldType;

    public TemplateField() {
    }

    public TemplateField(String fieldKey, String label, String fieldType) {
        this.fieldKey = fieldKey;
        this.label = label;
        this.fieldType = fieldType;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }
}
