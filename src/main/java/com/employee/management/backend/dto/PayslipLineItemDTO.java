package com.employee.management.backend.dto;

public class PayslipLineItemDTO {
    private String label;
    private String valueType;
    private Double value;

    public PayslipLineItemDTO() {
    }

    public PayslipLineItemDTO(String label, String valueType, Double value) {
        this.label = label;
        this.valueType = valueType;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
