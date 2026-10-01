package com.employee.management.backend.dto;

public class TemplateFieldDTO {
    private String fieldKey;
    private String label;
    private String fieldType;

    public TemplateFieldDTO() {
    }

    public TemplateFieldDTO(String fieldKey, String label, String fieldType) {
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
