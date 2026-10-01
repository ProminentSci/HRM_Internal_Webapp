package com.employee.management.backend.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PayslipLineItem {

    @Column(name = "label")
    private String label;

    // "FLAT" (a fixed rupee amount) or "PERCENT" (percent of monthly gross for an earning,
    // percent of earned Basic for a deduction - same basis the built-in fields already use).
    @Column(name = "value_type")
    private String valueType;

    @Column(name = "value")
    private Double value;

    public PayslipLineItem() {
    }

    public PayslipLineItem(String label, String valueType, Double value) {
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
